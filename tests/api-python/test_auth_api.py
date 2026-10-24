"""
鉴权模块接口测试（Python + pytest + requests）

覆盖：JWT 登录拦截器对受保护接口的校验（业务规则 BR-01）
规则：需要 token 的接口 —— /user/**（除 login/register）、/blog 的写操作与管理端；
     缺失/无效/伪造 token 返回 HTTP 401。
     游客可读：/blog/getList、/blog/getBlogDetail（BUG-07 修复后对游客开放）
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
        resp = requests.get(f"{base_url}/blog/adminList", timeout=10)
        assert resp.status_code == 401

        resp2 = requests.get(f"{base_url}/user/getUserInfo", params={"userId": 1}, timeout=10)
        assert resp2.status_code == 401

    @allure.story("鉴权")
    @allure.title("TC-AUTH-04 空字符串 token 返回 401")
    def test_empty_token(self, base_url):
        resp = requests.get(f"{base_url}/blog/adminList", headers={"user_token": ""}, timeout=10)
        assert resp.status_code == 401

    @allure.story("鉴权")
    @allure.title("TC-AUTH-02 篡改 token 返回 401")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_tampered_token(self, base_url, token):
        tampered = token[:-4] + "xxxx"

        resp = requests.get(f"{base_url}/blog/adminList", headers=auth_headers(tampered), timeout=10)
        assert resp.status_code == 401

    @allure.story("鉴权")
    @allure.title("TC-AUTH-03 伪造 token（其他密钥签发）返回 401")
    def test_forged_token(self, base_url):
        # 形如 JWT 但签名无效的 token
        forged = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwibmFtZSI6ImFkbWluIn0.invalid-signature"

        resp = requests.get(f"{base_url}/blog/adminList", headers=auth_headers(forged), timeout=10)
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
        mine = [item for item in list_body["data"]["list"] if item["title"] == "身份校验博客"]
        assert len(mine) > 0
        # userId 是 token 中解析出的用户，而不是别人
        assert mine[0]["userId"] is not None

    @allure.story("鉴权")
    @allure.title("TC-LIST-06 游客（不带 token）可浏览列表与详情，但写操作仍需登录")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_guest_can_browse(self, base_url, auth):
        # 先用已登录用户准备一篇博客
        add = requests.post(
            f"{base_url}/blog/add",
            json={"title": "游客可见博客", "content": "游客也能看到正文"},
            headers=auth["headers"],
            timeout=10,
        )
        assert add.json()["code"] == 200

        with allure.step("游客可读：列表（不带任何 token）"):
            resp = requests.get(f"{base_url}/blog/getList", timeout=10)
            assert resp.status_code == 200
            body = resp.json()
            assert body["code"] == 200
            items = body["data"]["list"]
            assert len(items) > 0
            blog_id = items[0]["id"]

        with allure.step("游客可读：详情（不带任何 token）"):
            detail = requests.get(
                f"{base_url}/blog/getBlogDetail", params={"blogId": blog_id}, timeout=10
            )
            assert detail.status_code == 200
            assert detail.json()["code"] == 200

        with allure.step("但写操作仍被拦截：未登录发表应 401"):
            assert requests.post(
                f"{base_url}/blog/add", json={"title": "x", "content": "y"}, timeout=10
            ).status_code == 401
