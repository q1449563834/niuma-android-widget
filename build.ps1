# 一键编译：aapt2 → javac → d8 → 打包 → zipalign → 签名
# 用法：pwsh -File .\build.ps1            只编译
#       pwsh -File .\build.ps1 -Install   编译并安装到已连接的设备/模拟器
param(
    [switch]$Install,
    [string]$Device = ""
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$bt = Join-Path $sdk 'build-tools\36.0.0'
$androidJar = Join-Path $sdk 'platforms\android-37.0\android.jar'
$javaHome = 'C:\Program Files\Android\Android Studio\jbr'
$javac = Join-Path $javaHome 'bin\javac.exe'
$jar = Join-Path $javaHome 'bin\jar.exe'
$keytool = Join-Path $javaHome 'bin\keytool.exe'
$adb = Join-Path $sdk 'platform-tools\adb.exe'

$env:JAVA_HOME = $javaHome
$env:Path = (Join-Path $javaHome 'bin') + ';' + $env:Path

foreach ($p in @($bt, $androidJar, $javac, $jar, $keytool)) {
    if (-not (Test-Path -LiteralPath $p)) { throw "缺少依赖：$p" }
}

$out = Join-Path $root 'build'
$gen = Join-Path $out 'gen'
$classes = Join-Path $out 'classes'
$dex = Join-Path $out 'dex'
$dist = Join-Path $root 'dist'

# 只清理构建自己的中间产物，不整目录删——build\ 里还放着真机调试留下的
# logcat 落盘文件和截图，整删会把调试证据一起清掉。
foreach ($t in @('res.zip', 'base.apk', 'unaligned.apk', 'aligned.apk')) {
    $f = Join-Path $out $t
    if (Test-Path -LiteralPath $f) { Remove-Item -LiteralPath $f -Force }
}
foreach ($d in @($gen, $classes, $dex, $dist)) {
    if (Test-Path -LiteralPath $d) { Remove-Item -LiteralPath $d -Recurse -Force }
}
foreach ($d in @($gen, $classes, $dex, $dist)) { New-Item -ItemType Directory -Force -Path $d | Out-Null }

Write-Host '[1/6] aapt2 compile 资源'
& (Join-Path $bt 'aapt2.exe') compile --dir (Join-Path $root 'src\main\res') -o (Join-Path $out 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile 失败' }

Write-Host '[2/6] aapt2 link 生成 APK 骨架 + R.java'
& (Join-Path $bt 'aapt2.exe') link -o (Join-Path $out 'base.apk') `
    -I $androidJar `
    --manifest (Join-Path $root 'src\main\AndroidManifest.xml') `
    -R (Join-Path $out 'res.zip') `
    --java $gen `
    --min-sdk-version 21 --target-sdk-version 33 `
    --version-code 1 --version-name 1.0 `
    --auto-add-overlay
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link 失败' }

Write-Host '[3/6] javac 编译 Java'
$srcs = @()
$srcs += (Get-ChildItem -LiteralPath (Join-Path $root 'src\main\java') -Recurse -Filter *.java | ForEach-Object { $_.FullName })
$srcs += (Get-ChildItem -LiteralPath $gen -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& $javac -encoding UTF-8 --release 11 -nowarn -classpath $androidJar -d $classes @srcs
if ($LASTEXITCODE -ne 0) { throw 'javac 编译失败' }

Write-Host '[4/6] d8 转 dex'
& $jar cf (Join-Path $out 'classes.jar') -C $classes .
& (Join-Path $bt 'd8.bat') --lib $androidJar --min-api 21 --output $dex (Join-Path $out 'classes.jar')
if ($LASTEXITCODE -ne 0) { throw 'd8 失败' }

Write-Host '[5/6] 打包 dex 进 APK'
$python = Join-Path $root 'tools\pack_apk.py'
$py = (Get-Command python -ErrorAction SilentlyContinue)
if (-not $py) { $py = $env:PYTHON }
& $py $python (Join-Path $out 'base.apk') (Join-Path $dex 'classes.dex') (Join-Path $out 'unaligned.apk')
if ($LASTEXITCODE -ne 0) { throw '打包 dex 失败' }

& (Join-Path $bt 'zipalign.exe') -f 4 (Join-Path $out 'unaligned.apk') (Join-Path $out 'aligned.apk')
if ($LASTEXITCODE -ne 0) { throw 'zipalign 失败' }

Write-Host '[6/6] 签名'
$signDir = Join-Path $root 'signing'
New-Item -ItemType Directory -Force -Path $signDir | Out-Null
$ks = Join-Path $signDir 'niuma.keystore'
if (-not (Test-Path -LiteralPath $ks)) {
    & $keytool -genkeypair -keystore $ks -storepass android -keypass android `
        -alias niuma -keyalg RSA -keysize 2048 -validity 10000 `
        -dname 'CN=NiuMa, OU=Widget, O=feiyuaq, L=Jinan, ST=Shandong, C=CN'
    if ($LASTEXITCODE -ne 0) { throw '生成签名证书失败' }
}
$apkOut = Join-Path $dist 'NiuMaWidget-1.0.apk'
& (Join-Path $bt 'apksigner.bat') sign --ks $ks --ks-pass pass:android --key-pass pass:android `
    --ks-key-alias niuma --out $apkOut (Join-Path $out 'aligned.apk')
if ($LASTEXITCODE -ne 0) { throw '签名失败' }
& (Join-Path $bt 'apksigner.bat') verify $apkOut | Out-Null

$size = [math]::Round((Get-Item -LiteralPath $apkOut).Length / 1KB, 1)
Write-Host "编译完成：$apkOut ($size KB)"

if ($Install) {
    $adbg = @()
    if ($Device -ne '') { $adbg = @('-s', $Device) }
    & $adb @adbg install -r $apkOut
    if ($LASTEXITCODE -ne 0) { throw 'adb install 失败' }
    Write-Host '已安装到设备'
}
