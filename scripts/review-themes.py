"""Review shared theme assets and the browser sample, without installing Android apps."""
from pathlib import Path
import base64
import io
import json
import re
import subprocess
import time
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from PIL import Image
import websocket

ROOT=Path(__file__).resolve().parents[1]
THEMES=ROOT/'design/themes'
ARTIFACTS=ROOT/'artifacts'
ARTIFACTS.mkdir(exist_ok=True)
checks=[]
def expect(condition,name):
    if not condition: raise AssertionError(name)
    checks.append(name)

palettes=json.loads((THEMES/'palettes.json').read_text(encoding='utf-8'))
css=(THEMES/'theme-tokens.css').read_text(encoding='utf-8')
native=(ROOT/'app/src/main/java/com/simaoqingnian/myaccount/ThemePalette.java').read_text(encoding='utf-8')
styles=ET.parse(ROOT/'app/src/main/res/values/styles.xml').getroot()
for p in palettes:
    expect(p['id'] in css and p['paper'] in css and p['ink'] in css,p['id']+' generated CSS matches palette')
    expect('0xFF'+p['ink'][1:] in native and '0xFF'+p['paper'][1:] in native,p['id']+' native palette includes matching background and text')
    name='Theme.Heji' if p['id']=='titanium' else 'Theme.Heji.'+p['id'].title()
    style=next(s for s in styles if s.attrib.get('name')==name)
    attrs={item.attrib['name']:item.text for item in style}
    expect(attrs['android:colorAccent']==p['accent'] and attrs['android:windowBackground']==p['paper'],p['id']+' system/dialog theme matches palette')

def luminance(color):
    rgb=[int(color[i:i+2],16)/255 for i in (1,3,5)]
    values=[v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in rgb]
    return sum(a*b for a,b in zip(values,[.2126,.7152,.0722]))
def contrast(a,b):
    hi,lo=sorted([luminance(a),luminance(b)],reverse=True)
    return (hi+.05)/(lo+.05)
contrast_results={}
for p in palettes:
    pairs=[('ink','paper'),('muted','paper'),('accent','paper'),('cardInk','card')]
    contrast_results[p['id']]={f'{a}/{b}':round(contrast(p[a],p[b]),2) for a,b in pairs}
    expect(all(v>=4.5 for v in contrast_results[p['id']].values()),p['id']+' primary, secondary, accent and card text contrast >= 4.5')
for name,foreground in [('material_titanium.png','#24272B'),('material_champagne.png','#30251A')]:
    image=Image.open(ROOT/'app/src/main/res/drawable-nodpi'/name).convert('RGB')
    minimum=min(contrast(foreground,'#%02X%02X%02X'%pixel) for pixel in set(image.get_flattened_data()))
    expect(minimum>=4.5,name+' text contrast across every material shade >= 4.5')

apk=ROOT/'artifacts/Heji-0.3.0-debug.apk'
with zipfile.ZipFile(apk) as z:
    for name in ['material_titanium.png','material_champagne.png','peach_mascot.png']:
        entry=next(n for n in z.namelist() if n.startswith('res/') and n.endswith('/'+name))
        packed=Image.open(io.BytesIO(z.read(entry))).convert('RGBA')
        original=Image.open(ROOT/'app/src/main/res/drawable-nodpi'/name).convert('RGBA')
        expect(packed.size==original.size and packed.tobytes()==original.tobytes(),name+' APK pixels equal the shared browser asset')

