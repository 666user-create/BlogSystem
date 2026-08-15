// 全局为所有 jQuery Ajax 请求添加 token
$(document).ajaxSend(function(event, xhr, options) {
    const token = sessionStorage.getItem("user_token") || sessionStorage.getItem("userToken");
    if (token) {
        xhr.setRequestHeader("user_token", token);
        xhr.setRequestHeader("userToken", token);
    }
});

// 全局拦截错误，统一处理未登录 / 无权限 / 服务器异常等
$(document).ajaxError(function(event, xhr, options, error) {
    if (xhr.status === 401) {
        sessionStorage.clear();
        alert("登录已失效，请重新登录");
        window.location.href = "blog_login.html";
    } else if (xhr.status === 403) {
        alert("无权限执行该操作");
    } else if (xhr.status >= 500) {
        alert("服务器开小差了，请稍后重试");
    }
});

// 注销函数，导航栏里的 "注销" 按钮会调用
function logout() {
    sessionStorage.clear();
    window.location.href = "blog_login.html";
}

// 管理员才显示"管理"入口
$(function () {
    if (sessionStorage.getItem("userName") === "admin") {
        $(".nav-admin").show();
    }
});
