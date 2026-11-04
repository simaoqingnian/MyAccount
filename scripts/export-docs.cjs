const path=require('node:path');
const {pathToFileURL}=require('node:url');
const {chromium}=require(process.argv[2]||'playwright');
const root=path.resolve(__dirname,'..');
(async()=>{
  const browser=await chromium.launch({executablePath:process.env.HEJI_BROWSER_PATH||'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',headless:true});
  try{
    const page=await browser.newPage();
    await page.goto(pathToFileURL(path.join(root,'docs/需求与设计说明.html')).href);
    await page.evaluate(()=>document.fonts.ready);
    await page.pdf({path:path.join(root,'docs/需求与设计说明.pdf'),format:'A4',printBackground:true,preferCSSPageSize:true,displayHeaderFooter:true,headerTemplate:'<span></span>',footerTemplate:'<div style="width:100%;font-size:8px;color:#6b7a67;text-align:center;font-family:Arial">HEJI / MyAccount &nbsp; · &nbsp; <span class="pageNumber"></span> / <span class="totalPages"></span></div>'});
    const phone=await browser.newPage({viewport:{width:390,height:844},deviceScaleFactor:1,reducedMotion:'reduce'});
    await phone.goto(pathToFileURL(path.join(root,'prototype/index.html')).href+'#charts-history');
    await phone.locator('[data-action="chart-year-expand"][data-year="2025"]').click();
    await phone.evaluate(()=>{const main=document.getElementById('app-main'),legend=document.querySelector('.history-table-legend');main.scrollTop+=legend.getBoundingClientRect().top-main.getBoundingClientRect().top;});
    await phone.screenshot({path:path.join(root,'design/screens/10-history.png')});
    console.log('Updated PDF and history screenshot.');
  }finally{await browser.close();}
})();
