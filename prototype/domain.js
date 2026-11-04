(function (root) {
  'use strict';
  const TODAY = '2026-11-04';
  const MAX_MINOR = 99999999999;
  const categories = [
    ['food','餐饮','expense','food','#AAC0A4'],['transport','交通','expense','bus','#A9C6CC'],
    ['shopping','购物','expense','bag','#EAC4A0'],['daily','日用','expense','basket','#C9CF9E'],
    ['housing','住房','expense','home','#345E50'],['utilities','水电','expense','bolt','#C2B8D6'],
    ['fun','娱乐','expense','game','#E4B8AF'],['health','医疗','expense','heart','#D49F91'],
    ['study','学习','expense','book','#B0BED9'],['travel','旅行','expense','plane','#8BBAB3'],
    ['gift','人情','expense','gift','#D2B8CD'],['other','其他','expense','grid','#B8BDB5'],
    ['salary','工资','income','wallet','#345E50'],['bonus','奖金','income','gift','#AAC0A4'],
    ['side','兼职','income','bag','#EAC4A0'],['otherIncome','其他收入','income','grid','#A9C6CC']
  ].map(([id,name,type,icon,color])=>({id,name,type,icon,color}));
  const category = id => categories.find(c=>c.id===id);
  const money = n => (n/100).toLocaleString('en-US',{minimumFractionDigits:2,maximumFractionDigits:2});
  function amount(text) {
    const raw = String(text).trim();
    if (!/^\d{1,9}(\.\d{1,2})?$/.test(raw)) throw new Error('请输入最多两位小数的有效金额');
    const [yuan,cents=''] = raw.split('.');
    const value = Number(yuan)*100+Number(cents.padEnd(2,'0'));
    if (!Number.isSafeInteger(value)||value<1||value>MAX_MINOR) throw new Error('金额须为 0.01–999,999,999.99 元');
    return value;
  }
  function validDate(date) {
    if (typeof date!=='string'||!/^\d{4}-\d{2}-\d{2}$/.test(date)||date<'2000-01-01'||date>TODAY) return false;
    const d=new Date(date+'T12:00:00Z');
    return !Number.isNaN(d.getTime())&&d.toISOString().slice(0,10)===date;
  }
  function monthValid(month) { return typeof month==='string'&&/^20\d{2}-(0[1-9]|1[0-2])$/.test(month); }
  function validateTransaction(t) {
    if (!t || typeof t!=='object') throw new Error('账目结构无效');
    if(typeof t.id!=='string'||!t.id||t.id.length>100) throw new Error('账目 ID 无效');
    const c=category(t.categoryId);
    if(!c||c.type!==t.type) throw new Error('类别与收支类型不匹配');
    if(!Number.isSafeInteger(t.amountMinor)||t.amountMinor<1||t.amountMinor>MAX_MINOR) throw new Error('备份含无效金额');
    if(!validDate(t.occurredOn)) throw new Error('请选择 2000-01-01 至演示今天的真实日期');
    if(typeof t.note!=='string'||[...t.note].length>100) throw new Error('备注最多 100 个字符');
    for(const key of ['createdAt','updatedAt']) if(typeof t[key]!=='string'||!Number.isFinite(Date.parse(t[key]))) throw new Error('账目时间格式无效');
    return {id:t.id,type:t.type,amountMinor:t.amountMinor,categoryId:t.categoryId,occurredOn:t.occurredOn,note:t.note.trim(),createdAt:t.createdAt,updatedAt:t.updatedAt};
  }
  function validateBudget(b) {
    if(!b||typeof b!=='object'||Array.isArray(b)||!b.categoryLimits||typeof b.categoryLimits!=='object'||Array.isArray(b.categoryLimits)) throw new Error('预算结构无效');
    const validMinor=v=>Number.isSafeInteger(v)&&v>0&&v<=MAX_MINOR;
    if(b.totalMinor!==null&&!validMinor(b.totalMinor)) throw new Error('总预算必须大于零，或留空取消设置');
    const limits={}; let sum=0;
    for(const [id,value] of Object.entries(b.categoryLimits)) {
      if(!category(id)||category(id).type!=='expense'||!validMinor(value)) throw new Error('分类预算无效');
      limits[id]=value;sum+=value;
    }
    if(sum>0&&b.totalMinor===null) throw new Error('请先设置月总预算');
    if(sum>(b.totalMinor||0)) throw new Error('分类预算合计不能超过月总预算');
    return {totalMinor:b.totalMinor,categoryLimits:limits};
  }
  function validateData(data) {
    if(!data||!Array.isArray(data.transactions)||data.transactions.length>10000) throw new Error('备份账目无效或超过 10,000 笔');
    const ids=new Set();
    const transactions=data.transactions.map(t=>{
      const clean=validateTransaction(t);
      if(ids.has(clean.id)) throw new Error('备份含重复账目 ID');
      ids.add(clean.id);return clean;
    });
    if(!data.budgets||typeof data.budgets!=='object'||Array.isArray(data.budgets)) throw new Error('备份缺少预算数据');
    const budgets={};
    for(const [month,budget] of Object.entries(data.budgets)) {
      if(!monthValid(month)) throw new Error('预算月份无效');
      budgets[month]=validateBudget(budget);
    }
    if(!data.preferences||typeof data.preferences.hideAmounts!=='boolean') throw new Error('备份偏好设置无效');
    return {transactions,budgets,preferences:{hideAmounts:data.preferences.hideAmounts}};
  }
  function parseBackup(text) {
    let envelope;try{envelope=JSON.parse(text);}catch{throw new Error('文件不是有效的 JSON 备份');}
    if(!envelope||envelope.schemaVersion!==1) throw new Error('不支持此备份版本');
    if(typeof envelope.exportedAt!=='string'||!Number.isFinite(Date.parse(envelope.exportedAt))) throw new Error('备份导出时间无效');
    return validateData(envelope.data);
  }
  const forMonth=(data,month)=>data.transactions.filter(t=>t.occurredOn.startsWith(month)).sort((a,b)=>b.occurredOn.localeCompare(a.occurredOn)||b.createdAt.localeCompare(a.createdAt));
  function totals(rows) {const income=rows.filter(t=>t.type==='income').reduce((s,t)=>s+t.amountMinor,0),expense=rows.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amountMinor,0);return {income,expense,balance:income-expense};}
  function grouped(rows,type) {return categories.filter(c=>c.type===type).map(c=>({...c,total:rows.filter(t=>t.categoryId===c.id).reduce((s,t)=>s+t.amountMinor,0),count:rows.filter(t=>t.categoryId===c.id).length})).filter(c=>c.total>0).sort((a,b)=>b.total-a.total);}
  function filterRows(rows,f) {return rows.filter(t=>(f.type==='all'||t.type===f.type)&&(!f.category||t.categoryId===f.category)&&(!f.day||t.occurredOn===f.day)&&(!f.search||(t.note+' '+category(t.categoryId).name).toLowerCase().includes(f.search.toLowerCase())));}
  function csv(rows) {
    const cell=v=>{let s=String(v);if(/^[\s]*[=+\-@]/.test(s))s="'"+s;return '"'+s.replace(/"/g,'""')+'"';};
    const lines=[['账目ID','日期','类型','类别','金额','币种','备注'],...rows.map(t=>[t.id,t.occurredOn,t.type==='expense'?'支出':'收入',category(t.categoryId).name,(t.amountMinor/100).toFixed(2),'CNY',t.note])];
    return '\ufeff'+lines.map(r=>r.map(cell).join(',')).join('\r\n');
  }
  const dateUTC=value=>new Date(value+'T12:00:00Z');
  const isoDate=date=>date.toISOString().slice(0,10);
  function shiftDay(value,days) {const date=dateUTC(value);date.setUTCDate(date.getUTCDate()+days);return isoDate(date);}
  function periodRange(period,anchor) {
    if(!['week','month','year'].includes(period)||!validDate(anchor)) throw new Error('统计周期或日期无效');
    const date=dateUTC(anchor),year=date.getUTCFullYear(),month=date.getUTCMonth();
    if(period==='week'){const start=shiftDay(anchor,-((date.getUTCDay()+6)%7));return {start,end:shiftDay(start,6)};}
    if(period==='month')return {start:anchor.slice(0,7)+'-01',end:isoDate(new Date(Date.UTC(year,month+1,0,12)))};
    return {start:year+'-01-01',end:year+'-12-31'};
  }
  function adjacentPeriod(period,anchor,delta) {
    const range=periodRange(period,anchor);
    const next=delta<0?shiftDay(range.start,-1):shiftDay(range.end,1);
    return next<'2000-01-01'||next>TODAY?null:next;
  }
  function forRange(data,start,end) {return data.transactions.filter(t=>t.occurredOn>=start&&t.occurredOn<=end).sort((a,b)=>b.occurredOn.localeCompare(a.occurredOn)||b.createdAt.localeCompare(a.createdAt));}
  function statistics(data,period,anchor,type) {
    if(!['income','expense'].includes(type))throw new Error('统计收支类型无效');
    const range=periodRange(period,anchor),selected=forRange(data,range.start,range.end).filter(t=>t.type===type);
    const total=selected.reduce((sum,t)=>sum+t.amountMinor,0),buckets=[];
    if(period==='year'){
      for(let m=1;m<=12;m++){const key=anchor.slice(0,4)+'-'+String(m).padStart(2,'0');buckets.push({key,label:m+'月',future:key>TODAY.slice(0,7),total:selected.filter(t=>t.occurredOn.startsWith(key)).reduce((s,t)=>s+t.amountMinor,0)});}
    }else{
      for(let key=range.start;key<=range.end;key=shiftDay(key,1))buckets.push({key,label:period==='week'?['日','一','二','三','四','五','六'][dateUTC(key).getUTCDay()]:String(Number(key.slice(8))),future:key>TODAY,total:selected.filter(t=>t.occurredOn===key).reduce((s,t)=>s+t.amountMinor,0)});
    }
    const elapsed=buckets.filter(b=>!b.future&&b.key>=(period==='year'?'2000-01':'2000-01-01')).length;
    return {...range,period,type,rows:selected,total,groups:grouped(selected,type),buckets,elapsed,average:elapsed?Math.round(total/elapsed):0};
  }
  function yearlySummary(data,year) {
    if(!/^20\d{2}$/.test(String(year))||Number(year)>Number(TODAY.slice(0,4)))throw new Error('统计年份无效');
    const months=Array.from({length:12},(_,i)=>{const month=year+'-'+String(i+1).padStart(2,'0'),entries=forMonth(data,month);return {month,...totals(entries),count:entries.length,future:month>TODAY.slice(0,7)};});
    return {year:Number(year),months,...totals(forRange(data,year+'-01-01',year+'-12-31'))};
  }
  function historySeed() {
    const entries=[];
    for(let year=2024;year<=2026;year++)for(let month=1;month<=(year===2026?10:12);month++){
      const prefix=year+'-'+String(month).padStart(2,'0');
      const items=[['housing',150000,2],['food',(650+(year*7+month*31)%400)*100,5],['transport',(90+month*19%160)*100,9],['shopping',(100+month*67%500)*100,16],['fun',(60+month*37%200)*100,22],['salary',(10000+(year-2024)*1000)*100,1]];
      for(const [id,value,day] of items){const date=prefix+'-'+String(day).padStart(2,'0');entries.push({id:'history-'+prefix+'-'+id,type:category(id).type,categoryId:id,amountMinor:value,note:'历史演示 · '+category(id).name,occurredOn:date,createdAt:date+'T12:00:00+08:00',updatedAt:date+'T12:00:00+08:00'});}
    }
    return entries;
  }
  function seed(includeHistory=true) {
    const entries=[
      ['04','food',3200,'工作日午餐'],['04','food',2200,'楼下的咖啡'],['04','transport',600,'地铁通勤'],['04','food',1800,'早餐也要好好吃'],
      ['03','food',6800,'和朋友吃晚饭'],['03','transport',3600,'下雨打车回家'],['03','food',2500,'水果与小零食'],
      ['02','daily',15600,'周末超市采购'],['02','fun',7800,'一场喜欢的电影'],['02','health',8000,'常备药品'],
      ['01','housing',150000,'11 月房租'],['01','shopping',18900,'换季针织衫'],['01','utilities',10000,'电费充值'],['01','salary',1200000,'11 月工资']
    ];
    return {transactions:[...entries.map(([day,c,a,n],i)=>({id:'demo-'+i,type:category(c).type,categoryId:c,amountMinor:a,note:n,occurredOn:'2026-11-'+day,createdAt:`2026-11-${day}T${String(20-i).padStart(2,'0')}:00:00+08:00`,updatedAt:'2026-11-04T12:00:00+08:00'})),...(includeHistory?historySeed():[])],budgets:{'2026-11':{totalMinor:500000,categoryLimits:{food:120000,transport:30000,shopping:50000,housing:180000}}},preferences:{hideAmounts:false}};
  }
  const api={TODAY,MAX_MINOR,categories,category,money,amount,validDate,monthValid,validateTransaction,validateBudget,validateData,parseBackup,forMonth,forRange,totals,grouped,filterRows,csv,seed,historySeed,periodRange,adjacentPeriod,statistics,yearlySummary,shiftDay};
  if(typeof module!=='undefined'&&module.exports) module.exports=api; else root.Heji=api;
})(typeof window==='undefined'?globalThis:window);
