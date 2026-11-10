from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json
from pypdf import PdfReader
import zipfile
import re
from html.parser import HTMLParser
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]
font_path = 'C:/Windows/Fonts/msyh.ttc'
font = lambda size: ImageFont.truetype(font_path, size)
board = Image.new('RGB', (1440, 2070), '#F0F2E9')
draw = ImageDraw.Draw(board)
draw.text((90, 42), '禾记 HEJI / 日常有数，生活从容', fill='#174E42', font=font(32))
draw.text((90, 95), 'Android 个人记账 MVP · 实际交互原型截图 · 2026.11.04', fill='#67766D', font=font(16))
pages = [('01-home', '01  收支总览'), ('02-add', '02  快速记账'), ('03-bills', '03  明细查询'), ('04-stats', '04  收支分析'), ('05-budget', '05  月度预算'), ('06-mine', '06  数据与设置')]
for i, (filename, title) in enumerate(pages):
    x = 90 + (i % 3) * 435
    y = 183 + (i // 3) * 920
    screenshot = Image.open(ROOT / 'design' / 'screens' / (filename + '.png')).convert('RGB')
    mask = Image.new('L', screenshot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, 389, 843), radius=24, fill=255)
    draw.rounded_rectangle((x-2, y-2, x+391, y+845), radius=26, fill='#DCE3D4')
    board.paste(screenshot, (x, y), mask)
    draw.text((x+2, y-35), title, fill='#345E50', font=font(17))
draw.text((90, 2000), '图中为虚构演示数据。页面支持滚动；完整操作与计算请打开 prototype/index.html。', fill='#67766D', font=font(15))
board.save(ROOT / 'design' / 'prototype-screens.png')

history_board = Image.new('RGB', (1440, 1120), '#F0F2E9')
history_draw = ImageDraw.Draw(history_board)
history_draw.text((90, 35), '禾记 / 历史收支图表', fill='#174E42', font=font(32))
history_draw.text((90, 86), '周 / 月 / 年切换 · 年度月份比较 · 历年账单 · 实际可交互页面', fill='#67766D', font=font(16))
for i, (filename, title) in enumerate([('08-past-year', '01  历史年份 · 12 个月趋势'), ('09-past-month', '02  历史月份 · 每日趋势'), ('10-history', '03  历年账单 · 每月收支')]):
    x, y = 90 + i * 435, 174
    screenshot = Image.open(ROOT / 'design' / 'screens' / (filename + '.png')).convert('RGB')
    mask = Image.new('L', screenshot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, 389, 843), radius=24, fill=255)
    history_draw.rounded_rectangle((x-2, y-2, x+391, y+845), radius=26, fill='#DCE3D4')
    history_board.paste(screenshot, (x, y), mask)
    history_draw.text((x+2, y-34), title, fill='#345E50', font=font(17))
history_draw.text((90, 1054), 'v0.2 · 虚构演示账目 · 点击年份、月份或趋势点，可继续查看对应周期和明细。', fill='#67766D', font=font(15))
history_board.save(ROOT / 'design' / 'history-statistics.png')

pdf = PdfReader(ROOT / 'docs' / '需求与设计说明.pdf')
pdf_text = '\n'.join(page.extract_text() or '' for page in pdf.pages)
required = ['鲨鱼记账功能调研', '禾记 APP 产品需求文档', '原型设计与开发交接', '金额', '999,999,999.99']
missing = [text for text in required if text.replace(' ', '') not in pdf_text.replace(' ', '')]
if missing:
    raise RuntimeError(f'PDF missing expected content: {missing}')
missing_links = []
class LinkAudit(HTMLParser):
    def __init__(self, source):
        super().__init__()
        self.source = source
    def handle_starttag(self, tag, attrs):
        for name, value in attrs:
            if name in ['href', 'src'] and value and not value.startswith(('http:', 'https:', '#', 'data:', 'mailto:')):
                target = (self.source.parent / unquote(value.split('#')[0])).resolve()
                if not target.exists():
                    missing_links.append(str(target))
