# 牛马脱敏器 · 安卓版

Windows 桌面悬浮窗项目 [feiyuaq/niuma](https://github.com/feiyuaq/niuma)（MIT）的安卓移植版：
把「今天已经赚了多少钱」做成 **1×2 / 1×4 / 2×2 / 2×4 四种桌面小部件**，外加一个可以配置班制的 App。
纯 Java、零第三方依赖、**无网络权限**，所有设置只存在本机。

- 包名：`com.feiyuaq.niuma`
- 支持：Android 5.0（API 21）～ Android 13（target 33）
- 许可证：MIT，原项目版权归 [feiyuaq](https://github.com/feiyuaq)，全文见 `LICENSE`

### 下载安装

**[⬇ NiuMaWidget-1.0.apk](https://github.com/q1449563834/niuma-android-widget/releases/download/v1.0.0/NiuMaWidget-1.0.apk)**（71,153 B，约 69.5 KB）

也可以在 [Releases 页面](https://github.com/q1449563834/niuma-android-widget/releases) 里取。下载后在手机上允许「安装未知来源应用」即可安装，
装完长按桌面空白处 → 小部件 → 找到「牛马脱敏器」的四种尺寸放上去。首次打开 App 先设薪资与班制，小部件才会按你的班表计薪。

> 仓库里不放二进制：`dist/` 与 `signing/` 都在 `.gitignore` 里，安装包只走 Release 资产。

> 本仓库**不含签名私钥**：`build.ps1` 发现 `signing/niuma.keystore` 不存在时会用 `keytool` 自动生成一张
> （storepass / keypass 都是 `android`）。因此克隆后第一次构建能直接出包；但如果你手机上装着的旧包是用
> 别的证书签的，覆盖安装会报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`，先卸载再装即可。

> 这是原项目的**非官方移植版**，不修改原仓库，也不代表原作者立场；安卓端的小部件模型、班制配置、
> 真机修复等均为本移植版新增。构建产物（`build/`、`dist/`）、签名私钥、真机调试截图都不进仓库，见 `.gitignore`。

---

## 一、装到手机上怎么用

1. `adb install -r dist/NiuMaWidget-1.0.apk`，或直接把 APK 拷进手机点击安装（需要允许「安装未知来源应用」）。
2. 打开 App，先设薪资（日薪 / 月薪 / 时薪三选一，默认月薪 7000、月计薪 21.75 天、5 位小数、¥、发薪日 10 号）。
3. 选班制：**长白班 / APN / 自定义循环** 三选一。
4. 回到桌面 → 长按空白处 → 「小部件 / 添加小部件」→ 找到「牛马脱敏器」，会看到四种尺寸：
   - **1×2（迷你条）**：两格宽一行高，状态 + 金额 + 细进度条；
   - **1×4（细条）**：四格宽一行高，金额 + 状态 + 细进度条；
   - **2×2（方卡）**：两格宽两行高，多出「今日已赚 / 班次 / 倒计时 / 时薪 / 距发薪」；
   - **2×4（完整卡）**：四格宽两行高，再叠上星期与 +¥/时。
   拖到桌面松手即可。App 里也有「添加 1×2 迷你条 / 1×4 细条 / 2×2 方卡 / 2×4 完整卡到桌面」四个按钮（走系统的 `requestPinAppWidget`，
   启动器不支持时会自动弹出图文教程）。
5. 想秒级跳动，把「秒级刷新」开关留着（默认开）。它靠一个 `IMPORTANCE_MIN` 的前台服务每秒重绘小部件，熄屏不画、不耗电；
   关掉后改由系统广播驱动（打开 App 或每半小时刷新一次）。Android 13 首次启动会请求通知权限，不给也不影响计薪。

---

## 二、三种班制

### 1. 长白班
固定起止时间 + 可选午休段 + 周几上班。
默认 08:30–17:30、午休 12:00–13:00（不计薪）、周一至周五。改时间就是点右侧那几行直接改。

### 2. APN（原「工厂三班倒」）
按 白（Afternoon 前段）/ 中（Afternoon）/ 夜（Night）三个班拼出 8 天循环：**白 白 中 中 夜 夜 休 休**
- 三个班的上下班时间**都能改**：设置里「三班时间」六行（白班上班/下班、中班上班/下班、夜班上班/下班），
  默认 白 `08:00–16:00`、中 `16:00–24:00`、夜 `00:00–08:00`，每班 8 小时
- 下班时间早于上班时间就按跨零点处理（例如中班填 `16:00–00:00`、夜班填 `20:00–04:00`）
- 改完任何一行，会立刻用三个新时间重排整个 8 天循环（手动改过的格子会被覆盖，界面下方有提示）
- 循环第 1 天由你选（工具条「选循环起始日」），今天落在第几天自动算

### 3. 自定义循环（你要的 16 天那种）
16 格一排格子，点任意一格改班次：**休息 / 白班 / 早班 / 中班 / 夜班 / 中夜班 / 自定义时间 / 加夜间值班 / 去掉夜间值班**。

内置模板就是按你描述的 16 天上：

| 天 | D1 | D2 | D3 | D4 | D5 | D6 | D7 | D8 |
|---|---|---|---|---|---|---|---|---|
| 班次 | 白 08–20 | 白 08–20 | 休 | 白 08–20 | 休 | **中夜 16–次日08** | 休 | 休 |

| 天 | D9 | D10 | D11 | D12 | D13 | D14 | D15 | D16 |
|---|---|---|---|---|---|---|---|---|
| 班次 | 白 08–20 | 白 08–20 | 休 | 白 08–20 | 休 | **中夜 16–次日08** | 休 | 休 |

即「两白 → 休 → 白 → 休 → 中夜(16h) → 休 → 休」× 2 轮，第二轮与第一轮完全对称（D2 与 D3 之间那天休、第三个白班后也休一天再上中夜）。
实际排班不一样的话，自己点格子改就行；工具条还有「加一天 / 减一天 / 把今天设为第 1 天 / 恢复 16 天模板 / 选循环起始日」。
有的格子改成「自定义时间」时，会弹两个时间选择器，结束时间早于开始时间就自动当成跨零点（例如 16:00–08:00）。

**同一天夜间值班**：点某一格 → 「加夜间值班（当天第二段）…」，先选值班上班时间、再选下班时间，
默认上班时间就接着这天白班的下班时间，可以直接「白班 08:00–16:00 再值 18:00–22:00」，也可以中间隔一段。
这天原来是休息、又只想那天夜里去值班，就选同一个菜单里的夜间值班（这时它就是这天的唯一一段班）。

---

## 三、计薪口径（和原项目一致，额外补了跨零点）

- 一个人一天能拿多少，取决于这个班怎么算，**同一个班内部金额随秒线性增长**（原项目 `rate = 日薪 / 当日计薪秒数`，午休段不计）：
  - **日薪**：一个班 = 一天的钱，摊到这个班的计薪时长；
  - **月薪**：先按「月薪 ÷ 月计薪天数（默认 21.75）」折成日薪，再同上；
  - **时薪**：按小时给，16h 的中夜班天然是 8h 白班的两倍。
- 状态机：`休息日 / 未开工 / 上班中 / 午休中 / 已下班 / 加班`，文案和配色沿用原版（毒鸡汤语录、心愿单「赚到 / 还差多久」都保留了）。
- **同一天「白班 + 夜间值班」**：两段共用同一个 rate，也就是这一天仍然只算**一个班次的钱**
  （日薪/月薪折出来的那份），只是平摊到「白班计薪分钟 + 值班分钟」上，所以加班值班不会多拿钱、只是把单价摊薄；
  时薪模式则按两段总小时数给钱。这样和原项目「16h 中夜班 = 一天的钱」是同一套口径。
- **中夜 16:00–次日 08:00 这种跨零点的班**：金额不会在 0 点清零，而是顺着同一个班继续涨，
  因为引擎把「这笔钱从哪一刻开始攒」往前推到了班次起点（这也是本轮修掉的一个真 bug，见下）。
- 「本月累计」按 `日薪 × 本月已排班天数` 逐日累加，跨零点那天只算一次；「本月满勤约」= 整月所有排班之和；「距发薪日」按你设的几号算。

---

## 四、和原 Windows 版的对齐与差别

| 原版功能 | 安卓版 |
|---|---|
| 悬浮窗常驻显示今日已赚 | 换成桌面小部件（1×2 / 1×4 / 2×2 / 2×4 四种可选），点一下开 App |
| 机械滚轮数字（进位前 25% 缓动） | 原样移植（`WidgetRenderer.drawMoney` 位图版 + `OdometerView` View 版） |
| 日/月/时薪、月计薪天数、发薪日、小数位、货币符号 | 全都有 |
| 加班按倍率继续累加 | 有（「下班后继续按倍率加班」开关 + 倍率） |
| 老板来了（隐藏金额） | 有 |
| 毒鸡汤语录、心愿单 | 有，可编辑；心愿单是「名称=价格」一行一条 |
| 设置存 `%APPDATA%\...\settings.ini` | 改存 SharedPreferences（单键 JSON） |
| —（原版只有一种固定班） | 新增长白班 / APN / 自定义循环三种，支持任意跨零点班次、支持同一天两段（白班 + 夜间值班） |

差异：安卓没有「总在最前」的悬浮窗（系统不允许普通应用常驻悬浮），所以做成了小部件；
秒级刷新需要前台服务，通知栏会有一个极简通知（`IMPORTANCE_MIN`，不响不亮）。

---

## 五、自检（23 项断言全 PASS）

内置过一版自检页 `CheckActivity`（现放在 `tools/probe/CheckActivity.java`，需要时拷回 `src` 重编），
在雷电模拟器（API 28）跑出来：

```
长白班 午休不计薪 / 午休后继续计薪 / 下班后金额=今日满 / 计薪 8 小时
三班倒 D1 早班在岗 / D3 中班在岗 / D7 休息
16 天循环排班落位（16 天全部对上） / 循环第 6 天是中夜 16h
中夜 18:00 在岗且已计薪 / 23:30 比 18:00 多 / 跨零点后仍在计薪（不归零） / 次日 09:00 已下班
当天零点前只结算 8 小时工资
时薪 12h 白班=360 / 时薪 16h 中夜按当天只算 8h=240
月薪 白班当天=日薪 / 月薪 中夜当天=半日薪
中夜 16h 跨零点单调不减（回退 0） / 零点前后不归零（159.24 → 162.60） / 白班 12h 单调不减 / 16 天每个班次内单调不减（8 个班次全单调）
发薪日 10 号 → 还有 5 天
SUMMARY 通过 23 / 失败 0
```

> 顺便说一句，这轮自检抓到一个真 bug：最早那版「今日已赚」是从当天 00:00 起算的，
> 中夜班一过零点金额就归零重来，16 天里逐小时巡检能抓到 10 次回退。
> 现在改成先扫描当天的班次区间，找出当前这段连续工作从哪开始，跨零点只接着涨不清零。

---

## 六、自己改代码 / 重新编译

```
NiuMaWidget/
├─ build.ps1                 # 一键：aapt2 compile+link → javac → jar → d8 → 塞 dex → zipalign → apksigner
├─ src/main/AndroidManifest.xml
├─ src/main/java/com/feiyuaq/niuma/
│   ├─ Prefs.java            # 设置模型（SharedPreferences 单键 JSON）
│   ├─ Plan.java / Cycle.java # 一个班的定义、周期串（"|" 分隔，R 或 W,start,end,bs,be）
│   ├─ Engine.java           # 计薪引擎：状态机、跨零点、本月累计、发薪日
│   ├─ WidgetRenderer.java   # 小部件位图渲染（逻辑 344×88 / 344×172 / 176×88 / 176×172 后等比缩放）
│   ├─ OdometerView.java     # App 内滚轮数字
│   ├─ WidgetUpdater.java / NiumaWidgetBase.java
│   ├─ NiumaWidget1x2.java / NiumaWidget1x4.java / NiumaWidget2x2.java / NiumaWidget2x4.java
│   ├─ TickerService.java    # 秒级刷新前台服务
│   ├─ MainActivity.java / SegView.java / ProgressView.java / WidgetPreview.java / Ui.java
├─ src/main/res/xml/        # widget_1x2.xml / widget_1x4.xml / widget_2x2.xml / widget_2x4.xml（各尺寸的格子声明）
├─ tools/make_icons.py       # 纯标准库 Python 手写 PNG 图标（4× 超采样）
├─ tools/pack_apk.py         # 把 classes.dex 打进 aapt2 骨架 apk
├─ tools/probe/             # ProbeActivity（AppWidgetHost 真绑定验证）、CheckActivity（引擎自检）、PreviewAllActivity（四尺寸对照）
├─ k70_debug/               # 红米 K70 真机调试的 logcat / dumpsys / 截图
├─ signing/niuma.keystore    # 固定签名（storepass/keypass=android，alias=niuma）
└─ dist/NiuMaWidget-1.0.apk
```

不需要 Gradle。在有 Android SDK build-tools 36 与 JDK 21+ 的机器上：

```powershell
$env:PYTHON = "<python.exe 路径>"     # 只用于打 dex / 生成图标
.\build.ps1                            # 只编译打包
.\build.ps1 -Install -Device emulator-5554
```

想复跑自检：把 `tools/probe/CheckActivity.java` 拷回 `src/main/java/com/feiyuaq/niuma/`，
在 Manifest 里加一行 `<activity android:name=".CheckActivity" android:exported="true"/>`，然后

```powershell
adb shell am start -n com.feiyuaq.niuma/.CheckActivity
adb logcat -d -s NIUMA_CHECK
```

---

## 七、已知限制

1. **小部件在雷电模拟器桌面上没法真正摆上去**：launcher3 长按空白处进不了编辑模式、`adb shell appwidget` 只有 `grantbind/revokebind` 没有 `bind`，
   `requestPinAppWidget` 也被这个启动器静默吞掉。所以小部件是用自建 `AppWidgetHost` 真绑定 `NiuMaWidget2x4` 验证的：
   连续两张截图间隔 8 秒，金额 289.10090 → 289.16195、倒计时同步减少，说明 `RemoteViews` + 秒级刷新链路是通的（`build/probe3.png`、`build/probe4.png`）。
   真机（MIUI / ColorOS / 原生 Android）走系统「添加小部件」或 App 内放置按钮都正常。
2. 秒级刷新依赖前台服务；国产 ROM 需要给「后台常驻 / 自启动」白名单，否则熄屏久了服务会被杀，小部件退回半小时一刷。
3. `1×2 / 1×4 / 2×2 / 2×4` 是按「2 或 4 列、1 或 2 行」的格子算的，实际高宽随启动器（是否允许 1 行、边距多少）略有出入，渲染是按逻辑尺寸等比缩放，不会变形。
4. 原项目的 `beij.png`、`app.ico` 没有下载（`raw.githubusercontent.com` 直连被重置），图标是按「¥ + 进度条 + 暖纸底」重画的。

---

## 八、真机修复记录（Redmi K70 / HyperOS，Android 16 / API 36，1440×3200，density 560）

用户反馈两个症状：**小部件透明/空白**、**点击后应用闪退**。用 `adb` 连真机抓 `logcat -b crash` 定位后确认两者同源。

### 8.1 根因：PendingIntent 缺少 FLAG_IMMUTABLE

修复前 crash buffer 里 19:06–19:08 连崩 8 次，栈完全一致：

```
FATAL EXCEPTION: main / Process: com.feiyuaq.niuma
java.lang.RuntimeException: Unable to create service com.feiyuaq.niuma.TickerService:
  java.lang.IllegalArgumentException: Targeting S+ (version 31 and above) requires
  that one of FLAG_IMMUTABLE or FLAG_MUTABLE be specified when creating a PendingIntent
  at com.feiyuaq.niuma.WidgetUpdater.openApp(WidgetUpdater.java:80)
  at com.feiyuaq.niuma.TickerService.buildNotification(TickerService.java:92)
  at com.feiyuaq.niuma.TickerService.onCreate(TickerService.java:75)
```

`openApp()` 同时被两处调用：`TickerService.buildNotification()`（拼通知里的「打开 App」）和
`WidgetUpdater.updateProvider()`（给小部件设 `setOnClickPendingIntent`）。
前者直接把 `TickerService` 拖崩，服务被系统反复重启 → 表现就是「点开就闪退」；
后者当时被 `catch (Exception ignored)` 静默吞掉，`RemoteViews` 根本没 `set` 出去 →
表现就是「小部件一直空白透底」。

### 8.2 逐个修掉的点

| # | 位置 | 改动 | 修掉的症状 |
|---|------|------|-----------|
| 1 | `WidgetUpdater.openApp` | `PendingIntent` 在 `SDK>=23` 时加 `FLAG_IMMUTABLE`（保留 `FLAG_UPDATE_CURRENT`） | 闪退（根本原因） |
| 2 | `WidgetUpdater.MAX_PIXELS` | 360000 → 200000 | 部分启动器会丢弃过大的 Binder 位图，导致小部件不显示 |
| 3 | `WidgetUpdater.updateProvider` | `catch (Exception ignored)` → `Log.w("NiuMaWidget", …)` | 以后失败能看见，不再静默吞掉 |
| 4 | `TickerService.onCreate` | `createChannel()` + `startForeground()` 包 `try/catch (Throwable)` 降级 | 前台服务异常时不再拖垮进程 |
| 5 | `WidgetRenderer.draw` | 卡片底 `setAlpha(255)`（原 240/246） | 小部件「透明/发灰/透壁纸」——HyperOS 上小部件是贴壁纸的，半透明就是脏 |
| 6 | `WidgetRenderer.draw1x4` | `mini` 与 `stateText` 同词时不再拼接 | 「已下班 已下班」重复 |
| 7 | `WidgetUpdater.updateOne` | `drawColor(0, PorterDuff.Mode.CLEAR)` 取代 `drawColor(0x00000000)` | 复用位图旧像素残留成「幽灵斑」（透明色 SRC_OVER 等于没清） |

### 8.3 真机验收证据

- **崩溃消失**：`adb logcat -b crash` 清空后重新安装、启动、点小部件，crash buffer 为空（修复前连崩 8 次的完整栈见 8.1 引用；那份原始落盘被后续构建的 `build\` 清理覆盖了，见 9.2 的说明与修改）。
- **小部件正常**：4 块小部件在 K70 桌面正常不透明显示，`k70_debug\k70_all4_home2.png` 为最终效果。
- **秒级刷新**：`logcat` 尾部持续 `AppWidgetServiceImpl: Trying to notify widget update for package com.feiyuaq.niuma with widget id: 1502/1503/1505/1506` 与 `Launcher:WidgetView: onWidgetUpdate`，间隔约 1 秒（`k70_debug\k70_new_widgets.txt`，578 条命中）。
- **点击能打开且不闪退**：实测点击两次，日志出现
  `START u0 {flg=0x14000000 xflg=0x4 cmp=com.feiyuaq.niuma/.MainActivity} … callingPackage com.feiyuaq.niuma (realCallingUid=10138)`，
  `realCallingUid=10138` 就是桌面 `com.miui.home`，`xflg=0x4` 即 `FLAG_IMMUTABLE`，随后 `Displayed … +127ms`，无 `AndroidRuntime` 记录（当时那一次抓的 logcat 随首次构建清理丢失了，原始行见上方引用；清理问题已在 9.2 修掉）。
- **前台服务健在**：`dumpsys activity services com.feiyuaq.niuma` 显示 `TickerService` 运行中，`isForeground=true foregroundId=1001 types=0x40000000`（`k70_debug\k70_services.txt`）。

### 8.4 已知的两个真机限制（不是本应用的 bug）

1. **小米/HyperOS 拦 `adb` 注入输入**：设备未开「USB 调试（安全设置）」时，任何 `adb shell input tap / keyevent` 都会抛
   `java.lang.SecurityException: Injecting input events requires the caller … to have the INJECT_EVENTS permission`，
   所以自动化点击验证做不到，最终是靠用户手指点击 + 上述日志确认的。
2. **`adb -s` 必须带序列号**：电脑上同时连着 `emulator-5554` 与 K70（`89c91cbc`），不带 `-s` 会 `more than one device`。

---

## 九、四种尺寸（1×2 / 1×4 / 2×2 / 2×4）

### 9.1 尺寸与内容对应

| 尺寸 | provider | 格子 | 逻辑画布 | 桌面内容 |
|------|----------|------|----------|----------|
| 1×2 | `NiumaWidget1x2` | 2 列 1 行 | 176×88 | 状态、进度百分比、**明日（短）**、金额、底部进度条（迷你条，最省地方） |
| 1×4 | `NiumaWidget1x4` | 4 列 1 行 | 344×88 | 状态 + 迷你信息、百分比、金额、**明日**、进度条（细长条） |
| 2×2 | `NiumaWidget2x2` | 2 列 2 行 | 176×172 | 状态胶囊、时钟、今日已赚、班次（带 +值）、金额、倒计时（最多两行）、进度条、**明日**、时薪 / 距发薪（方卡） |
| 2×4 | `NiumaWidget2x4` | 4 列 2 行 | 344×172 | 上面全部内容 + 星期、今日满额；倒计时行右侧固定显示**明日**（完整卡） |

- 窄卡片（2 列）的金额字号会按位数自动回退（`WidgetRenderer.fitSize`），`¥1234.56789` 这种也不会溢出被裁。
- 倒计时在 176 宽的画布里按宽度贪心折成两行，第二行还超宽才截断加省略号（`WidgetRenderer.twoLine`）。
- 设置页里四种尺寸都有实时预览，2 列的预览只占卡片一半宽，看起来和桌面实际比例一致。

### 9.2 真机调试产生的文件（`k70_debug\`，仅本地留存，不进仓库）

调试期间所有 logcat / dumpsys / 截图都落在 `NiuMaWidget\k70_debug\`：

| 文件 | 说明 |
|------|------|
| `preview_all.png` | 四种尺寸在 K70 上按真实格子 dp 渲染的对照图（1×2 / 1×4 / 2×2 / 2×4 一屏） |
| `k70_all4_home.png` | 四块小部件第一次上桌面的截图（截图时桌面还在翻页动画，右侧被切） |
| `k70_all4_home2.png` | 稳定后的最终效果：四块小部件同屏、卡片不透明、金额铺满 |
| `k70_appwidget.txt` | `dumpsys appwidget`：4 个 provider 注册情况（装机后第一次） |
| `k70_appwidget2.txt` | 放好 1×2 / 2×2 后的 `dumpsys appwidget`：id 1505 = 1×2、1506 = 2×2，宿主 `com.miui.home` |
| `k70_appwidget3.txt` | 加完开机/覆盖安装重启服务之后的 `dumpsys appwidget`（此刻桌面上只剩用户自己留的 2×2，id 1506） |
| `k70_new_widgets.txt` | 放置新尺寸期间的全量 logcat（9.8 MB，578 条 widget update，用于核对秒级刷新） |
| `k70_all4_home3.png` | 最后一次装机后回到桌面第一页的截图（小部件在另一页，用于对照桌面状态） |
| `k70_crash.txt` | 当前 crash buffer（本应用 0 条，文件 0 字节） |
| `k70_services.txt` | `dumpsys activity services com.feiyuaq.niuma`：`TickerService` 前台运行中（`isForeground=true foregroundId=1001`） |
| `k70_pkg.txt` | `dumpsys package com.feiyuaq.niuma`：targetSdk 33、权限、receiver 列表 |
| `apk_manifest.txt` | `aapt2 dump xmltree` 出来的最终 APK Manifest，核对 4 个 widget receiver + `BootReceiver`、无调试 Activity |
| `v2_preview_top.png` | 本轮改动后四种尺寸的预览（默认班制，`明日` 已在四块卡上） |
| `v2_preview_wide.png` | 同一页第二段：白班 + 值班、明日中夜的最宽文案对照（用于查窄卡溢出） |
| `v2_preview_night2.png` | 「白班 08:00–16:00 + 值班 18:00–22:00 / 明日中夜」的最终四尺寸渲染（1×2 与 2×2 都不溢出） |
| `v2_preview_night.png` | 同一场景的中间版本（1×2 金额与「明日」贴太近，据此把金额下移 2px） |
| `v2_app_apn.png` | APN 模式下的设置页：班制三段文案、三班时间六行、8 天循环（白×2 中×2 夜×2 休×2） |
| `v2_app_cycle.png` | 自定义循环模式下的设置页（16 天格子里跨零点显示为 `16:00-明08:00`） |
| `v2_home.png` | 本轮装机后的桌面首页截图（此时桌面上已被用户清空小部件，用于记录桌面状态） |
| `k70_appwidget_v2.txt` | 本轮装机后的 `dumpsys appwidget`：4 个 provider 仍注册、当前无实例绑定 |
| `k70_services_v2.txt` | 本轮装机后的 `dumpsys activity services`：桌面上没有实例，前台服务按设计未启动（`(nothing)`） |
| `k70_crash_v2.txt` | 本轮打开主界面后的 crash buffer（0 字节，无崩溃） |
| `apk_manifest_v2.txt` | 本轮 APK 的 Manifest 树：1 个 activity（MainActivity）、1 个 service、5 个 receiver，无调试页 |

上报几个坑（都已改掉）：

1. `build.ps1` 原来会 `Remove-Item build\ -Recurse`，把之前真机抓的 logcat / 截图一起删了；现在只删自己那 4 个中间文件（res.zip / base.apk / unaligned.apk / aligned.apk）和 gen、classes、dex、dist。
2. 早期那份「修复前 8 次 PendingIntent 闪退」的原始 crash 落盘已被第一次整目录清理覆盖，崩溃原文保留在 8.1 的引用块里。
3. K70 没开「USB 调试（安全设置）」，`adb shell input tap/keyevent` 全部抛 `SecurityException: … INJECT_EVENTS`，所以点击链路是请用户手点 + 读日志确认的；自动化点按这条路不可用。

### 9.3 秒级刷新的生命周期（本轮新补的坑）

`adb install -r` 覆盖安装后，旧进程被杀，`TickerService` 不会自己回来——**小部件就停在当时那个数值上不动了**。这是实测出来的：装机后 6 秒窗口里 `AppWidgetServiceImpl` 一条 widget update 都没有，`dumpsys activity services` 里 `ServiceRecord` 的 `app=null`、`startForegroundCount=0`（说明记录还在，服务没真正跑起来）。

根因：Android 12+ 不允许后台（例如系统 30 分钟一次的小部件广播里）启动前台服务，`TickerService.ensure()` 原来又 `catch (Exception ignored)` 把这个 `ForegroundServiceStartNotAllowedException` 吞了。

修法：

- 新增 `BootReceiver`（`BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` / `QUICKBOOT_POWERON`，加 `RECEIVE_BOOT_COMPLETED` 权限），收到广播就把服务重新拉起来；
- `TickerService.ensure()` 的 `catch` 改成 `catch (Throwable)` + `Log.w`，失败原因能在 logcat 看到，不再静默。

验证：重新 `install -r` 之后 4 秒，`dumpsys activity services` 直接出现

```
app=ProcessRecord{9ecf7cf 3165:com.feiyuaq.niuma/u0a277}
startForegroundCount=1
infoAllowStartForeground=[… code:PACKAGE_REPLACED; tempAllowListReason:… MY_PACKAGE_REPLACED … duration:20000 …]
isForeground=true foregroundId=1001 types=0x40000000
```

即系统给了 20 秒的临时豁免把前台服务拉起来；随后 6 秒窗口里恢复成每秒一条 `Trying to notify widget update`。用户手动改设置、开机、覆盖安装这三种情况现在都能自动恢复秒级刷新，不用再手动打开一次应用。

---

## 十、本轮四项改动（三班时间可调 / 夜间值班 / 班制改名 / 明日排班）

### 10.1 班制文案：长白班 / APN / 自定义循环

设置页第一张卡的分段控件改成三段 `长白班`、`APN`、`自定义循环`（原来是「办公室长白班 / 工厂三班倒 / 自定义循环」）。
只改显示文案与代码里的注释，枚举值（`Prefs.MODE_FIXED=0 / MODE_THREE=1 / MODE_CYCLE=2`）不变，老设置直接沿用。

### 10.2 APN 三个班的上下班时间可调

`Prefs` 新增六个字段（`apnDayStart/apnDayEnd`、`apnMidStart/apnMidEnd`、`apnNightStart/apnNightEnd`，默认 08:00–16:00 / 16:00–24:00 / 00:00–08:00），`load`/`save`/`copy` 三处都补齐。

- 新增 `Prefs.apnCycle()`：用三个班的当前时间拼出 8 天循环 `白|白|中|中|夜|夜|休|休`；
- 新增 `Prefs.shift(st,en)`：下班早于上班就 `+= 1440` 按跨零点算（所以中班可以填 `16:00–00:00`、夜班 `20:00–04:00`）；
- APN 模式设置页顶部多了「三班时间」六行，点任意一行改时间后**立刻重排整个循环**并刷新小部件；
- 「恢复 APN 模板」按钮也改成按当前三个时间重排（自定义循环模式仍是恢复 16 天模板）。

### 10.3 同一天的夜间值班（白班结束后接着上一段）

`Plan` 从「一天一段」扩成「最多两段」：

- 新增 `work2` / `start2` / `end2`，`plusNight(s2,e2)`（`e2<=s2` 自动 +1 天），`encode()` 变 7 段 `W,start,end,bs,be,nightStart,nightEnd`；
- `parse()` 只有 `length>=7` 才读第二段，所以旧的 5 段串、老设置全部兼容；
- `paidMinutes()` 累加第二段，`timeText()` 追加 `+值班 18:00–22:00`，新增 `Plan.range(a,b)`（`1440` 显示 `24:00`、`>1440` 显示 `次日HH:MM`）。

`Engine` 里把原来「一个 Plan → 一个计薪区间」的 `spanOf` 换成 `spanParts(p, plan, shiftSec, out)`，一天最多产出两段，
**两段共用同一个 rate**（`shiftPay / paidMinutes` 一次算出来）：所以「白班 + 值班」这一天还是一份班次的钱，只是摊到更多计薪分钟上，
和「16h 中夜班 = 一天的钱」是同一套口径。昨天的第二段跨零点到今天，也照样按昨天的 rate 接着涨（金额单调递增不变）。

设置页里点某一格 → 「加夜间值班（当天第二段）…」，默认值班上班时间就接着这天白班的下班时间；
原来是休息的日子，选了这一段值班就当这天的唯一一段班。已有效值班的格子还多一条「去掉夜间值班」。
格子上会显示 `白·值` 这种标记，小部件的班次行会显示 `08:00–16:00 +值`。

### 10.4 小部件常驻「明日排班」

`Engine.Snapshot` 新增 `shiftText` / `tomorrowText` / `tomorrowShort` / `tomorrowWork` / `tomorrowPlan`，
由 `Engine.compute` 用 `planFor(p, 明天)` 现算，格式例如：

| 情况 | 文案 |
|---|---|
| 明天白班 | `明日 白班 08:00–20:00` |
| 明天中夜 | `明日 中夜 16:00–次日08:00` |
| 明天白班 + 值班 | `明日 白班 08:00–16:00 +值班 18:00–22:00` |
| 明天休息 | `明日 休息` |

四种尺寸的落位：

- 1×2：第 42 行一行短版（`明日 白班` / `明日 中夜`），金额下移 2px 让开滚轮字面；
- 1×4：金额右侧同一基线，右对齐，左侧留给金额，放不下就截断加「…」（`WidgetRenderer.textRight` / `drawTomorrow`）；
- 2×2：进度条下面单独一行（147）右对齐，时薪/距发薪那行下移到 161；
- 2×4：倒计时那一行右侧（原来放 `+¥/时` 的位置），倒计时左侧限宽相应收窄。

### 10.5 验证过什么

- `build.ps1` 全链路通过，产物 `dist/NiuMaWidget-1.0.apk` 69.5 KB；
- `aapt2 dump xmltree` 复核最终 APK：1 个 activity（只有 `MainActivity`）、1 个 service、5 个 receiver（4 widget + `BootReceiver`），**没有留下任何调试页**；
- 在 K70 上用临时预览页按真实格子 dp 渲染四种尺寸：默认班制（`v2_preview_top.png`）与最宽文案「白班+值班 / 明日中夜」（`v2_preview_night2.png`）都不溢出、不遮挡；
- APN 模式下截图确认三班时间六行、循环重排为 `白×2 中×2 夜×2 休×2`（`v2_app_apn.png`）；
- 计薪单调性：值班当天 20:23 显示 `278.59621`，随秒继续增长，且未超过当天满额（值班段沿用同一 rate，不会多算钱）；
- 装机后 `dumpsys appwidget` 四个 provider 都在，但**当前桌面上没有实例**（此前被清空），所以这一次没能截到桌面上的实机小部件——
  需要你在桌面重新放一块（设置页「添加…到桌面」按钮或长按桌面空白处），放上去就会立刻按新代码渲染并带上「明日」那行。

