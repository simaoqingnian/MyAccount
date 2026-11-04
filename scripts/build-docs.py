from pathlib import Path
import html
import markdown

ROOT = Path(__file__).resolve().parents[1]
FILES = [("research", "01-鲨鱼记账功能调研.md"), ("prd", "02-禾记APP需求文档.md"), ("design", "03-原型设计与开发交接.md"), ("android", "06-项目图标与连续记账更新.md"), ("android-base", "05-Android开发与打包说明.md")]
parts = []
for anchor, filename in FILES:
    text = (ROOT / 'docs' / filename).read_text(encoding='utf-8')
    rendered = markdown.markdown(text, extensions=['tables', 'fenced_code', 'toc', 'sane_lists'])
    parts.append(f'<article id="{anchor}">{rendered}</article>')
style = '''
*{box-sizing:border-box}html{scroll-behavior:smooth}body{margin:0;background:#f4f5ee;color:#283d34;font-family:"Microsoft YaHei","Segoe UI",sans-serif;font-size:14px;line-height:1.9}nav{position:sticky;top:0;z-index:3;background:#174e42;color:#fff;padding:16px 5%;display:flex;gap:28px;align-items:center;font-size:12px}nav a{color:#fff;text-decoration:none}nav strong{font-size:17px;margin-right:auto}main{max-width:1050px;margin:38px auto;padding:0 28px}.cover{padding:28px 0 36px;border-bottom:1px solid #cdd9c5}.cover small{letter-spacing:2px;color:#697b61}.cover h1{font-size:40px;font-weight:500;line-height:1.45}.cover p{color:#6d7968}article{scroll-margin-top:80px;padding:20px 0 45px}article+article{border-top:2px solid #174e42}h1{font-size:29px;margin:28px 0 22px}h2{font-size:21px;color:#174e42;margin:36px 0 16px}h3{font-size:17px;margin:28px 0 12px}p{margin:12px 0}table{border-collapse:collapse;width:100%;font-size:12px;margin:20px 0;line-height:1.8}th,td{border:1px solid #d5decf;padding:10px 12px;text-align:left;vertical-align:top;overflow-wrap:anywhere}th{background:#e4ecdc;color:#214937;font-weight:600}tbody tr:nth-child(even){background:#eef2e8}a{color:#326950;text-underline-offset:3px}li{padding:3px 0}code{background:#e8eddf;padding:2px 5px;border-radius:4px;font-family:Consolas,monospace;font-size:.9em;overflow-wrap:anywhere}pre{white-space:pre-wrap;background:#e8eddf;padding:18px;border-radius:12px;line-height:1.6;overflow:auto}pre code{padding:0}blockquote{border-left:3px solid #93ad84;margin:20px 0;padding:0 18px}img{max-width:100%}.print-button{border:1px solid #ffffff80;background:none;color:white;padding:6px 12px;border-radius:6px;cursor:pointer;font:inherit}.flow{display:flex;flex-wrap:wrap;gap:10px;margin:22px 0;padding:20px;background:#e8efdf;border-radius:12px}.flow span{background:white;border:1px solid #b5c9a7;border-radius:8px;padding:9px 14px;font-size:12px}.flow i{align-self:center;font-style:normal;color:#6d865c}.footer{padding:25px 0;color:#74846b;font-size:11px}
@media(max-width:700px){nav{gap:12px;flex-wrap:wrap;padding:12px 18px}nav strong{width:100%}main{padding:0 17px;margin-top:20px}.cover h1{font-size:30px}h1{font-size:25px}table{display:block;overflow:auto}th,td{min-width:100px}.print-button{display:none}}
@media print{@page{size:A4;margin:16mm 13mm 18mm}body{background:white;font-size:10px;line-height:1.7}nav{display:none}main{max-width:none;margin:0;padding:0}.cover{break-after:page;padding:30mm 0 0;border:0}.cover h1{font-size:32px}.cover img{margin-top:25px;width:100%}article{padding:0;scroll-margin:0}article+article{break-before:page;border:0}h1{font-size:23px}h2{font-size:16px;margin-top:22px}h3{font-size:13px}h1,h2,h3{break-after:avoid}table{font-size:9px;line-height:1.6;display:table}th,td{padding:6px;min-width:0}tr{break-inside:avoid}thead{display:table-header-group}pre{font-size:9px;break-inside:avoid}a{color:#28543d;text-decoration:none}.flow{break-inside:avoid}p{orphans:3;widows:3}.footer{display:none}}
'''
body = ''.join(parts)
# Render the small workflow locally, so the document works offline without a diagram CDN.
start = body.find('<pre><code class="language-mermaid">')
if start >= 0:
    end = body.index('</code></pre>', start) + len('</code></pre>')
    flow = '<div class="flow"><span>当月总览</span><i>→</i><span>记一笔：类型 / 类别 / 金额</span><i>→</i><span>本地保存</span><i>→</i><span>明细 / 日历 / 统计 / 预算</span><i>→</i><span>编辑删除后重算</span></div>'
    body = body[:start] + flow + body[end:]
page = f'''<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>禾记 · 调研、需求与设计说明</title><style>{style}</style></head><body><nav><strong>禾记 / 产品文档</strong><a href="#research">竞品调研</a><a href="#prd">MVP 需求</a><a href="#design">原型交接</a><a href="#android">Android 开发</a><a href="../prototype/index.html">打开原型 ↗</a><button class="print-button" onclick="window.print()">打印 / PDF</button></nav><main><section class="cover"><small>MYACCOUNT / PRODUCT & ANDROID DELIVERY</small><h1>禾记个人记账 APP<br>调研、需求与设计说明</h1><p>Android 优先 · 离线个人记账 MVP<br>2026 年 11 月 4 日 · 用户已确认 MVP 方向</p><p>包含官方功能证据、业务规则、开发拆分、验收条件与原型交接。<br>已提供原生 Android 0.2.0 调试 APK；按用户要求未安装测试。</p><img src="../design/sunburst-concept.png" alt="Sunburst 原创视觉探索"><p style="font-size:10px">封面图为 AI 视觉探索，具体金额、日期和功能以本文需求及交互原型为准。</p></section>{body}<div class="footer">禾记 · MyAccount · 2026-11-04 / 可维护源文件为 docs 目录内 Markdown</div></main></body></html>'''
target = ROOT / 'docs' / '需求与设计说明.html'
target.write_text(page, encoding='utf-8')
print(f'Built: {target}')
