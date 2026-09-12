"""
管理端接口测试（Python + pytest + requests）

覆盖：管理员列表 /blog/adminList、切换上下架 /blog/togglePublish
权限规则：管理端接口仅用户名为 admin 的账号可调用（业务规则 BR-05）
对应用例文档：docs/03-测试用例设计.md 的 TC-ADM
"""

import allure
import requests

from test_blog_api import publish_blog


@allure.feature("管理端")
class TestAdminList:
    """管理员列表"""

    @allure.story("管理员列表")
    @allure.title("TC-ADM-01 管理员获取全部博客列表（含下架）")
    def test_admin_list(self, base_url, admin_headers):
        resp = requests.get(f"{base_url}/blog/adminList", headers=admin_headers, timeout=10)
        body = resp.json()

        assert body["code"] == 200
        assert isinstance(body["data"], list)

    @allure.story("管理员列表")
    @allure.title("TC-ADM-02 非管理员访问管理列表被拒绝")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_admin_list_normal_user(self, base_url, auth):
        resp = requests.get(f"{base_url}/blog/adminList", headers=auth["headers"], timeout=10)
        body = resp.json()

        assert body["code"] == -1
        assert body["errMsg"] == "无管理员权限"

    @allure.story("管理员列表")
    @allure.title("TC-ADM-03 未登录访问管理列表返回 401")
    def test_admin_list_without_token(self, base_url):
        resp = requests.get(f"{base_url}/blog/adminList", timeout=10)
        assert resp.status_code == 401


@allure.feature("管理端")
class TestTogglePublish:
    """上下架切换"""

    @allure.story("上下架")
    @allure.title("TC-ADM-04/05 管理员切换博客上下架")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_toggle_publish(self, base_url, auth, admin_headers):
        blog_id = publish_blog(base_url, auth["headers"], "上下架测试博客")

        # 第一次切换：上架 -> 下架
        resp1 = requests.post(
            f"{base_url}/blog/togglePublish",
            params={"blogId": blog_id},
            headers=admin_headers,
            timeout=10,
        )
        assert resp1.json()["data"] is True

        # 下架后普通列表不再展示
        list_body = requests.get(f"{base_url}/blog/getList", headers=auth["headers"], timeout=10).json()
        assert all(item["id"] != blog_id for item in list_body["data"]), "下架博客不应出现在普通列表"

        # 第二次切换：下架 -> 上架（恢复环境）
        resp2 = requests.post(
            f"{base_url}/blog/togglePublish",
            params={"blogId": blog_id},
            headers=admin_headers,
            timeout=10,
        )
        assert resp2.json()["data"] is True

    @allure.story("上下架")
    @allure.title("TC-ADM-07 非管理员切换上下架被拒绝")
    def test_toggle_publish_normal_user(self, base_url, auth):
        blog_id = publish_blog(base_url, auth["headers"], "普通用户尝试切换")

        resp = requests.post(
            f"{base_url}/blog/togglePublish",
            params={"blogId": blog_id},
            headers=auth["headers"],
            timeout=10,
        )
        body = resp.json()
        assert body["code"] == -1
        assert body["errMsg"] == "无管理员权限"

    @allure.story("上下架")
    @allure.title("TC-ADM-06 切换不存在的博客返回错误")
    def test_toggle_publish_not_exist(self, base_url, admin_headers):
        resp = requests.post(
            f"{base_url}/blog/togglePublish",
            params={"blogId": 99999999},
            headers=admin_headers,
            timeout=10,
        )
        body = resp.json()
        assert body["code"] == -1
        assert body["errMsg"] == "博客不存在"
