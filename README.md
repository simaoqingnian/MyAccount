# 禾记 · 个人记账 APP

本项目包含鲨鱼记账公开资料调研、个人记账 MVP 需求、可交互网页原型、Sunburst 视觉概念图，以及基于原型开发的 **Android 原生应用**。调研与首版开发日期为 2026-11-04；用户已确认优先个人日常记账 MVP。

**新主题设计样板（2026-11-10）：** [Sunburst 钛银金属主题](design/themes/titanium/sunburst-styleboard.png) · [Sunburst 奶油桃桃可爱主题](design/themes/peach/sunburst-styleboard.png) · [切换钛银 / 青禾绿 / 奶油桃桃预览](design/themes/preview.html)。两套新主题已接入下方 0.3.2 APK，可在“我的 → 外观主题”或记账页切换。

**APK：[artifacts/Heji-0.3.2-debug.apk](artifacts/Heji-0.3.2-debug.apk)**。包名 `com.simaoqingnian.myaccount`，版本 `0.3.2 (11)`，支持 Android 8.0 / API 26 及以上，使用本项目独立的调试签名。APK 是本地构建产物，未提交到 Git；克隆仓库后可按下方命令生成。

需要一起带走源码、APK、需求和原型时，可使用本地交付包 [禾记-Android与需求-0.3.2.zip](禾记-Android与需求-0.3.2.zip)（不含本地 SDK、缓存和签名密钥）。执行 `python scripts/package-delivery.py` 可重新生成。

应用采用 Java 原生界面与 SQLite 私有数据库，复用 FilmWord 的 JDK 17、Android SDK 35，以及当前电脑缓存中的 Gradle 8.7 / Android Gradle Plugin 8.6.1。首次启动为空账本，使用手机的真实日期；在“我的 → 载入演示账目”可追加近三年的虚构数据。按用户要求，**没有安装到手机或模拟器测试**。构建、静态检查及桌面账务 / 备份规则验证的具体结果见 [0.3.2 列表更新说明](docs/15-总览与明细列表行高统一.md)。

