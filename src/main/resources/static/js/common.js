// 全局为所有 jQuery Ajax 请求添加 token（请求头字段名与后端 Constants.TOKEN 保持一致）
$(document).ajaxSend(function(event, xhr, options) {
    const token = sessionStorage.getItem("user_token");
    if (token) {
        xhr.setRequestHeader("user_token", token);
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

/**
 * 密码加密传输：取服务端 RSA 公钥，用 Web Crypto（RSA-OAEP + SHA-256）加密。
 * 取不到公钥或浏览器不支持时回退明文（后端两者兼容），不影响功能。
 */
function encryptPassword(plain) {
    if (!(window.crypto && crypto.subtle && window.TextEncoder)) {
        return Promise.resolve(plain);
    }
    return fetch("/user/rsaPublicKey").then(function (r) { return r.json(); }).then(function (res) {
        if (!res || res.code !== 200 || !res.data) { return plain; }
        var bin = atob(res.data), der = new Uint8Array(bin.length);
        for (var i = 0; i < bin.length; i++) { der[i] = bin.charCodeAt(i); }
        return crypto.subtle.importKey("spki", der.buffer, {name: "RSA-OAEP", hash: "SHA-256"}, false, ["encrypt"])
            .then(function (key) {
                return crypto.subtle.encrypt({name: "RSA-OAEP"}, key, new TextEncoder().encode(plain));
            })
            .then(function (buf) {
                var bytes = new Uint8Array(buf), s = "";
                for (var j = 0; j < bytes.length; j++) { s += String.fromCharCode(bytes[j]); }
                return btoa(s);
            });
    }).catch(function () { return plain; });
}
