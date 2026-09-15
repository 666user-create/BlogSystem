"""
极简 HTTP 抓包代理（用于替代 Fiddler 的"明文泄露"自动验证）

原理：
    启动一个本地 HTTP 代理，把经过它的请求（请求行 / 请求头 / 请求体）记录下来，
    再转发给真实服务。测试客户端通过该代理访问接口，
    即可检查请求中是否以明文方式传输了密码等敏感信息。

用法（测试中由 fixture 自动启动，也可手工运行）：
    python proxy_server.py 8888
    # 另一个终端：curl -x http://127.0.0.1:8888 -X POST http://127.0.0.1:8080/user/login -d ...
"""

import http.server
import json
import socketserver
import sys
import threading
import urllib.request


class CaptureProxyHandler(http.server.BaseHTTPRequestHandler):
    """记录请求并转发的代理处理器"""

    # 捕获到的请求记录（进程内共享）
    captured: list = []

    def _handle(self):
        # 1. 读取请求体
        length = int(self.headers.get("Content-Length", 0) or 0)
        body = self.rfile.read(length) if length else b""

        # 2. 记录请求（请求行 / 请求头 / 请求体）
        CaptureProxyHandler.captured.append({
            "method": self.command,
            "url": self.path,
            "headers": {k: v for k, v in self.headers.items()},
            "body": body.decode("utf-8", errors="replace"),
        })

        # 3. 转发到真实服务
        headers = {k: v for k, v in self.headers.items()
                   if k.lower() not in ("proxy-connection", "host", "content-length")}
        req = urllib.request.Request(
            self.path,                       # 代理请求里 path 是完整 URL
            data=body if body else None,
            headers=headers,
            method=self.command,
        )
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                data = resp.read()
                self.send_response(resp.status)
                for k, v in resp.headers.items():
                    if k.lower() not in ("transfer-encoding", "connection", "content-length"):
                        self.send_header(k, v)
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
        except Exception as exc:                                  # noqa: BLE001
            message = f"proxy forward error: {exc}".encode()
            self.send_response(502)
            self.send_header("Content-Length", str(len(message)))
            self.end_headers()
            self.wfile.write(message)

    do_GET = _handle
    do_POST = _handle
    do_PUT = _handle
    do_DELETE = _handle

    def log_message(self, *args):        # 静默，避免污染测试输出
        pass


class CaptureProxy:
    """代理服务器封装：支持 with 语法在测试里启停"""

    def __init__(self, port: int = 8888):
        self.port = port
        self._server = None
        self._thread = None

    def start(self):
        CaptureProxyHandler.captured = []
        self._server = socketserver.ThreadingTCPServer(("127.0.0.1", self.port), CaptureProxyHandler)
        self._server.daemon_threads = True
        self._thread = threading.Thread(target=self._server.serve_forever, daemon=True)
        self._thread.start()
        return self

    def stop(self):
        if self._server:
            self._server.shutdown()
            self._server.server_close()

    @property
    def proxy_url(self) -> str:
        return f"http://127.0.0.1:{self.port}"

    @property
    def captured(self) -> list:
        return CaptureProxyHandler.captured

    def dump(self, path: str):
        """把捕获记录落盘为 JSON（测试证据）"""
        with open(path, "w", encoding="utf-8") as fp:
            json.dump(self.captured, fp, ensure_ascii=False, indent=2)

    def __enter__(self):
        return self.start()

    def __exit__(self, *args):
        self.stop()


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8888
    proxy = CaptureProxy(port).start()
    print(f"抓包代理已启动: {proxy.proxy_url}（Ctrl+C 退出）")
    try:
        while True:
            pass
    except KeyboardInterrupt:
        proxy.stop()
