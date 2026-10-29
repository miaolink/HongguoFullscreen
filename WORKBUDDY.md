---
doc_type: agent_install_playbook
project: HongguoFullscreen
project_cn: 红果短剧全屏
repo: https://github.com/miaolink/HongguoFullscreen
release_apk: https://github.com/miaolink/HongguoFullscreen/releases/latest/download/HongguoFullscreen.apk
ldplayer_installer: https://lddl01.ldmnq.com/downloader/ldplayerinst14.exe?n=ldplayer14_ld_112_ld.exe
hongguo_official: https://applink.novelquickapp.com/dppHr
package: com.allinpol.hgfloat
activity: com.allinpol.hgfloat/.MainActivity
target_app: com.phoenix.read
audience: WorkBuddy / AI agent / 自动化助手
goal: 在已连接的 Root 安卓设备或模拟器上，全自动完成下载、安装、权限、依赖配置并启动应用；若无模拟器可先引导安装雷电
idempotent: true
user_action_required: 仅在 Root 弹窗、无障碍确认、安装模拟器/红果需人工确认时，请求用户操作
---

# WorkBuddy / AI 全自动安装配置手册

把下面提示词发给 WorkBuddy（或任意可执行 shell/adb 的 AI）：

```text
请按 https://github.com/miaolink/HongguoFullscreen 里的 WORKBUDDY.md，在我电脑的模拟器上全自动安装配置红果短剧全屏，完成后按检查清单回报。
```

Agent **必须按顺序执行**，每步可重复执行；已成功则跳过。不要让用户手动编译。

---

## 0. 目标与成功标准

完成后设备上应满足：

| # | 检查项 | 判定命令 / 方式 |
|---|--------|-----------------|
| 1 | 已安装本应用 | `adb shell pm path com.allinpol.hgfloat` 有输出 |
| 2 | 已安装红果短剧 | `adb shell pm path com.phoenix.read` 有输出（没有则打开官网下载安装：https://applink.novelquickapp.com/dppHr ） |
| 3 | 悬浮窗已授权 | `adb shell appops get com.allinpol.hgfloat SYSTEM_ALERT_WINDOW` 含 `allow` |
| 4 | frida-inject 可用 | `adb shell su -c 'test -x /data/local/tmp/frida-inject && echo OK'` 输出 `OK` |
| 5 | 应用可启动 | `adb shell am start -n com.allinpol.hgfloat/.MainActivity` 无 Activity 找不到错误 |
| 6 | （可选）通知权限 | Android 13+：`POST_NOTIFICATIONS` 已 grant |

全部通过后，回复用户：「安装配置完成，打开红果播放页后点悬浮球或按 F9 即可全屏。」

---

## 1. 前置探测（失败则停止并说明）

闭环资源（需要时下载，勿把大型安装包塞进本仓库）：

| 资源 | URL |
|------|-----|
| 雷电模拟器 | https://lddl01.ldmnq.com/downloader/ldplayerinst14.exe?n=ldplayer14_ld_112_ld.exe |
| 红果短剧官网 | https://applink.novelquickapp.com/dppHr |
| 本工具 APK | https://github.com/miaolink/HongguoFullscreen/releases/latest/download/HongguoFullscreen.apk |

```bash
adb devices
```

规则：

- 恰好 1 台 `device`：用之。
- 多台：优先 `emulator-*`；否则用用户给出的 `host:5555`；仍不明则询问。
- 无设备：尝试常见模拟器 ADB（雷电/夜神/MuMu/逍遥等），例如：
  - `adb connect 127.0.0.1:5555`
  - `adb connect 127.0.0.1:62001`
  - `adb connect 127.0.0.1:7555`
  - 或用户提供的 IP:端口
- 仍无设备：下载并引导安装雷电模拟器（上表 URL），启动模拟器并开启 Root 后重试连接。
- Root 探测：`adb shell su -c id` 须含 `uid=0`。失败则停止，提示开启模拟器 Root。

下文用 `$ADB` 表示 `adb` 或 `adb -s <serial>`。

---

## 2. 下载 APK（不要编译）

```bash
# 推荐：Release 直链
curl -L -o HongguoFullscreen.apk \
  "https://github.com/miaolink/HongguoFullscreen/releases/latest/download/HongguoFullscreen.apk"
```

备选：

```bash
gh release download -R miaolink/HongguoFullscreen -p "*.apk" -D .
```

校验：文件存在且大小 > 50KB。

---

## 3. 安装 APK

```bash
$ADB install -r HongguoFullscreen.apk
```

若签名冲突：`$ADB uninstall com.allinpol.hgfloat` 后再装。

---

## 4. 授权（尽量静默）

```bash
# 悬浮窗（必须）
$ADB shell appops set com.allinpol.hgfloat SYSTEM_ALERT_WINDOW allow

# 通知（Android 13+，失败可忽略）
$ADB shell pm grant com.allinpol.hgfloat android.permission.POST_NOTIFICATIONS
```

验证：

