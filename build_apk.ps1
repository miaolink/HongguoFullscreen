# 无 Gradle：用 SDK aapt2/d8/apksigner 打 hgfloat.apk
$ErrorActionPreference = "Stop"
$sdk = "D:\allinpol\ADB\android-sdk"
$bt = "$sdk\build-tools\34.0.0"
$proj = $PSScriptRoot
if (-not (Test-Path "$proj\app")) { $proj = "D:\allinpol\ADB\analysis\hongguo\HongguoFullscreen" }
$out = "$proj\manual_build"
$androidJar = "$sdk\platforms\android-34\android.jar"
$src = "$proj\app\src\main\java"
$res = "$proj\app\src\main\res"
$assets = "$proj\app\src\main\assets"
$manifest = "$proj\app\src\main\AndroidManifest.xml"

if (-not (Test-Path "$assets\hg_fs_bundle.js")) {
  $bundleSrc = "D:\allinpol\ADB\analysis\hongguo\tools\frida_bundle\hg_fs_bundle.js"
  if (-not (Test-Path $bundleSrc)) { throw "missing assets/hg_fs_bundle.js (and no $bundleSrc)" }
  New-Item -ItemType Directory -Force -Path $assets | Out-Null
  Copy-Item $bundleSrc "$assets\hg_fs_bundle.js" -Force
}

New-Item -ItemType Directory -Force -Path "$out\res_flat","$out\gen","$out\classes","$out\dex" | Out-Null
& "$bt\aapt2.exe" compile --dir $res -o "$out\res_flat\res.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }
$linkArgs = @(
  "link", "-o", "$out\base.apk", "-I", $androidJar,
  "--manifest", $manifest, "--java", "$out\gen",
  "--auto-add-overlay", "$out\res_flat\res.zip",
  "-A", $assets
)
& "$bt\aapt2.exe" @linkArgs
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

$sources = @(
  Get-ChildItem $src -Recurse -Filter *.java | ForEach-Object FullName
  Get-ChildItem "$out\gen" -Recurse -Filter *.java | ForEach-Object FullName
)
$argfile = "$out\sources.txt"
$sources | Set-Content $argfile -Encoding ascii
Remove-Item "$out\classes\*" -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "$out\classes" | Out-Null
& javac -encoding UTF-8 -g:none --release 8 -classpath $androidJar -d "$out\classes" "@$argfile"
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Push-Location "$out\classes"
& jar cf "$out\classes.jar" .
Pop-Location
Remove-Item "$out\dex\*" -Force -ErrorAction SilentlyContinue
& "$bt\d8.bat" --lib $androidJar --min-api 26 --output "$out\dex" "$out\classes.jar"
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

python -c @"
import zipfile, os
src=r'$out\base.apk'; dst=r'$out\unsigned.apk'; dex=r'$out\dex\classes.dex'; tmp=dst+'.tmp'
with zipfile.ZipFile(src,'r') as zin, zipfile.ZipFile(tmp,'w',zipfile.ZIP_DEFLATED) as zout:
    for i in zin.infolist():
        if i.filename!='classes.dex':
            zout.writestr(i, zin.read(i.filename))
    zout.write(dex,'classes.dex')
os.replace(tmp,dst)
"@

$ks = "$out\debug.keystore"
if (-not (Test-Path $ks)) {
  & keytool -genkeypair -keystore $ks -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
}
& "$bt\zipalign.exe" -f 4 "$out\unsigned.apk" "$out\aligned.apk"
$apkDir = Join-Path $proj "apk"
New-Item -ItemType Directory -Force -Path $apkDir | Out-Null
$finalApk = Join-Path $apkDir "HongguoFullscreen.apk"
& "$bt\apksigner.bat" sign --ks $ks --ks-pass pass:android --key-pass pass:android --out $finalApk "$out\aligned.apk"
Write-Host "OK: $finalApk ($((Get-Item $finalApk).Length) bytes)"
