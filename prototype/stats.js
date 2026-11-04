'use strict';

function chartReport(){return D.statistics(data,state.statsPeriod,state.statsAnchor,state.statsType);}
function chartPeriodTitle(report=chartReport()){
  if(report.period==='year')return state.statsAnchor.slice(0,4)+' 年';
  if(report.period==='month')return state.statsAnchor.slice(0,4)+' 年 '+Number(state.statsAnchor.slice(5,7))+' 月';
  return report.start.replaceAll('-','.')+' — '+report.end.slice(5).replace('-','.');
}
function chartAnchorForMonth(month){return month===D.TODAY.slice(0,7)?D.TODAY:month+'-01';}
function chartFrame(){
  return `<div class="chart-view-switch"><button data-action="chart-mode" data-mode="trend" class="${state.statsMode==='trend'?'active':''}">趋势分析</button><button data-action="chart-mode" data-mode="history" class="${state.statsMode==='history'?'active':''}">历年账单 ${icon('chevron')}</button></div>`;
}
function statsPage(){
  if(state.statsMode==='history')return historyPage();
  const report=chartReport(),label=state.statsType==='expense'?'支出':'收入',unit=state.statsPeriod==='year'?'月':'日';
  const current=report.start<=D.TODAY&&report.end>=D.TODAY;
  let selected=report.buckets.findIndex(b=>b.key===state.statsSelected&&!b.future);
  if(selected<0)selected=report.buckets.reduce((best,b,i)=>!b.future&&b.total>report.buckets[best].total?i:best,0);
  const bucket=report.buckets[selected];
  const top=report.groups[0];
  const avgNote=report.period==='year'?`按${report.elapsed}个已历月份计算，含当月`: `按${report.elapsed}个已历自然日计算，含零记录日`;
  const previous=D.adjacentPeriod(state.statsPeriod,state.statsAnchor,-1),next=D.adjacentPeriod(state.statsPeriod,state.statsAnchor,1);
  return `${chartFrame()}<div class="chart-type-tabs">${[['expense','支出'],['income','收入']].map(([type,name])=>`<button data-action="chart-type" data-type="${type}" class="${state.statsType===type?'selected':''}" aria-pressed="${state.statsType===type}">${name}</button>`).join('')}</div>
    <div class="segmented period-tabs" aria-label="统计周期">${[['week','周'],['month','月'],['year','年']].map(([id,name])=>`<button data-action="chart-period" data-period="${id}" class="${state.statsPeriod===id?'selected':''}" aria-pressed="${state.statsPeriod===id}">${name}</button>`).join('')}</div>
    <div class="chart-period-nav"><button data-action="chart-shift" data-delta="-1" aria-label="上一个周期" ${previous?'':'disabled'}>${icon('back')}</button><button data-action="chart-picker" class="period-title">${chartPeriodTitle(report)}${icon('down')}</button><button data-action="chart-shift" data-delta="1" aria-label="下一个周期" ${next?'':'disabled'}>${icon('chevron')}</button></div>
    <section class="chart-summary"><div><span>${state.statsPeriod==='year'?'年度':state.statsPeriod==='month'?'月度':'本周'}总${label}</span><strong id="chart-total">¥ ${money(report.total)}</strong><small>${report.rows.length} 笔记录${current?' · 截至 11 月 4 日':''}</small></div><div><span>${unit}均${label}</span><b id="chart-average">¥ ${money(report.average)}</b></div></section>
    <section class="trend-card"><div class="chart-card-title">${state.statsPeriod==='year'?'每月':'每日'}${label}趋势 <span class="hint">单位：元</span></div>${lineChart(report,selected)}<div class="chart-selection"><span><i></i>${state.statsPeriod==='year'?bucket.key.replace('-',' 年 ')+' 月':bucket.key.slice(5).replace('-',' 月 ')+' 日'}<strong>¥ ${money(bucket.total)}</strong></span><button data-action="chart-bucket-open" data-key="${bucket.key}">${state.statsPeriod==='year'?'查看本月':'查看明细'} ${icon('chevron')}</button></div><p class="chart-footnote">${avgNote}${report.buckets.some(b=>b.future)?'；未来时段以「—」表示':''}。</p></section>
    <div class="section-title"><h2>${label}排行</h2><span class="hint">分类 · 金额 / 占比</span></div>
    <div class="chart-ranks">${report.groups.length?report.groups.map((c,i)=>`<button class="rank" data-action="chart-category" data-category="${c.id}"><span class="rank-number">${String(i+1).padStart(2,'0')}</span>${catIcon(c)}<span class="rank-main"><span class="rank-label"><span>${c.name}</span><strong>¥${money(c.total)}</strong></span><span class="rank-bar"><i style="width:${c.total/report.total*100}%;background:${c.color}"></i></span><span class="rank-percent">${c.count} 笔 · ${data.preferences.hideAmounts?'••':(c.total/report.total*100).toFixed(1)}%</span></span>${icon('chevron','rank-chevron')}</button>`).join(''):empty('这个周期还没有'+label,'可切换到有记录的年份或月份。',false)}</div>
    <button class="chart-all-details" data-action="chart-all">查看这个周期的全部${label} ${icon('chevron')}</button>
    ${report.groups.length?`<details class="chart-distribution"><summary>分类占比图 <span>${top.name}占比最高</span></summary>${distributionChart(report)}</details>`:''}
    <button class="history-callout" data-action="chart-mode" data-mode="history">${icon('calendar')}<span><strong>历年收支，一页看清</strong><small>按年份展开每个月的收入、支出与结余</small></span>${icon('chevron')}</button>`;
}
function lineChart(report,selected){
  const width=320,left=38,right=308,top=18,bottom=130,peak=Math.max(...report.buckets.map(b=>b.total),100);
  const x=i=>left+i*(right-left)/(report.buckets.length-1),y=value=>bottom-value/peak*(bottom-top);
  const live=report.buckets.map((b,i)=>({...b,index:i})).filter(b=>!b.future);
  const points=live.map(b=>`${x(b.index)},${y(b.total)}`).join(' ');
  const compact=n=>data.preferences.hideAmounts?'••':n>=1000000?(n/1000000).toFixed(1)+'万':(n/100).toLocaleString('en-US',{maximumFractionDigits:0});
  const grid=[0,.5,1].map(f=>`<line x1="${left}" y1="${y(peak*f)}" x2="${right}" y2="${y(peak*f)}" stroke="#E6EBDF" stroke-dasharray="3 4"/><text x="${left-7}" y="${y(peak*f)+3}" text-anchor="end">${compact(peak*f)}</text>`).join('');
  const area=live.length?`<path d="M ${x(live[0].index)} ${bottom} L ${points.replaceAll(' ',' L ')} L ${x(live.at(-1).index)} ${bottom} Z" fill="#E6EDDF" opacity=".72"/>`:'';
  const labels=report.buckets.map((b,i)=>{const show=report.period!=='month'||i===0||(i+1)%5===0||i===report.buckets.length-1;return show?`<text x="${x(i)}" y="151" text-anchor="middle" class="${b.future?'future-tick':''}">${b.label}</text>`:'';}).join('');
  const dots=report.buckets.map((b,i)=>b.future?`<text x="${x(i)}" y="${bottom+3}" text-anchor="middle" class="future-tick">—</text>`:`<g data-action="chart-select" data-key="${b.key}" role="button" tabindex="0" aria-label="${b.key} ${state.statsType==='expense'?'支出':'收入'} ${money(b.total)} 元"><title>${b.key}：${money(b.total)} 元</title><circle cx="${x(i)}" cy="${y(b.total)}" r="13" fill="transparent"/><circle cx="${x(i)}" cy="${y(b.total)}" r="${i===selected?5:2.7}" fill="${i===selected?'#174E42':'#fff'}" stroke="#174E42" stroke-width="1.5"/></g>`).join('');
  return `<svg class="history-line-chart" viewBox="0 0 ${width} 162" role="group" aria-label="${report.period==='year'?'按月份':'按日期'}的收支趋势，选择数据点查看金额">${grid}${area}<polyline points="${points}" fill="none" stroke="#174E42" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>${dots}${labels}</svg>`;
}
function distributionChart(report){let cursor=0;const segments=report.groups.map(c=>{const start=cursor;cursor+=c.total/report.total*100;return `${c.color} ${start}% ${cursor}%`;});return `<div class="donut-wrap"><div class="donut" style="background:conic-gradient(${segments.join(',')})"><div class="donut-inner"><span>所选周期总额</span><strong>${money(report.total)}</strong></div></div></div>`;}
function historyPage(){
  const firstYear=data.transactions.length?Math.min(...data.transactions.map(t=>Number(t.occurredOn.slice(0,4)))):Number(D.TODAY.slice(0,4));
  const years=Array.from({length:Number(D.TODAY.slice(0,4))-firstYear+1},(_,i)=>Number(D.TODAY.slice(0,4))-i);
  const all=D.totals(data.transactions),hasHistory=data.transactions.some(t=>t.occurredOn<'2026-11-01');
  return `${chartFrame()}<div class="history-intro"><span class="section-kicker">YOUR MONEY, OVER TIME</span><h2>把每一年，都看清。</h2><p>${firstYear} — ${D.TODAY.slice(0,4)} · 按自然年 / 月汇总</p></div><div class="history-total"><div><span>累计支出</span><strong>¥ ${money(all.expense)}</strong></div><div><span>累计收入</span><b>¥ ${money(all.income)}</b></div></div>${!hasHistory?'<button class="history-demo" data-action="chart-load-history">添加 2024–2026 年虚构演示账目，体验历史趋势 ↗</button>':''}<div class="history-table-legend"><span>年份 / 月份</span><span>收入</span><span>支出</span><span>结余</span></div>${years.map(year=>{const summary=D.yearlySummary(data,year),expanded=state.historyYear===year;return `<section class="year-block"><button class="year-summary ${expanded?'expanded':''}" data-action="chart-year-expand" data-year="${year}" aria-expanded="${expanded}"><span><b>${year}</b><small>${year===2026?'截至 11/04':'全年'} ${expanded?'⌃':'⌄'}</small></span><span>${money(summary.income)}</span><span>${money(summary.expense)}</span><span class="${summary.balance<0?'negative':'positive'}">${money(summary.balance)}</span></button>${expanded?`<div class="month-history">${summary.months.map(m=>`<button class="month-history-row ${m.future?'future-month':''}" data-action="chart-history-month" data-month="${m.month}" ${m.future?'disabled':''} aria-label="${m.month}，收入 ${m.future?'未到':money(m.income)}，支出 ${m.future?'未到':money(m.expense)}，结余 ${m.future?'未到':money(m.balance)}"><span>${Number(m.month.slice(5))} 月 ${m.month===D.TODAY.slice(0,7)?'<i>进行中</i>':''}</span><span>${m.future?'—':money(m.income)}</span><span>${m.future?'—':money(m.expense)}</span><span class="${m.balance<0?'negative':''}">${m.future?'—':money(m.balance)}</span></button>`).join('')}<button class="year-trend-button" data-action="chart-history-year" data-year="${year}">查看 ${year} 年趋势与分类排行 ${icon('chevron')}</button></div>`:''}</section>`;}).join('')}<p class="prototype-note">结余 = 收入 − 支出，不代表账户余额。<br>无记录的历史月份显示 0；未来月份显示「—」。<br>全部历史演示数据均为虚构。</p>`;
}
function chartPeriodModal(){
  const year=Number(state.statsAnchor.slice(0,4));
  const years=Array.from({length:27},(_,i)=>2026-i).map(y=>`<option value="${y}" ${y===year?'selected':''}>${y} 年</option>`).join('');
  return `${modalHead('选择统计周期')}<p class="hint">可以回看历史年份、月份，或选择某一天所在的自然周。</p><label class="modal-label" for="chart-year-input">年份</label><select id="chart-year-input" class="modal-input">${years}</select>${state.statsPeriod==='month'?`<label class="modal-label" for="chart-month-input">月份</label><select id="chart-month-input" class="modal-input">${Array.from({length:12},(_,i)=>`<option value="${String(i+1).padStart(2,'0')}" ${i+1===Number(state.statsAnchor.slice(5,7))?'selected':''}>${i+1} 月</option>`).join('')}</select>`:state.statsPeriod==='week'?`<label class="modal-label" for="chart-week-input">所选日期所在周（周一至周日）</label><input class="modal-input" id="chart-week-input" type="date" min="2000-01-01" max="${D.TODAY}" value="${state.statsAnchor}">`:''}<div id="form-error" class="form-error" role="alert"></div><button class="primary full" data-action="chart-apply-period">查看${state.statsPeriod==='year'?'这一年':state.statsPeriod==='month'?'这个月':'这一周'}</button><button class="text-button" style="margin:12px auto 0" data-action="chart-current">回到当前周期 · 2026.11.04</button>`;
}
function chartDetailModal(m){
  const entries=D.forRange(data,m.start,m.end).filter(t=>t.type===m.type&&(!m.category||t.categoryId===m.category));
  const total=entries.reduce((s,t)=>s+t.amountMinor,0);
  return `${modalHead(m.category?D.category(m.category).name+'明细':'周期明细')}<p class="hint">${m.start} 至 ${m.end}<br>${m.type==='expense'?'支出':'收入'} ${entries.length} 笔 · 合计 ¥${money(total)}</p>${entries.length?groupedList(entries):empty('这段时间还没有记录','选择其他日期再看看。',false)}`;
}
function openChartDetails(category='',range=chartReport()) {openModal('chart-details',{start:range.start,end:range.end,type:state.statsType,category});}
function refreshChart(){render();}
document.addEventListener('click',e=>{
  const button=e.target.closest('[data-action]');if(!button||!button.dataset.action.startsWith('chart-'))return;
  const action=button.dataset.action;
  if(action==='chart-mode'){state.statsMode=button.dataset.mode;refreshChart();main.scrollTop=0;}
  if(action==='chart-type'){state.statsType=button.dataset.type;state.statsSelected=null;refreshChart();}
  if(action==='chart-period'){state.statsPeriod=button.dataset.period;state.statsSelected=null;refreshChart();main.scrollTop=0;}
  if(action==='chart-shift'){const anchor=D.adjacentPeriod(state.statsPeriod,state.statsAnchor,Number(button.dataset.delta));if(anchor){state.statsAnchor=anchor;state.statsSelected=null;refreshChart();}}
  if(action==='chart-picker')openModal('chart-period');
  if(action==='chart-apply-period'){
    const year=document.getElementById('chart-year-input').value;
    let anchor=state.statsPeriod==='year'?year+'-01-01':state.statsPeriod==='month'?year+'-'+document.getElementById('chart-month-input').value+'-01':document.getElementById('chart-week-input').value;
    if(!D.validDate(anchor)){document.getElementById('form-error').textContent='请选择 2000-01-01 至演示今天之间的周期';return;}
    if(state.statsPeriod==='month')anchor=chartAnchorForMonth(anchor.slice(0,7));
    state.statsAnchor=anchor;state.statsSelected=null;closeModal();refreshChart();main.scrollTop=0;
  }
  if(action==='chart-current'){state.statsAnchor=D.TODAY;state.statsSelected=null;closeModal();refreshChart();}
  if(action==='chart-select'){state.statsSelected=button.dataset.key;refreshChart();}
  if(action==='chart-bucket-open'){
    if(state.statsPeriod==='year'){state.statsPeriod='month';state.statsAnchor=chartAnchorForMonth(button.dataset.key);state.statsSelected=null;refreshChart();main.scrollTop=0;}
    else openChartDetails('',{start:button.dataset.key,end:button.dataset.key});
  }
  if(action==='chart-category')openChartDetails(button.dataset.category);
  if(action==='chart-all')openChartDetails();
  if(action==='chart-year-expand'){state.historyYear=state.historyYear===Number(button.dataset.year)?null:Number(button.dataset.year);refreshChart();}
  if(action==='chart-history-month'){state.statsMode='trend';state.statsPeriod='month';state.statsAnchor=chartAnchorForMonth(button.dataset.month);state.statsSelected=null;refreshChart();main.scrollTop=0;}
  if(action==='chart-history-year'){state.statsMode='trend';state.statsPeriod='year';state.statsAnchor=button.dataset.year+'-01-01';state.statsSelected=null;refreshChart();main.scrollTop=0;}
  if(action==='chart-load-history'){confirmAction('添加历史演示账目？','会补充 2024 年 1 月至 2026 年 10 月的虚构收支，用于体验历史图表。已有账目保留，同 ID 的演示账目不会重复添加。',()=>{const ids=new Set(data.transactions.map(t=>t.id));if(mutate(next=>next.transactions.push(...D.historySeed().filter(t=>!ids.has(t.id))))){closeModal();refreshChart();toast('历史演示账目已添加');}},'添加演示账目');}
});
document.addEventListener('keydown',e=>{if((e.key==='Enter'||e.key===' ')&&e.target.matches('[data-action="chart-select"]')){e.preventDefault();e.target.dispatchEvent(new MouseEvent('click',{bubbles:true}));}});
document.addEventListener('change',e=>{if(e.target.id==='chart-year-input'&&state.statsPeriod==='week'){const input=document.getElementById('chart-week-input');input.value=e.target.value+'-01-01';}});
