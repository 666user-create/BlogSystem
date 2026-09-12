"""
用户模块接口测试（Python + pytest + requests）

覆盖：注册 /user/register、登录 /user/login、用户信息 /user/getUserInfo、作者信息 /user/getAuthorInfo
对应用例文档：docs/03-测试用例设计.md 的 TC-REG / TC-LOG / TC-UINF
"""

import allure
import requests

from conftest import DEFAULT_PASSWORD, auth_headers, login, register_user, unique_name


@allure.feature("用户模块")
class TestRegister:
    """注册接口"""

    @allure.story("注册")
    @allure.title("TC-REG-01 合法信息注册成功")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_register_success(self, base_url):
        username = unique_name("reg")
        with allure.step(f"注册用户 {username}"):
            resp = register_user(base_url, username)

        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 200
        assert body["data"] == "注册成功"

    @allure.story("注册")
    @allure.title("TC-REG-07 用户名已存在注册失败")
    def test_register_duplicate_username(self, base_url, new_user):
        with allure.step(f"用已存在的用户名 {new_user} 再次注册"):
            resp = register_user(base_url, new_user)

        body = resp.json()
        assert body["code"] == -1
        assert body["errMsg"] == "用户名已被注册"

    @allure.story("注册")
    @allure.title("TC-REG-03 用户名过短参数校验失败(400)")
    def test_register_username_too_short(self, base_url):
        resp = requests.post(
            f"{base_url}/user/register",
            json={"userName": "abc", "password": DEFAULT_PASSWORD, "confirmPassword": DEFAULT_PASSWORD},
            timeout=10,
        )
        assert resp.status_code == 400
        assert resp.json()["errMsg"] == "参数校验失败"

    @allure.story("注册")
    @allure.title("TC-REG-09 密码为空参数校验失败(400)")
    def test_register_blank_password(self, base_url):
        resp = requests.post(
            f"{base_url}/user/register",
            json={"userName": unique_name("blank")},
            timeout=10,
        )
        assert resp.status_code == 400
        assert resp.json()["errMsg"] == "参数校验失败"


@allure.feature("用户模块")
class TestLogin:
    """登录接口"""

    @allure.story("登录")
    @allure.title("TC-LOG-01 正确账号密码登录成功并返回 token")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_login_success(self, base_url, new_user):
        resp = login(base_url, new_user)
        body = resp.json()

        assert resp.status_code == 200
        assert body["code"] == 200
        assert body["data"]["userName"] == new_user
        assert body["data"]["token"], "登录成功必须返回 token"
        assert body["data"]["userId"] is not None

    @allure.story("登录")
    @allure.title("TC-LOG-02 用户名不存在登录失败")
    def test_login_user_not_exist(self, base_url):
        resp = login(base_url, "no_such_user_99999")
        body = resp.json()

        assert body["code"] == -1
        assert body["errMsg"] == "用户不存在"

    @allure.story("登录")
    @allure.title("TC-LOG-03 密码错误登录失败")
    def test_login_wrong_password(self, base_url, new_user):
        resp = login(base_url, new_user, "wrong-password")
        body = resp.json()

        assert body["code"] == -1
        assert body["errMsg"] == "密码错误"

    @allure.story("登录")
    @allure.title("TC-LOG-10 登录接口无需 token 即可访问")
    def test_login_without_token(self, base_url, new_user):
        resp = login(base_url, new_user)
        assert resp.status_code == 200


@allure.feature("用户模块")
class TestUserInfo:
    """用户信息接口"""

    @allure.story("用户信息")
    @allure.title("TC-UINF-01 查询存在的用户信息（含博客数）")
    def test_get_user_info(self, base_url, auth):
        login_resp = login(base_url, auth["username"])
        user_id = login_resp.json()["data"]["userId"]

        with allure.step(f"查询用户信息 userId={user_id}"):
            resp = requests.get(
                f"{base_url}/user/getUserInfo",
                params={"userId": user_id},
                headers=auth_headers(auth["token"]),
                timeout=10,
            )

        body = resp.json()
        assert body["code"] == 200
        assert body["data"]["userName"] == auth["username"]
        assert "blogCount" in body["data"]

    @allure.story("用户信息")
    @allure.title("TC-UINF-02 查询不存在的用户返回空数据")
    def test_get_user_info_not_exist(self, base_url, auth):
        resp = requests.get(
            f"{base_url}/user/getUserInfo",
            params={"userId": 99999999},
            headers=auth_headers(auth["token"]),
            timeout=10,
        )
        body = resp.json()
        assert body["code"] == 200
        assert body["data"] is None

    @allure.story("作者信息")
    @allure.title("TC-UINF-06 根据不存在的博客查作者返回错误")
    def test_get_author_info_blog_not_exist(self, base_url, auth):
        resp = requests.get(
            f"{base_url}/user/getAuthorInfo",
            params={"blogId": 99999999},
            headers=auth_headers(auth["token"]),
            timeout=10,
        )
        body = resp.json()
        assert body["code"] == -1
        assert body["errMsg"] == "博客不存在"
