# 禾记 · 个人记账 APP 需求与原型

本项目交付：鲨鱼记账公开资料调研、Android 个人记账 MVP 需求、高保真可交互网页原型、Sunburst 视觉概念图。调研日期为 2026-11-04；用户已确认优先个人日常记账 MVP。

**v0.2 已补齐鲨鱼记账式图表组织：周/月/年趋势、历史年份和月份选择、分类排行、历年收支账单。** [直接打开年度图表](prototype/index.html#charts-year) · [直接打开历年账单](prototype/index.html#charts-history)。

**推荐从 [交付首页](index.html) 打开。无需安装依赖或启动服务，直接双击即可。**

| 文件 | 用途 |
|---|---|
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

本次不是 Android APK，不含云同步、会员、自动支付抓取、OCR 或生物识别。AI 图是视觉参考，生成图中数值/文字不作为规格；交互原型和 PRD 才是开发依据。

## 文件结构

```text
MyAccount/
  index.html                  交付入口
  docs/                       调研、PRD、交接、合并 HTML/PDF、验证记录
  prototype/                  无外部运行依赖的 HTML / CSS / JavaScript
    domain.js                 金额、校验、聚合、导出和演示数据
    app.js                    页面与交互
    stats.js                  周/月/年图表、历年账单和统计下钻
  design/                     Sunburst 概念图、提示词、实际原型截图
  scripts/                    文档构建和有意义的规则/浏览器验证
```

## 维护与验证

原型运行不依赖 Node。修改后可用本地 Node 执行：

```powershell
node --test scripts/domain.test.cjs
```

文档 HTML 从 3 份 Markdown 构建，需 Python 的 `markdown` 模块：

```powershell
python scripts/build-docs.py
```

浏览器验证和 PDF 生成脚本使用 Playwright（开发验证依赖），可将已安装模块目录作为参数传入：

```powershell
node scripts/verify-prototype.cjs "你的 Playwright 模块绝对路径"
```

脚本默认查找 Windows Chrome；如需其他环境，可设置 `HEJI_BROWSER_PATH`。它在独立浏览器上下文中运行演示数据，不读取日常浏览器资料。脚本同时输出桌面/手机截图、JSON 验证记录与文档 PDF。

推荐后续顺序：评审 P0 → 建立 Android 原生数据层 → 记账闭环 → 查询分析 → 预算与备份 → 数据可靠性和设备兼容测试。

## GitHub 同步

远端：`git@github.com:simaoqingnian/MyAccount.git`，主分支为 `main`。当前 Windows 工作目录沿用 HappyFarm 的提交身份，通过正在运行的 FastGithub 和本机 OpenSSH 连接 GitHub。

后续在当前目录提交并推送：

```powershell
git add --all
git commit -m "Describe the change"
git push
```

文档、PDF、设计图片、原型和验证脚本均纳入版本管理。根目录的 ZIP 为可重新生成的本地交付包，已通过 `.gitignore` 排除；本地密钥与代理配置不进入仓库。
