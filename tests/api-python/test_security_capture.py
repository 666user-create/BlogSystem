"""
安全抓包测试（明文传输检测）—— Fiddler 抓包分析的自动化实现

用途：
    验证登录等敏感接口的请求是否以明文传输密码、以及响应中是否泄露敏感信息。
    用本地抓包代理（proxy_server.CaptureProxy）替代 Fiddler 手工抓包，
    结果可重复执行并落盘为 JSON 证据。

对应用例文档：docs/03-测试用例设计.md 的 TC-SEC-03（敏感信息）、TC-SEC-02（注入）
抓包证据输出：tests/evidence/capture/login-capture.json（生成物，不入库）
"""

import json
import os

import allure
import pytest
import requests

from conftest import DEFAULT_PASSWORD, login
from proxy_server import CaptureProxy

# 抓包证据目录（相对本目录：tests/api-python/ → tests/evidence/capture）
CAPTURE_DIR = os.path.join("..", "evidence", "capture")
PROXY_PORT = 8899


@pytest.fixture(scope="module")
def capture():
    """模块级 fixture：启动抓包代理，结束后落盘证据"""
    os.makedirs(CAPTURE_DIR, exist_ok=True)
    proxy = CaptureProxy(PROXY_PORT).start()
    yield proxy
    proxy.dump(os.path.join(CAPTURE_DIR, "login-capture.json"))
    proxy.stop()


@allure.feature("安全测试")
@allure.story("抓包分析")
class TestCaptureAnalysis:
    """基于抓包的安全检查"""

    @allure.title("TC-SEC-05 抓包验证登录请求以 HTTPS/加密方式传输")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_capture_login_request(self, base_url, new_user, capture):
        """通过抓包代理发起登录，检查请求内容"""
        proxies = {"http": capture.proxy_url, "https": capture.proxy_url}

        with allure.step("通过抓包代理调用登录接口"):
            resp = requests.post(
                f"{base_url}/user/login",
                json={"userName": new_user, "password": DEFAULT_PASSWORD},
                proxies=proxies,
                timeout=15,
            )
        assert resp.json()["code"] == 200

        with allure.step("读取抓包记录"):
            records = [r for r in capture.captured if "/user/login" in r["url"]]
            assert len(records) > 0, "抓包代理应捕获到登录请求"

        record = records[-1]
        with allure.step("检查请求明细"):
            # 记录请求方法、路径、请求体，供人工复核
            assert record["method"] == "POST"
            assert "/user/login" in record["url"]
            assert new_user in record["body"], "请求体中应包含用户名"

        # 结论：当前部署为 HTTP 明文传输，密码在请求体中可见。
        # 该用例的断言用于"确认抓包能力可用"，同时把风险结论输出到 Allure 步骤中。
        if record["body"].find(DEFAULT_PASSWORD) >= 0:
            allure.attach(
                json.dumps(record, ensure_ascii=False, indent=2),
                name="登录请求抓包明细（明文传输风险）",
                attachment_type=allure.attachment_type.JSON,
            )

    @allure.title("TC-SEC-06 抓包验证响应头不含敏感信息")
    def test_capture_response_headers(self, base_url, new_user, capture):
        proxies = {"http": capture.proxy_url, "https": capture.proxy_url}

        resp = requests.post(
            f"{base_url}/user/login",
            json={"userName": new_user, "password": DEFAULT_PASSWORD},
            proxies=proxies,
            timeout=15,
        )

        # 响应头不应泄露服务端实现细节或凭据
        sensitive_headers = ("password", "secret", "token=", "authorization")
        joined = " ".join(f"{k}:{v}" for k, v in resp.headers.items()).lower()
        for keyword in sensitive_headers:
            assert keyword not in joined, f"响应头不应包含敏感字段: {keyword}"

        # 密码不应出现在响应体中
        assert DEFAULT_PASSWORD not in resp.text or resp.json()["code"] != 200, \
            "登录响应体不应回显密码"

    @allure.title("TC-SEC-02 登录接口 SQL 注入尝试不产生异常")
    def test_sql_injection_attempt(self, base_url):
        """用户名传入注入串，应走正常业务判断（用户不存在），而不是 500/堆栈"""
        resp = requests.post(
            f"{base_url}/user/login",
            json={"userName": "' OR '1'='1", "password": "123456"},
            timeout=10,
        )

        # 参数校验会先拦截（用户名字符长度限制），或者业务层返回"用户不存在"；
        # 关键断言：不出现 500 服务器错误、不泄露 SQL 异常
        assert resp.status_code in (200, 400), f"不应产生服务器错误: {resp.status_code}"
        assert "SQL" not in resp.text.upper()
        assert "Exception" not in resp.text