**v0.2 已补齐鲨鱼记账式图表组织：周/月/年趋势、历史年份和月份选择、分类排行、历年收支账单。** [直接打开年度图表](prototype/index.html#charts-year) · [直接打开历年账单](prototype/index.html#charts-history)。

**Android 0.3.2 更新（2026-11-10）：总览最近账目与明细统一行高，采用明细页的 60dp 最小高度、上下各 8dp 留白，所有主题一致。**

**Android 0.3.1 更新（2026-11-10）：默认钛银改用缎面雾银，去掉分类格、数字键和卡片上的斜亮带，以柔和微纹与边缘高光保留金属质感。** [查看前后对比](design/themes/titanium/texture-comparison.png)。

**Android 0.3.0 更新（2026-11-10）：新增默认“钛银”金属主题与“奶油桃桃”可爱主题，保留青禾绿；我的和记账页可进入外观主题。原生与预览共用配色、金属材质和桃子素材，切换保留输入与筛选。**

**Android 0.2.6 更新（2026-11-10）：从后台返回且焦点在金额框时使用 APP 数字键盘，抑制系统输入法误恢复；历史备注默认隐藏，点击备注框后显示横向候选，选中、完成输入或返回金额时收起。**

**Android 0.2.5 更新（2026-11-10）：收起系统输入法后恢复数字键盘；历史备注改为备注框上方可左右滑动的小标签，点击直接采用；覆盖升级沿用原数据库与签名，打包时核对证书，禁止静默换签名。直接覆盖安装，不要先卸载或清除数据。**

**Android 0.2.4 调整：所有自绘列表采用单条底部分隔线，去除左右竖线和相邻项双线；图表切换移入标题栏，收支与周/月/年并排，收紧汇总与历年账单布局。**

**Android 0.2.3 新增：明细每天日期右侧显示当天支出、收入合计；合计跟随筛选且包含尚未加载的记录；月份移入标题栏，合并筛选行，收紧顶部与账目行留白。**

**Android 0.2.2 新增：默认选中当前排序第一个项目；键盘分为“保存”和“继续”，“继续”保存后留在录入页，清空金额和备注、保留项目和日期；日期左右按钮可逐天切换。**

**Android 0.2.1 调整：日期与备注固定在键盘上方、项目每行 5 列、图标大小保留、缩小表单留白，按最近点选顺序排列项目并在本机记忆。**

**Android 0.2.0 新增：40 个支出项目、12 个收入项目、72 个离线图标、自定义项目及同项目历史备注。** “记一笔 → ＋ 自定义”可选图标建项目；“我的 → 项目与图标”可管理项目。点击备注框可直接选用当前项目最近的历史备注。手动选过日期后，本轮使用的后续新增账目沿用该日期；重新开启应用默认今天，旋转和暂时切到后台不重置。直接编辑旧账目不会改变连续记账日期。

新版沿用原包名与调试签名，versionCode 增加为 11。数据库在 0.2.0 由版本 1 升为 2，只新增项目表，本轮保持版本 2；旧账目、预算与设置的表结构保留。升级安装行为尚未实机验证。

**推荐从 [交付首页](index.html) 打开。无需安装依赖或启动服务，直接双击即可。**

| 文件 | 用途 |
|---|---|
| [artifacts/Heji-0.3.2-debug.apk](artifacts/Heji-0.3.2-debug.apk) | 本地生成的 Android 安装包 |
| [docs/15-总览与明细列表行高统一.md](docs/15-总览与明细列表行高统一.md) | 当前版本：总览与明细共用紧凑账目行高 |
| [docs/14-缎面雾银材质更新.md](docs/14-缎面雾银材质更新.md) | 0.3.1 缎面雾银材质调整与前后对比 |
| [docs/13-钛银与奶油桃桃主题更新.md](docs/13-钛银与奶油桃桃主题更新.md) | 0.3.0 三套主题接入与视觉校对说明 |
| [docs/12-后台恢复与按需历史备注更新.md](docs/12-后台恢复与按需历史备注更新.md) | 0.2.6 后台恢复使用金额键盘、历史备注按需显示 |
| [docs/11-备注横条与升级数据保留更新.md](docs/11-备注横条与升级数据保留更新.md) | 0.2.5 备注横条、收起输入法恢复键盘、升级保留与签名保护 |
| [docs/10-列表分隔线与图表紧凑布局更新.md](docs/10-列表分隔线与图表紧凑布局更新.md) | 0.2.4 列表底部单线、图表与历年账单布局 |
| [docs/09-明细每日合计与紧凑布局更新.md](docs/09-明细每日合计与紧凑布局更新.md) | 0.2.3 每日收支合计与明细布局 |
| [docs/08-默认项目与连续保存更新.md](docs/08-默认项目与连续保存更新.md) | 0.2.2 默认第一项目、保存/继续、前后一天按钮 |
| [docs/07-记账页布局与最近项目更新.md](docs/07-记账页布局与最近项目更新.md) | 0.2.1 日期固定、紧凑五列布局、最近项目排序 |
| [docs/06-项目图标与连续记账更新.md](docs/06-项目图标与连续记账更新.md) | 0.2.0 项目、图标、历史备注、连续记账日期说明 |
| [docs/05-Android开发与打包说明.md](docs/05-Android开发与打包说明.md) | 0.1.0 历史交付与 FilmWord 环境复用说明 |
| [app/src/main](app/src/main) | 原生 Java 界面、SQLite 存储及 Android 资源 |
| [prototype/index.html](prototype/index.html) | 可点击、可记账、可编辑、可导出恢复的交互原型 |
| [docs/需求与设计说明.html](docs/需求与设计说明.html) | 合并的调研、PRD 与设计说明，浏览器阅读 |
| [docs/需求与设计说明.pdf](docs/需求与设计说明.pdf) | 可分享/打印的文档 |
| [docs/01-鲨鱼记账功能调研.md](docs/01-鲨鱼记账功能调研.md) | 功能证据、版本与会员边界、来源链接 |
| [docs/02-禾记APP需求文档.md](docs/02-禾记APP需求文档.md) | P0/P1/P2、业务规则、数据模型、验收与工作量估算 |
| [docs/03-原型设计与开发交接.md](docs/03-原型设计与开发交接.md) | 页面、视觉令牌、演示步骤、实现边界 |
| [design/prototype-overview.png](design/prototype-overview.png) | 已实现原型的桌面预览 |
| [design/prototype-screens.png](design/prototype-screens.png) | 主要手机页面的静态预览 |
| [design/history-statistics.png](design/history-statistics.png) | 历史年图、月图、历年账单的实际界面 |
| [design/sunburst-concept.png](design/sunburst-concept.png) | 用户指定模型生成的视觉探索 |
| [docs/04-验证记录.md](docs/04-验证记录.md) | 真实执行的规则与浏览器验证结果 |

## 原型可体验内容

总览、收入/支出录入、详情编辑删除、关键词与类别筛选、日历、周/月/年趋势、周期总额与均值、历史年度/月份选择、分类排行与同周期明细、历年收支表、总预算和分类预算、隐藏金额、CSV 导出、完整 JSON 备份恢复。

首次运行带虚构数据，日期固定为 2026-11-04。初始共 218 笔：本月 14 笔，收入 12,000.00、支出 2,310.00、结余 9,690.00 元；另有 2024-01 至 2026-10 共 204 笔历史演示。2026 年支出 32,580.00、2025 年支出 35,972.00。本月预算仍为 5,000.00 元。

已使用过原型的浏览器会保留已有账目，不自动注入历史样本。若未看到历史数据，进入“图表 → 历年账单”，点击“添加历史演示账目”并确认，可保留原账目后追加。也可在“我的”确认重置为最新完整样本。数据仅保存在当前浏览器 `localStorage`，请使用同一浏览器、同一文件路径继续体验。

网页原型和 Android 应用分别保存数据；Android 可导入网页原型 / 旧版的 schema 1 备份；新版导出 schema 2，额外保留自定义项目，旧网页原型无法导入新版备份。网页固定演示日期，Android 使用系统日期，导入日期不得晚于手机当天。两者均不含云同步、会员、自动支付抓取、OCR 或生物识别。AI 图是视觉参考，生成图中数值/文字不作为规格。

## 构建 Android APK

当前电脑在项目目录执行以下命令即可离线编译并运行 Lint 静态检查，全程不安装应用：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-android.ps1
```

脚本自动定位 `D:\03_work\09_android\FilmWord\.jdk\jdk-17` 和 `.android-sdk`，选择现有 Gradle 缓存，生成本项目的 `local.properties` 并校验已有 `.android/debug.keystore` 的证书，最后输出到 `artifacts/Heji-0.3.2-debug.apk`。原签名文件缺失或与 `gradle/debug-signing.sha256` 不一致时停止打包；请从自己的保管位置恢复原签名文件。环境、签名文件和构建产物均不进入 Git。复用的只是工具链，本项目没有 FilmWord 的业务代码或数据。

FilmWord 目录或缓存目录不同时可以指定参数；`-Online` 允许 Gradle 获取缺失依赖：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-android.ps1 -FilmWordRoot 'D:\你的路径\FilmWord' -GradleCacheRoot 'D:\你的缓存\.gradle'
```

主题素材已随源码提供，构建 APK 不需要调用图片服务。维护主题时可执行 `python scripts/build-theme-assets.py` 重新生成原生 / 浏览器共用资源；`python scripts/review-themes.py` 校对素材、对比度和浏览器布局，需要本机 Pillow、websocket-client 与 Chrome。该检查不安装 Android 应用。

纯 Java 账务规则和 SQLite 升级保留 / 签名保护可在桌面检查，不连接设备：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-ledger.ps1
python scripts/check-upgrade.py
```

## 文件结构

```text
MyAccount/
  index.html                  交付入口
  app/                        Android 原生界面 / SQLite / 资源
  gradle/                     Gradle 8.7 Wrapper
  artifacts/                  APK 与本地检查产物（Git 忽略）
  docs/                       调研、PRD、交接、合并 HTML/PDF、验证记录
  prototype/                  无外部运行依赖的 HTML / CSS / JavaScript
    domain.js                 金额、校验、聚合、导出和演示数据
    app.js                    页面与交互
    stats.js                  周/月/年图表、历年账单和统计下钻
  design/                     Sunburst 概念图、提示词、实际原型截图
  scripts/                    APK 构建、桌面账务检查、文档与原型验证
```

## 维护与验证

原型运行不依赖 Node。修改后可用本地 Node 执行：

```powershell
node --test scripts/domain.test.cjs
```

文档 HTML 从调研、PRD、原型交接及各版 Android 说明 Markdown 构建，需 Python 的 `markdown` 模块：

```powershell
python scripts/build-docs.py
```

浏览器验证和 PDF 生成脚本使用 Playwright（开发验证依赖），可将已安装模块目录作为参数传入：

```powershell
node scripts/verify-prototype.cjs "你的 Playwright 模块绝对路径"
```

脚本默认查找 Windows Chrome；如需其他环境，可设置 `HEJI_BROWSER_PATH`。它在独立浏览器上下文中运行演示数据，不读取日常浏览器资料。脚本同时输出桌面/手机截图、JSON 验证记录与文档 PDF。

原生首版已接入记账闭环、查询分析、历史图表、预算和文件备份。后续按需要开展真机界面、文件选择器、进程恢复及不同系统版本的兼容验证；这些结果不包含在本次交付中。

## GitHub 同步

远端：`git@github.com:simaoqingnian/MyAccount.git`，主分支为 `main`。当前 Windows 工作目录沿用 HappyFarm 的提交身份，使用本机 OpenSSH 和已有密钥认证。

本次 FastGithub 将 `github.com` 解析到本地，其 22 端口转发在 SSH 握手前断开。已仅在本仓库的 `core.sshCommand` 中选用 GitHub SSH 443 线路（当前可达地址 `20.205.243.160`），并以 `HostKeyAlias=github.com` 严格校验已有主机密钥。SSH 认证与推送成功，未修改全局 Git、系统 SSH 或 FastGithub 配置；远端 URL 保持不变。该线路属于当前电脑的本地配置，未进入源码；后续如地址变化，可重新选择可达的 SSH 线路。

后续在当前目录提交并推送：

```powershell
git add --all
git commit -m "Describe the change"
git push
```

Android 源码、文档、PDF、设计图片、原型和维护脚本均纳入版本管理。APK、构建缓存、环境路径、签名文件及根目录 ZIP 已通过 `.gitignore` 排除；本地密钥与代理配置不进入仓库。
