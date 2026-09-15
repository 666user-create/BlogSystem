# ============================================================
#  blog-cloud 一键启动脚本
#  配套 start-all.bat 双击使用
#
#  做的事:
#    1) 检查三个 jar 是否存在(顺带提醒源码是否比 jar 新)
#    2) 检查 Nacos 是否在运行,没运行就启动它并等它就绪
#    3) 依次启动 用户服务 -> 博客服务 -> 网关(各自一个窗口)
#    4) 等端口就绪,打印状态表
#    5) 全部就绪后自动打开浏览器
# ============================================================

$Root     = $PSScriptRoot
$NacosBin = "D:\nacos-server-2.4.3\nacos\bin"
$WebUrl   = "http://localhost:8080/blog_list.html"
$NacosUrl = "http://localhost:8848/nacos"

# ---------------- 工具函数 ----------------

# 探测本机端口是否已被监听(比 netstat 更快更可靠)
function Test-Port([int]$Port) {
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $client.Connect("127.0.0.1", $Port)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

# 等待端口就绪,超时返回 $false
function Wait-Port([int]$Port, [int]$TimeoutSec) {
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        if (Test-Port $Port) { return $true }
        Start-Sleep -Milliseconds 800
    }
    return $false
}

# 找一个能跑 Spring Boot 3 的 JDK(17+),优先用实测跑通过的 JDK 21
function Find-Java {
    $candidates = @(
        "C:\Users\27469\.jdks\ms-21.0.11\bin\java.exe",
        "D:\java\bin\java.exe"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }
    $cmd = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

# ---------------- 开场 ----------------
Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "            blog-cloud 一键启动" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# ---------------- 1) 检查 jar ----------------
$services = @(
    [pscustomobject]@{ Name = "用户服务"; Tag = "user-service";  Port = 8081; Jar = (Join-Path $Root "blog-user-service\target\blog-user-service-0.0.1-SNAPSHOT.jar") },
    [pscustomobject]@{ Name = "博客服务"; Tag = "blog-service";  Port = 8082; Jar = (Join-Path $Root "blog-blog-service\target\blog-blog-service-0.0.1-SNAPSHOT.jar") },
    [pscustomobject]@{ Name = "网关";     Tag = "gateway";       Port = 8080; Jar = (Join-Path $Root "blog-gateway\target\blog-gateway-0.0.1-SNAPSHOT.jar") }
)

$missing = @($services | Where-Object { -not (Test-Path $_.Jar) })
if ($missing.Count -gt 0) {
    Write-Host "[X] 找不到下面这些 jar, 请先构建项目:" -ForegroundColor Red
    foreach ($m in $missing) { Write-Host ("      " + $m.Jar) -ForegroundColor Red }
    Write-Host ""
    Write-Host "    构建命令(在本目录执行):" -ForegroundColor Yellow
    Write-Host "      mvn -o -DskipTests clean package" -ForegroundColor Yellow
    Write-Host "    或在 IDEA 里打开 blog-cloud 后重新构建" -ForegroundColor Yellow
    Write-Host ""
    return
}

# 源码比 jar 新 -> 提醒(不阻断)
try {
    $newestSrc = Get-ChildItem -Path (Join-Path $Root "*\src") -Recurse -Filter "*.java" -ErrorAction SilentlyContinue |
                 Sort-Object LastWriteTime -Descending | Select-Object -First 1
    $oldestJar = $services | ForEach-Object { Get-Item $_.Jar } | Sort-Object LastWriteTime | Select-Object -First 1
    if ($newestSrc -and $oldestJar -and ($newestSrc.LastWriteTime -gt $oldestJar.LastWriteTime)) {
        Write-Host "[!] 注意: 检测到源码比 jar 新, 现在启动的是旧代码" -ForegroundColor Yellow
        Write-Host "    想跑最新代码, 先执行: mvn -o -DskipTests clean package" -ForegroundColor Yellow
        Write-Host ""
    }
} catch { }

# ---------------- 2) 启动 Nacos ----------------
if (Test-Port 8848) {
    Write-Host "[OK] Nacos 已在运行 (端口 8848)" -ForegroundColor Green
} else {
    $startupCmd = Join-Path $NacosBin "startup.cmd"
    if (-not (Test-Path $startupCmd)) {
        Write-Host "[X] 找不到 Nacos 启动脚本: $startupCmd" -ForegroundColor Red
        Write-Host "    如果 Nacos 装在别的目录, 请修改本脚本开头的 `$NacosBin" -ForegroundColor Yellow
        Write-Host ""
        return
    }
    Write-Host "[..] Nacos 未运行, 正在启动 (standalone 模式) ..." -ForegroundColor Yellow
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c", "`"$startupCmd`" -m standalone" -WorkingDirectory $NacosBin -WindowStyle Minimized | Out-Null
    if (Wait-Port -Port 8848 -TimeoutSec 90) {
        Start-Sleep -Seconds 3   # 端口通了再稍等, 让它把服务注册接口准备好
        Write-Host "[OK] Nacos 启动成功 -> $NacosUrl" -ForegroundColor Green
    } else {
        Write-Host "[X] Nacos 启动超时(90 秒), 请看 Nacos 窗口或日志确认原因" -ForegroundColor Red
        Write-Host "    日志: $NacosBin\..\logs\start.out" -ForegroundColor Yellow
        Write-Host ""
        return
    }
}
Write-Host ""

# ---------------- 3) 启动三个服务 ----------------
$java = Find-Java
if (-not $java) {
    Write-Host "[X] 找不到可用的 java.exe, 请安装 JDK 17 或以上" -ForegroundColor Red
    return
}
Write-Host "[i] 使用 JDK: $java" -ForegroundColor Gray
Write-Host ""

# 先起两个业务服务(让它们注册到 Nacos), 最后起网关
$ordered = @($services | Where-Object { $_.Port -ne 8080 }) + @($services | Where-Object { $_.Port -eq 8080 })

foreach ($s in $ordered) {
    if (Test-Port $s.Port) {
        Write-Host ("[OK] {0} 已在运行 (端口 {1}), 跳过" -f $s.Name, $s.Port) -ForegroundColor Green
        continue
    }
    Write-Host ("[..] 正在启动 {0} (端口 {1}) ..." -f $s.Name, $s.Port) -ForegroundColor Yellow
    # 窗口标题带 blog-cloud-端口, 方便辨认和关闭
    $title = "blog-cloud-$($s.Port) $($s.Tag)"
    $inner = "title $title && `"$java`" -jar `"$($s.Jar)`""
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k", $inner -WorkingDirectory $Root -WindowStyle Normal | Out-Null

    if (Wait-Port -Port $s.Port -TimeoutSec 75) {
        Write-Host ("[OK] {0} 启动成功" -f $s.Name) -ForegroundColor Green
        if ($s.Port -ne 8080) { Start-Sleep -Seconds 3 }   # 给业务服务一点时间注册到 Nacos
    } else {
        Write-Host ("[X] {0} 启动超时, 请看标题为 '{1}' 的窗口里的报错" -f $s.Name, $title) -ForegroundColor Red
    }
}
Write-Host ""

# ---------------- 4) 状态汇总 ----------------
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "                      启动结果" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$checks = @(
    [pscustomobject]@{ Name = "Nacos    "; Port = 8848 },
    [pscustomobject]@{ Name = "用户服务 "; Port = 8081 },
    [pscustomobject]@{ Name = "博客服务 "; Port = 8082 },
    [pscustomobject]@{ Name = "网关     "; Port = 8080 }
)
$allOk = $true
foreach ($c in $checks) {
    $ok = Test-Port $c.Port
    if (-not $ok) { $allOk = $false }
    $state = if ($ok) { "运行中 OK" } else { "未运行 XX" }
    $color = if ($ok) { "Green" } else { "Red" }
    Write-Host ("   {0}  端口 {1}   {2}" -f $c.Name, $c.Port, $state) -ForegroundColor $color
}
Write-Host ""

if ($allOk) {
    Write-Host "   浏览器访问: $WebUrl" -ForegroundColor Green
    Write-Host "   管理员账号: admin / admin123" -ForegroundColor Green
    Write-Host ""
    Write-Host "   正在打开浏览器 ..." -ForegroundColor Gray
    Start-Process $WebUrl | Out-Null
} else {
    Write-Host "   有服务没起来: 看对应黑窗口(标题以 blog-cloud- 开头)里的红字报错" -ForegroundColor Yellow
    Write-Host "   常见原因: 端口被旧进程占用 / MySQL 没启动 / 数据库没建(见文档第 1 步)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "   服务注册查询: $NacosUrl   (nacos / nacos)" -ForegroundColor Gray
Write-Host "   停止全部:     双击 stop-all.bat" -ForegroundColor Gray
Write-Host ""
