"""
鉴权模块接口测试（Python + pytest + requests）

覆盖：JWT 登录拦截器对受保护接口的校验（业务规则 BR-01）
规则：除 /user/login、/user/register 外，/user/**、/blog/** 均需携带有效 token，
     缺失/无效/过期返回 HTTP 401
对应用例文档：docs/03-测试用例设计.md 的 TC-AUTH
"""

import allure
import requests

from conftest import auth_headers


@allure.feature("鉴权")
class TestAuth:
    """JWT 鉴权"""

    @allure.story("鉴权")
    @allure.title("TC-AUTH-01 无 token 访问受保护接口返回 401")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_no_token(self, base_url):
        resp = requests.get(f"{base_url}/blog/getList", timeout=10)
        assert resp.status_code == 401

        resp2 = requests.get(f"{base_url}/user/getUserInfo", params={"userId": 1}, timeout=10)
        assert resp2.status_code == 401

    @allure.story("鉴权")
    @allure.title("TC-AUTH-04 空字符串 token 返回 401")
    def test_empty_token(self, base_url):
        resp = requests.get(f"{base_url}/blog/getList", headers={"user_token": ""}, timeout=10)
        assert resp.status_code == 401

    @allure.story("鉴权")
    @allure.title("TC-AUTH-02 篡改 token 返回 401")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_tampered_token(self, base_url, token):
        tampered = token[:-4] + "xxxx"

        resp = requests.get(f"{base_url}/blog/getList", headers=auth_headers(tampered), timeout=10)
        assert resp.status_code == 401

    @allure.story("鉴权")
    @allure.title("TC-AUTH-03 伪造 token（其他密钥签发）返回 401")
    def test_forged_token(self, base_url):
        # 形如 JWT 但签名无效的 token
        forged = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwibmFtZSI6ImFkbWluIn0.invalid-signature"

        resp = requests.get(f"{base_url}/blog/getList", headers=auth_headers(forged), timeout=10)
        assert resp.status_code == 401

    @allure.story("鉴权")
    @allure.title("TC-AUTH-05 登录/注册接口无需 token 可访问")
    def test_public_endpoints(self, base_url, new_user):
        # 登录接口不带 token 也能正常到达业务层
        resp = requests.post(
            f"{base_url}/user/login",
            json={"userName": new_user, "password": "123456"},
            timeout=10,
        )
        assert resp.status_code == 200
        assert resp.json()["code"] == 200

    @allure.story("鉴权")
    @allure.title("TC-AUTH-06 token 携带正确用户身份（博客归属正确）")
    def test_token_identity(self, base_url, auth):
        resp = requests.post(
            f"{base_url}/blog/add",
            json={"title": "身份校验博客", "content": "内容"},
            headers=auth["headers"],
            timeout=10,
        )
        assert resp.json()["code"] == 200

        list_body = requests.get(f"{base_url}/blog/getList", headers=auth["headers"], timeout=10).json()
        mine = [item for item in list_body["data"] if item["title"] == "身份校验博客"]
        assert len(mine) > 0
        # userId 是 token 中解析出的用户，而不是别人
        assert mine[0]["userId"] is not None