profile=ARTIFACTS/'theme-review-profile'
profile.mkdir(exist_ok=True)
port_file=profile/'DevToolsActivePort'
port_file.unlink(missing_ok=True)
chrome=r'C:\Program Files\Google\Chrome\Application\chrome.exe'
process=subprocess.Popen([chrome,'--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=0','--user-data-dir='+str(profile),'about:blank'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,creationflags=subprocess.CREATE_NO_WINDOW)
socket=None
try:
    deadline=time.monotonic()+15
    while not port_file.exists() and time.monotonic()<deadline: time.sleep(.1)
    port=int(port_file.read_text().splitlines()[0])
    targets=json.load(urllib.request.urlopen(f'http://127.0.0.1:{port}/json/list',timeout=5))
    target=next(t for t in targets if t['type']=='page')
    socket=websocket.create_connection(target['webSocketDebuggerUrl'],timeout=12,suppress_origin=True)
    counter=0;errors=[]
    def cdp(method,params=None):
        global counter
        counter+=1;current=counter
        socket.send(json.dumps({'id':current,'method':method,'params':params or {}}))
        while True:
            response=json.loads(socket.recv())
            if response.get('method')=='Runtime.exceptionThrown':errors.append(response['params'])
            if response.get('id')==current:
                if 'error' in response: raise RuntimeError(response['error'])
                return response.get('result',{})
    def evaluate(expression):
        result=cdp('Runtime.evaluate',{'expression':expression,'returnByValue':True,'awaitPromise':True})
        if 'exceptionDetails' in result:raise RuntimeError(result['exceptionDetails'])
        return result.get('result',{}).get('value')
    def ready():
        for _ in range(100):
            if evaluate("document.readyState==='complete'&&document.querySelectorAll('.category').length>0"):return
            time.sleep(.05)
        raise RuntimeError('Preview did not finish loading')
    def choose(theme):evaluate(f"document.querySelector('.theme-toggle [data-theme-choice=\"{theme}\"]').click()")
    cdp('Page.enable');cdp('Runtime.enable');cdp('Emulation.setFocusEmulationEnabled',{'enabled':True});cdp('Emulation.setDeviceMetricsOverride',{'width':1440,'height':1280,'deviceScaleFactor':1,'mobile':False})
    cdp('Page.navigate',{'url':(THEMES/'preview.html').as_uri()});ready()
    evaluate("localStorage.removeItem('heji-theme-study-v1');location.hash=''")
    cdp('Page.reload');ready()
    expect(evaluate('document.documentElement.dataset.theme')=='titanium','browser fresh default is titanium')
    for theme in ['titanium','sage','peach']:
        choose(theme)
        expect(evaluate(f"document.documentElement.dataset.theme==='{theme}'&&document.querySelectorAll('[data-theme-choice][aria-pressed=true]').length===2"),theme+' both theme selectors stay synchronized')
        expect(evaluate(f"localStorage.getItem('heji-theme-study-v1')==='{theme}'"),theme+' browser preference is remembered')
        expect(evaluate("[...document.querySelectorAll('.screen')].every(el=>getComputedStyle(el).backgroundColor===getComputedStyle(document.querySelector('.screen')).backgroundColor)"),theme+' all three preview pages share the palette')
        expect(evaluate("[...document.querySelectorAll('.mini-budget')].every(el=>el.getBoundingClientRect().height<=6&&el.getBoundingClientRect().bottom<=el.closest('.mini').getBoundingClientRect().bottom)"),theme+' theme thumbnail progress bars remain inside their miniature cards')
        if theme in ['titanium','peach']:
            data=cdp('Page.captureScreenshot',{'format':'png','captureBeyondViewport':True,'clip':{'x':0,'y':0,'width':1440,'height':1280,'scale':1}})
            (THEMES/theme/'browser-preview.png').write_bytes(base64.b64decode(data['data']))
    evaluate("document.querySelector('[data-category=交通]').click();document.querySelector('#note').focus()")
    expect(evaluate("!document.querySelector('#note-history').hidden"),'note candidates appear on focus')
    evaluate("document.querySelector('#note-history button').click()")
    expect(evaluate("document.querySelector('#note-history').hidden&&document.querySelector('#note').value==='地铁'"),'candidate click fills full note and hides history')
    choose('titanium')
    expect(evaluate("document.querySelector('#note').value==='地铁'&&document.querySelector('#amount').textContent==='32.00'&&document.querySelector('#selected-category').textContent==='交通'"),'browser theme change preserves draft inputs')
    cdp('Page.reload');ready()
    expect(evaluate("document.documentElement.dataset.theme==='titanium'"),'browser selected theme survives reload')
    for width in [360,390,430]:
        cdp('Emulation.setDeviceMetricsOverride',{'width':width,'height':844,'deviceScaleFactor':1,'mobile':True})
        for theme in ['titanium','peach','sage']:
            choose(theme)
            expect(evaluate('document.documentElement.scrollWidth<=innerWidth'),f'{width}px {theme} no horizontal overflow')
            expect(evaluate("getComputedStyle(document.querySelector('#categories')).gridTemplateColumns.split(' ').length===5"),f'{width}px {theme} five category columns')
            expect(evaluate("document.querySelector('#continue').getBoundingClientRect().bottom<=document.querySelector('#entry-frame .screen').getBoundingClientRect().bottom"),f'{width}px {theme} keypad actions fit the entry canvas')
            expect(evaluate("document.querySelector('#theme-applied').getBoundingClientRect().bottom<=document.querySelector('#appearance-frame .screen').getBoundingClientRect().bottom"),f'{width}px {theme} all theme cards and footer fit')
    expect(not errors,'browser no uncaught JavaScript errors')
    report={'date':'2026-11-10','scope':'shared native/browser assets and desktop browser; no Android installation or device screenshots','checks':checks,'contrast':contrast_results,'browser':cdp('Browser.getVersion')['product'],'androidDeviceVerified':False}
    (THEMES/'review-results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'passed':len(checks),'contrast':contrast_results,'scope':report['scope']},ensure_ascii=True))
finally:
    if socket is not None:
        try:cdp('Browser.close')
        except Exception:pass
        socket.close()
    try:process.wait(timeout=8)
    except subprocess.TimeoutExpired:process.terminate()
