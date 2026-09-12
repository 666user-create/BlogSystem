"""
博客模块接口测试（Python + pytest + requests）

覆盖：列表 /blog/getList、详情 /blog/getBlogDetail、新增 /blog/add、
      更新 /blog/update、删除 /blog/delete
对应用例文档：docs/03-测试用例设计.md 的 TC-LIST / TC-DET / TC-ADD / TC-UPD / TC-DEL
"""

import allure
import requests


def publish_blog(base_url: str, headers: dict, title: str, content: str = "测试内容") -> int:
    """发表一篇博客并返回其 id（新增接口只返回 true，从列表按 id 倒序取第一条）"""
    resp = requests.post(
        f"{base_url}/blog/add",
        json={"title": title, "content": content},
        headers=headers,
        timeout=10,
    )
    assert resp.json()["code"] == 200, f"测试数据准备失败: {resp.text}"

    list_resp = requests.get(f"{base_url}/blog/getList", headers=headers, timeout=10)
    return list_resp.json()["data"][0]["id"]


@allure.feature("博客模块")
class TestBlogList:
    """博客列表"""

    @allure.story("列表")
    @allure.title("TC-LIST-01 列表返回已上架博客数组")
    def test_get_list(self, base_url, auth):
        publish_blog(base_url, auth["headers"], "列表接口测试博客")

        resp = requests.get(f"{base_url}/blog/getList", headers=auth["headers"], timeout=10)
        body = resp.json()

        assert body["code"] == 200
        assert isinstance(body["data"], list)
        assert len(body["data"]) > 0

    @allure.story("列表")
    @allure.title("TC-LIST-07 列表字段完整性")
    def test_get_list_fields(self, base_url, auth):
        publish_blog(base_url, auth["headers"], "字段完整性测试")

        resp = requests.get(f"{base_url}/blog/getList", headers=auth["headers"], timeout=10)
        first = resp.json()["data"][0]

        for field in ("id", "title", "content", "userId", "createTime"):
            assert field in first, f"列表响应应包含字段 {field}"


@allure.feature("博客模块")
class TestBlogDetail:
    """博客详情"""

    @allure.story("详情")
    @allure.title("TC-DET-01 详情正常返回")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_get_detail(self, base_url, auth):
        blog_id = publish_blog(base_url, auth["headers"], "详情接口测试博客", "详情内容")

        resp = requests.get(
            f"{base_url}/blog/getBlogDetail",
            params={"blogId": blog_id},
            headers=auth["headers"],
            timeout=10,
        )
        body = resp.json()

        assert body["code"] == 200
        assert body["data"]["title"] == "详情接口测试博客"
        assert body["data"]["content"] == "详情内容"

    @allure.story("详情")
    @allure.title("TC-DET-02 不存在的博客返回'博客不存在'")
    def test_get_detail_not_exist(self, base_url, auth):
        resp = requests.get(
            f"{base_url}/blog/getBlogDetail",
            params={"blogId": 99999999},
            headers=auth["headers"],
            timeout=10,
        )
        body = resp.json()
        assert body["code"] == -1
        assert body["errMsg"] == "博客不存在"

    @allure.story("详情")
    @allure.title("TC-DET-04 blogId=0 参数校验失败(400) —— BUG-12 回归用例")
    def test_get_detail_zero_id_bad_request(self, base_url, auth):
        resp = requests.get(
            f"{base_url}/blog/getBlogDetail",
            params={"blogId": 0},
            headers=auth["headers"],
            timeout=10,
        )
        assert resp.status_code == 400, "非法参数应返回 HTTP 400"
        assert resp.json()["errMsg"] == "参数校验失败"


