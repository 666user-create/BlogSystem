# ============================================================
#  blog-cloud 一键停止脚本
#  配套 stop-all.bat 双击使用
#
#  用法:
#    stop-all.bat        只停三个 Java 服务, Nacos 继续保留(下次启动更快)
#    stop-all.bat all    连 Nacos 一起停掉(全部清理)
# ============================================================

param([string]$Mode = "")

# ---------------- 工具函数 ----------------

# 查出正在监听某个端口的进程号
function Get-PortPid([int]$Port) {
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) { return $conn.OwningProcess }
    # 兜底: 用 netstat 解析
    $line = netstat -ano | Select-String -Pattern (":$Port\s+.*LISTENING") | Select-Object -First 1
    if ($line) {
        $parts = ($line.Line -split "\s+") | Where-Object { $_ -ne "" }
        return [int]$parts[-1]
    }
    return $null
}

# 停止某个端口上的进程, 并顺手关掉它的黑窗口
function Stop-ByPort([int]$Port, [string]$Name) {
    $procId = Get-PortPid $Port
    if (-not $procId) {
        Write-Host ("[--] {0} 本来就没有在运行 (端口 {1})" -f $Name, $Port) -ForegroundColor Gray
        return
    }
    Write-Host ("[..] 停止 {0} (端口 {1}, PID {2}) ..." -f $Name, $Port, $procId) -ForegroundColor Yellow
    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 800
    if (Get-PortPid $Port) {
        Write-Host ("[X]  {0} 停止失败, 请手动关闭它的窗口" -f $Name) -ForegroundColor Red
    } else {
        Write-Host ("[OK] {0} 已停止" -f $Name) -ForegroundColor Green
    }
}

# ---------------- 开场 ----------------
Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "            blog-cloud 一键停止" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# 先停网关(入口), 再停业务服务
Stop-ByPort -Port 8080 -Name "网关     "
Stop-ByPort -Port 8082 -Name "博客服务 "
Stop-ByPort -Port 8081 -Name "用户服务 "

Write-Host ""

# ---------------- Nacos ----------------
if ($Mode -eq "all") {
    Stop-ByPort -Port 8848 -Name "Nacos    "
} else {
    if (Get-PortPid 8848) {
        Write-Host "[i] Nacos 仍在运行(保留着, 下次启动更快)" -ForegroundColor Gray
        Write-Host "    想连 Nacos 一起关掉: 双击 stop-all.bat all" -ForegroundColor Gray
    } else {
        Write-Host "[i] Nacos 当前未运行" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "如果屏幕上还有标题以 blog-cloud- 开头的黑窗口, 直接点右上角关闭即可。" -ForegroundColor Gray
Write-Host ""