```bash
$ADB shell appops get com.allinpol.hgfloat SYSTEM_ALERT_WINDOW
```

---

## 5. 部署 frida-inject（必须）

目标路径（固定）：

```text
/data/local/tmp/frida-inject
```

### 5.1 若设备上已有

```bash
$ADB shell su -c 'chmod 755 /data/local/tmp/frida-inject; ls -la /data/local/tmp/frida-inject'
```

已是可执行文件则跳到第 6 步。

### 5.2 若没有则推送

1. 查 ABI：

```bash
$ADB shell getprop ro.product.cpu.abi
```

2. 下载对应官方 `frida-inject`（版本与常用 frida 工具链匹配即可，推荐近期稳定版），解压后得到可执行文件。
3. 推送并授权：

```bash
$ADB push frida-inject /data/local/tmp/frida-inject
$ADB shell su -c 'chmod 755 /data/local/tmp/frida-inject; chown root:root /data/local/tmp/frida-inject'
$ADB shell su -c 'test -x /data/local/tmp/frida-inject && /data/local/tmp/frida-inject --version'
```

模拟器常见 ABI：`x86_64`；真机多为 `arm64-v8a`。架构不对会导致注入失败，必须匹配。

---

## 6. 无障碍 / F9（尽量自动）

开启悬浮球时应用会尝试用 `su` 写入无障碍服务。Agent 可先启动应用并点「开启悬浮球」；或：

```bash
$ADB shell am start -n com.allinpol.hgfloat/.MainActivity
```

若 F9 不可用：引导用户在系统设置 → 无障碍 → 开启「红果全屏」。能自动点击设置页则代为打开：

```bash
$ADB shell am start -a android.settings.ACCESSIBILITY_SETTINGS
```

---

## 7. 启动并完成应用内配置

```bash
$ADB shell am force-stop com.allinpol.hgfloat
$ADB shell am start -n com.allinpol.hgfloat/.MainActivity
```

在应用 UI 中（可用 UI 自动化 / 视觉点击）：

1. 确认勾选「开机自动开启」「自动跳过」（默认一般为开）。
2. 点击「开启悬浮球」。
3. 若弹出 Root 授权：点「允许 / 永久」。
4. 可选：点「关闭授权弹窗」，减少后续 SU 提示。

首次全屏时应用会把脚本预热到 `/data/local/tmp/hg_fs_bundle.js`，无需 Agent 手动拷贝。

---

## 8. 目标应用（官方红果短剧）

```bash
$ADB shell pm path com.phoenix.read
```

未安装时：打开官网下载页，引导用户安装官方红果短剧（不要破解/重打包，不要托管大型 APK）：

```text
https://applink.novelquickapp.com/dppHr
```

可用浏览器打开该链接，或回报用户自行安装后再继续后续步骤。

---

## 9. 一键脚本模板（Windows PowerShell）

在已配置 `adb` 且设备在线时，Agent 可直接跑：

```powershell
$ErrorActionPreference = "Stop"
$serial = (adb devices | Select-String "`tdevice$" | ForEach-Object { ($_ -split "\s+")[0] } | Select-Object -First 1)
if (-not $serial) { throw "no adb device" }
$ADB = { param($a) adb -s $serial @a }

& $ADB @("install","-r","HongguoFullscreen.apk")
& $ADB @("shell","appops","set","com.allinpol.hgfloat","SYSTEM_ALERT_WINDOW","allow")
& $ADB @("shell","pm","grant","com.allinpol.hgfloat","android.permission.POST_NOTIFICATIONS")
& $ADB @("shell","su","-c","chmod 755 /data/local/tmp/frida-inject; ls -la /data/local/tmp/frida-inject")
& $ADB @("shell","am","force-stop","com.allinpol.hgfloat")
& $ADB @("shell","am","start","-n","com.allinpol.hgfloat/.MainActivity")
Write-Host "DONE: open Hongguo play page, tap float ball or press F9"
```

Linux/macOS 用等价 bash 即可。

---

## 10. 故障排查（Agent 自愈）

| 现象 | 处理 |
|------|------|
| `su: not found` / 无 root | 打开模拟器 Root 开关后重试第 1、5 步 |
| 点全屏无反应 | 检查 frida-inject 路径与 ABI；看 `adb logcat -s HgFloat` |
| 悬浮球不出现 | 重做第 4 步悬浮窗授权并重启应用 |
| F9 无效 | 开启无障碍「红果全屏」；仍可用悬浮球 |
| `INSTALL_FAILED_*` | unload 旧包后重装；确认 APK 完整 |

---

## 11. 约束（必须遵守）

- 不要要求用户编译源码；只用 Release APK。
- 不要修改红果短剧安装包。
- 不要关闭设备安全到无法恢复的状态。
- 仅在用户本机已连接、已授权的设备上操作。
- 完成后用第 0 节检查清单逐项回报。

---

## 12. 给人类的一句话用法

安装配置完成后：打开红果短剧播放页 → 点悬浮球或按 **F9** → 进入横屏全屏。
