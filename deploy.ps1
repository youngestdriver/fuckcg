# 快速安装和测试脚本

Write-Host "================================" -ForegroundColor Cyan
Write-Host "  Android 项目部署脚本" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# 检查 ADB 是否可用
try {
    $adbVersion = adb version 2>&1 | Select-String "Android Debug Bridge"
    Write-Host "✅ ADB 已就绪: $adbVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ ADB 未找到,请确保 Android SDK 已安装并配置到 PATH" -ForegroundColor Red
    exit 1
}

# 检查设备连接
Write-Host ""
Write-Host "正在检查设备连接..." -ForegroundColor Yellow
$devices = adb devices | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Host "❌ 未找到连接的设备,请连接 Android 设备或启动模拟器" -ForegroundColor Red
    Write-Host ""
    Write-Host "当前设备列表:" -ForegroundColor Yellow
    adb devices
    exit 1
}
Write-Host "✅ 找到 $($devices.Count) 个设备" -ForegroundColor Green

# 获取设备架构
Write-Host ""
Write-Host "正在检查设备架构..." -ForegroundColor Yellow
$abi = adb shell getprop ro.product.cpu.abi
Write-Host "设备架构: $abi" -ForegroundColor Cyan

if ($abi -notlike "*arm64*") {
    Write-Host "⚠️  警告: 设备架构为 $abi,但项目只包含 arm64-v8a 的 native 库" -ForegroundColor Yellow
    Write-Host "   应用可能无法正常运行!" -ForegroundColor Yellow
    Write-Host ""
    $continue = Read-Host "是否继续安装? (y/n)"
    if ($continue -ne "y") {
        exit 0
    }
}

# 构建项目
Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "开始构建项目..." -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
.\gradlew.bat assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "❌ 构建失败!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✅ 构建成功!" -ForegroundColor Green

# 安装 APK
Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "正在安装 APK..." -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
.\gradlew.bat installDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "❌ 安装失败!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✅ 安装成功!" -ForegroundColor Green

# 启动应用
Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "正在启动应用..." -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
adb shell am start -n com.wzjer.fuckcg/.MainActivity

# 等待一下
Start-Sleep -Seconds 2

# 显示日志
Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "应用日志 (按 Ctrl+C 停止)" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
adb logcat -s ChingoEncrypt:D SignTest:D AndroidRuntime:E