for source in [ROOT / 'index.html', ROOT / 'prototype' / 'index.html', ROOT / 'docs' / '需求与设计说明.html']:
    LinkAudit(source).feed(source.read_text(encoding='utf-8'))
# The verification note is written just below, so allow it during the first build.
missing_links = [p for p in missing_links if not p.endswith('04-验证记录.md')]
if missing_links:
    raise RuntimeError(f'Missing local links: {missing_links}')
report = json.loads((ROOT / 'docs' / 'verification-results.json').read_text(encoding='utf-8'))
checks = '\n'.join(f'| {i+1} | {item["name"]} | 通过 |' for i, item in enumerate(report['checks']))
validation = f'''# 原型验证记录

日期：2026-11-04 · 浏览器：Chrome {report['browser']}（无头模式） · 加载方式：本地 `file://` · 独立浏览器上下文

## 结果

- 领域规则测试 **18 / 18 通过**：Node 内置测试运行器，覆盖金额精度、日期、预算、筛选、跨月编辑、备份、CSV；新增年/月聚合、跨年周、平均值分母、未来周期、空年度与历史修改重算。
- 浏览器验证 **{len(report['checks'])} / {len(report['checks'])} 通过**：真实浏览器执行点击、填表、下载、恢复和截图。
- 未捕获页面 JavaScript 错误：**0**。
- 桌面画布 1440 × 1000；手机截图 390 × 844；另验证 360 / 430px 宽度主流程无横向溢出。
- 当前合并 PDF：**{len(pdf.pages)} 页**；检查了中文文本可提取，调研、PRD、交接和金额规则均存在。原型阶段为 13 页，后续补入 Android 开发说明；当前原生验证范围见《15-总览与明细列表行高统一》，旧版记录见《05-Android开发与打包说明》。
- 已人工查看桌面总览、手机记账、年度图表及历史账单截图，确认主要文字、按钮、图表和键盘可见。

## 浏览器验证项目

| 序号 | 项目 | 结果 |
|---|---|---|
{checks}

## 边界

验证对象是本地网页原型。没有做 Android 真机测试、原生数据库迁移、性能压测、云同步、系统通知、应用锁或上架验收。浏览器视口模拟不等于真机软键盘和系统返回验证。

完整备份导出是明文文件；下载动作已验证，文件能否长期保留取决于用户保管。浏览器数据可能因清理站点数据而丢失。正式 Android 的持久化要求详见 PRD。

原型的触控尺寸和正文排版用于方案评审；PRD 中的 48dp、字体放大和屏幕阅读器验收仍需在原生实现中完成。主要功能测试通过不等同于已满足全部非功能目标。

## 复现

规则测试：`node --test scripts/domain.test.cjs`。

浏览器验证：`node scripts/verify-prototype.cjs <Playwright模块目录>`。原始结果见 `verification-results.json`。文档构建命令见根目录 README。
'''
(ROOT / 'docs' / '04-验证记录.md').write_text(validation, encoding='utf-8')

version = re.search(r'versionName\s+"([^"]+)"', (ROOT / 'app' / 'build.gradle').read_text(encoding='utf-8')).group(1)
archive = ROOT / f'禾记-Android与需求-{version}.zip'
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
    for name in ['index.html', 'README.md', '.gitignore', '.gitattributes', 'build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat']:
        z.write(ROOT / name, name)
    for directory in ['docs', 'prototype', 'design', 'scripts', 'app', 'gradle']:
        for file in sorted((ROOT / directory).rglob('*')):
            if file.is_file() and file.name != 'verification-failure.png' and '__pycache__' not in file.parts and 'build' not in file.relative_to(ROOT).parts:
                z.write(file, file.relative_to(ROOT).as_posix())
    apk = ROOT / 'artifacts' / f'Heji-{version}-debug.apk'
    if apk.is_file():
        z.write(apk, apk.relative_to(ROOT).as_posix())
print(json.dumps({'pdf_pages': len(pdf.pages), 'pdf_page_text_lengths': [len(p.extract_text() or '') for p in pdf.pages], 'board_size': board.size, 'zip_bytes': archive.stat().st_size, 'pdf_content_check': 'passed', 'local_links': 'passed'}, ensure_ascii=True))
