"""
安全测试：密码加密传输、响应敏感信息、SQL 注入

对应设计用例：TC-SEC-02（注入）、TC-SEC-03（日志/响应不泄露敏感信息）、TC-SEC-05（密码加密传输）。
前端登录/注册页会用服务端 RSA 公钥加密密码后再提交，抓包只能看到密文；
接口层同时兼容明文（脚本/测试直接调用），因此这里验证"加密链路可用"与"响应不泄露"。
"""

import base64

import allure
import requests
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding

from conftest import DEFAULT_PASSWORD


def rsa_encrypt(public_key_b64: str, plain: str) -> str:
    """用服务端公钥（X.509/Base64）按 RSA-OAEP-SHA256 加密，返回 Base64 密文"""
    public_key = serialization.load_der_public_key(base64.b64decode(public_key_b64))
    cipher = public_key.encrypt(
        plain.encode(),
        padding.OAEP(mgf=padding.MGF1(algorithm=hashes.SHA256()),
                     algorithm=hashes.SHA256(), label=None),
    )
    return base64.b64encode(cipher).decode()


@allure.feature("安全测试")
@allure.story("传输与响应安全")
class TestSecurity:
    """安全基线检查"""

    @allure.title("TC-SEC-05 登录密码加密传输（RSA-OAEP）")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_encrypted_password_login(self, base_url):
        with allure.step("获取服务端公钥"):
            resp = requests.get(f"{base_url}/user/rsaPublicKey", timeout=10)
            body = resp.json()
            assert body["code"] == 200
            public_key = body["data"]
            assert public_key and len(public_key) > 100, "应返回非空 RSA 公钥"

        with allure.step("用公钥加密密码后登录"):
            encrypted = rsa_encrypt(public_key, "admin123")
            assert encrypted != "admin123", "密码应为密文"
            login = requests.post(
                f"{base_url}/user/login",
                json={"userName": "admin", "encryptedPassword": encrypted},
                timeout=15,
            )
            result = login.json()
            assert result["code"] == 200, result
            assert result["data"]["token"], "登录成功应返回 token"

        with allure.step("请求体里只有密文，没有明文密码"):
            assert "admin123" not in login.request.body.decode()

    @allure.title("TC-SEC-03 登录响应不含密码等敏感信息")
    def test_login_response_no_secret(self, base_url, new_user):
        resp = requests.post(
            f"{base_url}/user/login",
            json={"userName": new_user, "password": DEFAULT_PASSWORD},
            timeout=15,
        )

        joined = " ".join(f"{k}:{v}" for k, v in resp.headers.items()).lower()
        for keyword in ("password", "secret", "token=", "authorization"):
            assert keyword not in joined, f"响应头不应包含敏感字段: {keyword}"

        assert DEFAULT_PASSWORD not in resp.text, "登录响应体不应回显密码"

    @allure.title("TC-SEC-02 登录接口 SQL 注入尝试不产生异常")
    def test_sql_injection_attempt(self, base_url):
        resp = requests.post(
            f"{base_url}/user/login",
            json={"userName": "' OR '1'='1", "password": "123456"},
            timeout=10,
        )

        assert resp.status_code in (200, 400), f"不应产生服务器错误: {resp.status_code}"
        assert "SQL" not in resp.text.upper()
        assert "Exception" not in resp.text
