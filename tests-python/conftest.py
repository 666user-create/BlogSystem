"""
接口自动化测试公共配置与 fixture（Python + pytest + requests）

运行前置条件：
    1. MySQL 已启动；
    2. 被测应用已启动，默认 http://127.0.0.1:8080
       （启动命令：mvn spring-boot:run -Dspring-boot.run.profiles=dev）
       可通过环境变量覆盖：set BASE_URL=http://127.0.0.1:8080

运行：
    cd tests-python
    pytest                    # 全量执行
    pytest -k login           # 只跑登录相关
    allure serve ../target/allure-results-python    # 查看 Allure 报告
"""

import os
import time

import allure
import pytest
import requests

# 被测服务地址（可用环境变量覆盖，便于指向测试/预发环境）
BASE_URL = os.getenv("BASE_URL", "http://127.0.0.1:8080")

# 测试账号（管理员由应用启动时自动创建）
ADMIN_USER = "admin"
ADMIN_PASSWORD = "admin123"
DEFAULT_PASSWORD = "123456"


# ======================================================================
# 基础工具
# ======================================================================

def unique_name(prefix: str) -> str:
    """生成唯一的用户名（4~20 位，避免用例之间数据冲突）"""
    return f"{prefix}{int(time.time() * 1000) % 10000000}"


def register_user(base_url: str, username: str, password: str = DEFAULT_PASSWORD):
    """注册用户（返回响应对象）"""
    return requests.post(
        f"{base_url}/user/register",
        json={"userName": username, "password": password, "confirmPassword": password},
        timeout=10,
    )


def login(base_url: str, username: str, password: str = DEFAULT_PASSWORD):
    """登录并返回响应对象"""
    return requests.post(
        f"{base_url}/user/login",
        json={"userName": username, "password": password},
        timeout=10,
    )


def auth_headers(token: str) -> dict:
    """构造携带 token 的请求头（后端兼容 user_token / userToken 两个字段名）"""
    return {"user_token": token}


# ======================================================================
# fixture
# ======================================================================

@pytest.fixture(scope="session")
def base_url() -> str:
    """被测服务地址，并在会话开始时做一次健康检查"""
    with allure.step(f"检查被测服务是否可用: {BASE_URL}"):
        try:
            resp = requests.get(f"{BASE_URL}/blog_login.html", timeout=10)
        except requests.exceptions.ConnectionError:
            pytest.exit(
                f"无法连接被测服务 {BASE_URL}，请先启动应用："
                "mvn spring-boot:run -Dspring-boot.run.profiles=dev",
                returncode=1,
            )
        assert resp.status_code == 200, f"服务健康检查失败: HTTP {resp.status_code}"
    return BASE_URL


@pytest.fixture
def new_user(base_url) -> str:
    """注册一个全新的随机用户，返回用户名（密码固定 123456）"""
    username = unique_name("py")
    resp = register_user(base_url, username)
    assert resp.status_code == 200 and resp.json()["code"] == 200, f"测试数据准备失败: {resp.text}"
    return username


@pytest.fixture
def token(base_url, new_user) -> str:
    """新用户登录后的 token"""
    resp = login(base_url, new_user)
    body = resp.json()
    assert body["code"] == 200, f"测试用户登录失败: {resp.text}"
    return body["data"]["token"]


@pytest.fixture
def auth(base_url, new_user, token) -> dict:
    """登录态上下文：{username, token, headers}"""
    return {"username": new_user, "token": token, "headers": auth_headers(token)}


@pytest.fixture
def other_user(base_url) -> dict:
    """
    另一个独立用户（与 auth/new_user 不是同一个账号），用于"非作者越权"用例。
    注意：不能复用 new_user/token，否则"用户 B"实际就是作者本人。
    """
    username = unique_name("other")
    resp = register_user(base_url, username)
    assert resp.status_code == 200 and resp.json()["code"] == 200, f"越权测试账号准备失败: {resp.text}"
    token = login(base_url, username).json()["data"]["token"]
    return {"username": username, "token": token, "headers": auth_headers(token)}


@pytest.fixture
def admin_headers(base_url) -> dict:
    """管理员登录态请求头"""
    resp = login(base_url, ADMIN_USER, ADMIN_PASSWORD)
    body = resp.json()
    assert body["code"] == 200, f"管理员登录失败: {resp.text}"
    return auth_headers(body["data"]["token"])