@allure.feature("博客模块")
class TestBlogAdd:
    """新增博客"""

    @allure.story("新增")
    @allure.title("TC-ADD-01 登录用户正常发表博客")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_add_blog(self, base_url, auth):
        title = "新增接口测试博客"
        resp = requests.post(
            f"{base_url}/blog/add",
            json={"title": title, "content": "新增内容"},
            headers=auth["headers"],
            timeout=10,
        )
        body = resp.json()
        assert body["code"] == 200
        assert body["data"] is True

        # 断言：新博客出现在列表中
        list_body = requests.get(f"{base_url}/blog/getList", headers=auth["headers"], timeout=10).json()
        assert any(item["title"] == title for item in list_body["data"])

    @allure.story("新增")
    @allure.title("TC-ADD-02 标题为空参数校验失败(400)")
    def test_add_blog_blank_title(self, base_url, auth):
        resp = requests.post(
            f"{base_url}/blog/add",
            json={"title": "", "content": "内容"},
            headers=auth["headers"],
            timeout=10,
        )
        assert resp.status_code == 400
        assert resp.json()["errMsg"] == "参数校验失败"

    @allure.story("新增")
    @allure.title("TC-ADD-05 标题超过200字符参数校验失败(400)")
    def test_add_blog_title_too_long(self, base_url, auth):
        resp = requests.post(
            f"{base_url}/blog/add",
            json={"title": "长" * 201, "content": "内容"},
            headers=auth["headers"],
            timeout=10,
        )
        assert resp.status_code == 400

    @allure.story("新增")
    @allure.title("TC-ADD-07 未登录发表博客被拦截(401)")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_add_blog_without_token(self, base_url):
        resp = requests.post(
            f"{base_url}/blog/add",
            json={"title": "未登录发表", "content": "内容"},
            timeout=10,
        )
        assert resp.status_code == 401


@allure.feature("博客模块")
class TestBlogUpdate:
    """更新博客"""

    @allure.story("更新")
    @allure.title("TC-UPD-01 作者本人更新博客成功")
    def test_update_blog(self, base_url, auth):
        blog_id = publish_blog(base_url, auth["headers"], "更新前标题", "更新前内容")

        resp = requests.post(
            f"{base_url}/blog/update",
            json={"id": blog_id, "title": "更新后标题", "content": "更新后内容"},
            headers=auth["headers"],
            timeout=10,
        )
        assert resp.json()["code"] == 200

        detail = requests.get(
            f"{base_url}/blog/getBlogDetail",
            params={"blogId": blog_id},
            headers=auth["headers"],
            timeout=10,
        ).json()
        assert detail["data"]["title"] == "更新后标题"
        assert detail["data"]["content"] == "更新后内容"

    @allure.story("更新")
    @allure.title("TC-UPD-02 非作者更新他人博客被拒绝")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_blog_not_owner(self, base_url, auth, other_user):
        """auth 是作者 A；other_user 是另一个独立用户 B"""
        blog_id = publish_blog(base_url, auth["headers"], "A的博客", "A的内容")

        resp = requests.post(
            f"{base_url}/blog/update",
            json={"id": blog_id, "title": "B想篡改", "content": "B的内容"},
            headers=other_user["headers"],
            timeout=10,
        )
        body = resp.json()
        assert body["code"] == -1
        assert body["errMsg"] == "无权编辑该博客"


@allure.feature("博客模块")
class TestBlogDelete:
    """删除博客"""

    @allure.story("删除")
    @allure.title("TC-DEL-01 作者删除博客后详情查不到（逻辑删除）")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_delete_blog(self, base_url, auth):
        blog_id = publish_blog(base_url, auth["headers"], "待删除博客", "待删除内容")

        resp = requests.post(
            f"{base_url}/blog/delete",
            params={"blogId": blog_id},
            headers=auth["headers"],
            timeout=10,
        )
        assert resp.json()["data"] is True

        # 逻辑删除后详情不可见
        detail = requests.get(
            f"{base_url}/blog/getBlogDetail",
            params={"blogId": blog_id},
            headers=auth["headers"],
            timeout=10,
        ).json()
        assert detail["code"] == -1
        assert detail["errMsg"] == "博客不存在"

    @allure.story("删除")
    @allure.title("TC-DEL-02 非作者删除他人博客被拒绝")
    def test_delete_blog_not_owner(self, base_url, auth, other_user):
        blog_id = publish_blog(base_url, auth["headers"], "A的博客2", "A的内容2")

        resp = requests.post(
            f"{base_url}/blog/delete",
            params={"blogId": blog_id},
            headers=other_user["headers"],
            timeout=10,
        )
        body = resp.json()
        assert body["code"] == -1
        assert body["errMsg"] == "无权删除该博客"

    @allure.story("删除")
    @allure.title("TC-DEL-03 删除不存在的博客返回错误")
    def test_delete_blog_not_exist(self, base_url, auth):
        resp = requests.post(
            f"{base_url}/blog/delete",
            params={"blogId": 99999999},
            headers=auth["headers"],
            timeout=10,
        )
        body = resp.json()
        assert body["code"] == -1
        assert body["errMsg"] == "博客不存在"
