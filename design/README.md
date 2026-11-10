# 设计文件

- `themes/preview.html`：2026-11-10 新增三主题切换样板，可离线对比“钛银 / 青禾绿 / 奶油桃桃”；原生 0.3.0 已接入主题切换，浏览器样板截图不作为设备截图。
- `themes/titanium/`：“钛银”金属主题，指定 Sunburst 模型生成的三屏风格图、提示词和设计令牌。见 [设计说明](themes/titanium/README.md)。
- `themes/peach/`：“奶油桃桃”可爱主题，指定 Sunburst 模型生成的三屏风格图、提示词和设计令牌。见 [设计说明](themes/peach/README.md)。
- `sunburst-concept.png`：2026-11-04 生成，模型 `gpt-image-2.5-sunburst`，使用指定本地技能的 helper 与 Images API。分辨率 1536 × 1024，提示词见 `sunburst-prompt.txt`；未发生失败重试或 Responses 回退。
- `prototype-overview.png`：实际 HTML 原型在桌面评审画布中的截图。
- `prototype-screens.png`：实际手机页面拼接预览，字体、金额和图表来自原型真实渲染。
- `history-statistics.png`：v0.2 新增的历史年度图表、历史月份图表、历年收支表三屏预览；单屏见 `screens/07-year.png` 至 `screens/10-history.png`。
- `screens/`：每个主页面的独立手机截图。

Sunburst 图用于色彩、布局气质和界面方向探索。生成图中的预算、日期、图表占比与原型演示数据不完全一致，请以 PRD 规则和实际原型为准。原型使用可编辑的 HTML/CSS 与 SVG 图标，没有将位图铺成不可交互的页面。

本目录不包含模型 API 密钥、服务配置或用户真实财务数据。
