package com.simaoqingnian.myaccount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.text.Normalizer;

/** Amounts are integer CNY cents. Dates represent the user's local calendar day. */
public final class Ledger {
    public static final long MAX_MINOR = 99_999_999_999L;
    public static final int MAX_ENTRIES = 10_000;
    public static final int MAX_CUSTOM_CATEGORIES = 100;
    public static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    public static final class Category {
        public final String id, name, type, icon, color;
        public Category(String id, String name, String type, String icon, String color) {
            if(id==null||!id.matches("[A-Za-z][A-Za-z0-9_-]{0,99}"))throw new IllegalArgumentException("项目 ID 无效");
            String clean=name==null?"":Normalizer.normalize(name.trim(),Normalizer.Form.NFKC);
            if(clean.isEmpty()||clean.codePointCount(0,clean.length())>12||clean.matches("(?s).*[\\p{Cntrl}].*"))throw new IllegalArgumentException("项目名称须为 1–12 个字符，不能包含换行");
            if(!"expense".equals(type)&&!"income".equals(type))throw new IllegalArgumentException("项目收支类型无效");
            IconCatalog.get(icon);
            if(color==null||!color.matches("#[0-9A-Fa-f]{6}"))throw new IllegalArgumentException("项目颜色无效");
            this.id=id; this.name=clean; this.type=type; this.icon=icon; this.color=color;
        }
        public boolean custom(){return id.startsWith("custom-");}
    }
    public static final List<Category> CATEGORIES;
    static {
        List<Category> c=new ArrayList<>();
        String[][] definitions={
            {"food","餐饮","expense","food","#AAC0A4"},{"transport","交通","expense","bus","#A9C6CC"},
            {"shopping","购物","expense","bag","#EAC4A0"},{"daily","日用","expense","basket","#C9CF9E"},
            {"housing","住房","expense","home","#345E50"},{"utilities","水电","expense","bolt","#C2B8D6"},
            {"fun","娱乐","expense","game","#E4B8AF"},{"health","医疗","expense","heart","#D49F91"},
            {"study","学习","expense","book","#B0BED9"},{"travel","旅行","expense","plane","#8BBAB3"},
            {"gift","人情","expense","gift","#D2B8CD"},{"other","其他","expense","grid","#B8BDB5"},
            {"coffee","咖啡饮品","expense","coffee","#C4AC91"},{"fruit","水果","expense","fruit","#AAC0A4"},
            {"snacks","零食","expense","cookie","#EAC4A0"},{"groceries","买菜","expense","vegetable","#AAC0A4"},
            {"dining","聚餐","expense","bowl","#E4B8AF"},{"takeout","外卖","expense","takeout","#EAC4A0"},
            {"clothing","服饰","expense","shirt","#B0BED9"},{"beauty","美妆","expense","lipstick","#D2B8CD"},
            {"haircut","理发","expense","scissors","#C2B8D6"},{"mobile","通讯","expense","phone","#A9C6CC"},
            {"internet","网络","expense","wifi","#8BBAB3"},{"subscriptions","订阅","expense","subscription","#B0BED9"},
            {"digital","数码","expense","laptop","#A9C6CC"},{"car","养车","expense","car","#A9C6CC"},
            {"fuel","加油","expense","fuel","#EAC4A0"},{"parking","停车","expense","parking","#B0BED9"},
            {"taxi","打车","expense","taxi","#EAC4A0"},{"pets","宠物","expense","paw","#C4AC91"},
            {"kids","育儿","expense","baby","#E4B8AF"},{"sports","运动","expense","ball","#AAC0A4"},
            {"fitness","健身","expense","dumbbell","#8BBAB3"},{"insurance","保险","expense","shield","#A9C6CC"},
            {"mortgage","房贷","expense","bank","#B0BED9"},{"rent","房租","expense","home","#8BBAB3"},
            {"service","维修","expense","tools","#C4AC91"},{"charity","公益","expense","handHeart","#D2B8CD"},
            {"fees","手续费","expense","receipt","#B8BDB5"},{"express","快递","expense","package","#EAC4A0"},
            {"salary","工资","income","wallet","#345E50"},{"bonus","奖金","income","gift","#AAC0A4"},
            {"side","兼职","income","bag","#EAC4A0"},{"otherIncome","其他收入","income","grid","#A9C6CC"},
            {"refund","退款","income","refund","#AAC0A4"},{"interest","利息","income","percent","#8BBAB3"},
            {"investment","理财收益","income","coins","#EAC4A0"},{"resale","闲置出售","income","recycle","#AAC0A4"},
            {"redpacket","红包","income","redpacket","#E4B8AF"},{"reimbursement","报销","income","briefcase","#A9C6CC"},
            {"pension","养老金","income","pension","#C4AC91"},{"allowance","补贴","income","ticket","#B0BED9"}
        };
        for(String[] d:definitions)c.add(new Category(d[0],d[1],d[2],d[3],d[4]));
        CATEGORIES=Collections.unmodifiableList(c);
    }
    public static LocalDate today(){return LocalDate.now();}
    public static Category category(String id){for(Category c:CATEGORIES)if(c.id.equals(id))return c;throw new IllegalArgumentException("未知账目类别");}
    public static List<Category> categories(String type){List<Category> out=new ArrayList<>();for(Category c:CATEGORIES)if(c.type.equals(type))out.add(c);return out;}
    public static final class Catalog {
        public final List<Category> all,custom;
        public Catalog(List<Category> custom){
            if(custom.size()>MAX_CUSTOM_CATEGORIES)throw new IllegalArgumentException("最多可创建 100 个自定义项目");
            List<Category> combined=new ArrayList<>(CATEGORIES);Set<String> ids=new HashSet<>(),names=new HashSet<>();
            for(Category c:CATEGORIES){ids.add(c.id);names.add(c.type+":"+c.name.toLowerCase(Locale.ROOT));}
            for(Category c:custom){if(!c.custom()||!ids.add(c.id))throw new IllegalArgumentException("自定义项目 ID 重复或无效");if(!names.add(c.type+":"+c.name.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("同一收支类型下项目名称不能重复");combined.add(c);}
            this.custom=Collections.unmodifiableList(new ArrayList<>(custom));all=Collections.unmodifiableList(combined);
        }
        public Category get(String id){for(Category c:all)if(c.id.equals(id))return c;throw new IllegalArgumentException("未知账目项目");}
        public List<Category> ofType(String type){List<Category> out=new ArrayList<>();for(Category c:all)if(c.type.equals(type))out.add(c);return out;}
    }
    public static long parseAmount(String input){
        String s=input==null?"":input.trim();
        if(!s.matches("[0-9]{1,9}(\\.[0-9]{1,2})?"))throw new IllegalArgumentException("请输入最多两位小数的金额");
        long value=new BigDecimal(s).movePointRight(2).longValueExact();
        checkMinor(value); return value;
    }
    public static void checkMinor(long value){if(value<1||value>MAX_MINOR)throw new IllegalArgumentException("金额须为 0.01–999,999,999.99 元");}
    public static LocalDate parseDate(String value){
        try {if(value==null||!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw new Exception();LocalDate d=LocalDate.parse(value);if(d.isBefore(MIN_DATE)||d.isAfter(today()))throw new Exception();return d;}
        catch(Exception e){throw new IllegalArgumentException("日期须为 2000-01-01 至今天的真实日期");}
    }
    public static String money(long cents){return new DecimalFormat("#,##0.00",DecimalFormatSymbols.getInstance(Locale.US)).format(BigDecimal.valueOf(cents,2));}
    public static String plain(long cents){return BigDecimal.valueOf(cents,2).setScale(2,RoundingMode.UNNECESSARY).toPlainString();}
    public static String monthLabel(YearMonth m){return m.getYear()+" 年 "+m.getMonthValue()+" 月";}
    public static final class Entry {
        public final String id,type,categoryId,date,note,createdAt,updatedAt;
        public final long amount;
        public final Category category;
        public Entry(String id,String type,String categoryId,long amount,String date,String note,String createdAt,String updatedAt){
            this(id,type,categoryId,amount,date,note,createdAt,updatedAt,new Catalog(Collections.emptyList()));
        }
        public Entry(String id,String type,String categoryId,long amount,String date,String note,String createdAt,String updatedAt,Catalog catalog){
            if(id==null||id.trim().isEmpty()||id.length()>100)throw new IllegalArgumentException("账目 ID 无效");
            this.category=catalog.get(categoryId);if(!category.type.equals(type))throw new IllegalArgumentException("类别与收支类型不匹配");
            checkMinor(amount);parseDate(date);
            if(note==null||note.codePointCount(0,note.length())>100)throw new IllegalArgumentException("备注最多 100 个字符");
            try{Instant.parse(createdAt);Instant.parse(updatedAt);}catch(Exception e){throw new IllegalArgumentException("账目时间无效");}
            this.id=id;this.type=type;this.categoryId=categoryId;this.amount=amount;this.date=date;this.note=note.trim();this.createdAt=createdAt;this.updatedAt=updatedAt;
        }
        public static Entry create(String type,String category,long amount,String date,String note){String now=Instant.now().toString();return new Entry(UUID.randomUUID().toString(),type,category,amount,date,note,now,now);}
    }
    public static final class Budget {
        public final Long total;
        public final Map<String,Long> limits;
        public Budget(Long total,Map<String,Long> values){
            this(total,values,new Catalog(Collections.emptyList()));
        }
        public Budget(Long total,Map<String,Long> values,Catalog catalog){
            if(total!=null)checkMinor(total);
            long assigned=0;Map<String,Long> clean=new LinkedHashMap<>();
            for(Map.Entry<String,Long> e:values.entrySet()){
                if(!catalog.get(e.getKey()).type.equals("expense"))throw new IllegalArgumentException("收入类别不能设置预算");
                if(e.getValue()==null)throw new IllegalArgumentException("分类预算无效");checkMinor(e.getValue());assigned+=e.getValue();clean.put(e.getKey(),e.getValue());
            }
            if(assigned>(total==null?0:total))throw new IllegalArgumentException("分类预算合计不能超过月总预算");
            this.total=total;this.limits=Collections.unmodifiableMap(clean);
        }
        public static Budget empty(){return new Budget(null,Collections.emptyMap());}
    }
    public static final class Data {
        public final List<Entry> entries;
        public final Map<String,Budget> budgets;
        public final boolean hide;
        public final Catalog catalog;
        public Data(List<Entry> entries,Map<String,Budget> budgets,boolean hide){this(entries,budgets,hide,Collections.emptyList());}
        public Data(List<Entry> entries,Map<String,Budget> budgets,boolean hide,List<Category> custom){
            catalog=new Catalog(custom);List<Entry> clean=new ArrayList<>();Set<String> ids=new HashSet<>();
            if(entries.size()>MAX_ENTRIES)throw new IllegalArgumentException("账目超过 10,000 笔上限");
            for(Entry e:entries){Category c=catalog.get(e.categoryId);if(!ids.add(e.id))throw new IllegalArgumentException("账目 ID 重复");clean.add(e.category==c?e:new Entry(e.id,e.type,e.categoryId,e.amount,e.date,e.note,e.createdAt,e.updatedAt,catalog));}
            for(Map.Entry<String,Budget> b:budgets.entrySet()){if(!b.getKey().matches("20[0-9]{2}-(0[1-9]|1[0-2])"))throw new IllegalArgumentException("预算月份无效");new Budget(b.getValue().total,b.getValue().limits,catalog);}
            this.entries=Collections.unmodifiableList(clean);this.budgets=Collections.unmodifiableMap(new LinkedHashMap<>(budgets));this.hide=hide;
        }
        public Category category(String id){return catalog.get(id);}
        public List<Category> categories(String type){return catalog.ofType(type);}
        public static Data empty(){return new Data(Collections.emptyList(),Collections.emptyMap(),false);}
        public Budget budget(YearMonth m){Budget b=budgets.get(m.toString());return b==null?Budget.empty():b;}
    }
    public static List<Entry> range(Data data,LocalDate start,LocalDate end){
        List<Entry> out=new ArrayList<>();String a=start.toString(),b=end.toString();for(Entry e:data.entries)if(e.date.compareTo(a)>=0&&e.date.compareTo(b)<=0)out.add(e);
        out.sort(Comparator.comparing((Entry e)->e.date).thenComparing(e->e.createdAt).reversed());return out;
    }
    public static List<Entry> month(Data data,YearMonth m){return range(data,m.atDay(1),m.atEndOfMonth());}
    public static long total(List<Entry> entries,String type){long result=0;for(Entry e:entries)if(e.type.equals(type))result=Math.addExact(result,e.amount);return result;}
    public static List<Entry> filter(List<Entry> entries,String type,String category,String keyword,LocalDate day){
        List<Entry> out=new ArrayList<>();String search=keyword.trim().toLowerCase(Locale.ROOT);
        for(Entry e:entries)if((type.equals("all")||e.type.equals(type))&&(category.isEmpty()||category.equals(e.categoryId))&&(day==null||e.date.equals(day.toString()))&&(e.note+" "+e.category.name).toLowerCase(Locale.ROOT).contains(search))out.add(e);return out;
    }
    public static List<String> noteSuggestions(Data data,String type,String categoryId){
        List<Entry> history=filter(data.entries,type,categoryId,"",null);
        history.sort(Comparator.comparing((Entry e)->Instant.parse(e.updatedAt)).thenComparing(e->Instant.parse(e.createdAt)).reversed());
        Set<String> notes=new LinkedHashSet<>();for(Entry e:history)if(!e.note.isEmpty()){notes.add(e.note);if(notes.size()==30)break;}
        return new ArrayList<>(notes);
    }
    public static final class Group {
        public final Category category;public long total;public int count;
        Group(Category c){category=c;}
    }
    public static List<Group> groups(List<Entry> entries,String type){
        Map<String,Group> map=new LinkedHashMap<>();
        for(Entry e:entries)if(e.type.equals(type)){Group g=map.get(e.categoryId);if(g==null){g=new Group(e.category);map.put(e.categoryId,g);}g.total+=e.amount;g.count++;}
        List<Group> out=new ArrayList<>();for(Group g:map.values())if(g.count>0)out.add(g);out.sort((a,b)->Long.compare(b.total,a.total));return out;
    }
    public static final class Bucket {
        public final String key,label;public final boolean future;public long total;
        Bucket(String key,String label,boolean future){this.key=key;this.label=label;this.future=future;}
    }
    public static final class Report {
        public final String period,type;public final LocalDate start,end;
        public final List<Entry> entries;public final List<Bucket> buckets;public final List<Group> groups;
        public final long total,average;public final int elapsed;
        Report(Data data,String period,LocalDate anchor,String type){
            this.period=period;this.type=type;
            if(period.equals("year")){start=LocalDate.of(anchor.getYear(),1,1);end=LocalDate.of(anchor.getYear(),12,31);}
            else if(period.equals("month")){start=anchor.withDayOfMonth(1);end=YearMonth.from(anchor).atEndOfMonth();}
            else if(period.equals("week")){start=anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));end=start.plusDays(6);}
            else throw new IllegalArgumentException("统计周期无效");
            entries=filter(range(data,start,end),type,"","",null);total=total(entries,type);groups=groups(entries,type);buckets=new ArrayList<>();int count=0;
            if(period.equals("year"))for(int i=1;i<=12;i++){YearMonth m=YearMonth.of(anchor.getYear(),i);Bucket b=new Bucket(m.toString(),i+"月",m.isAfter(YearMonth.from(today())));for(Entry e:entries)if(e.date.startsWith(m.toString()))b.total+=e.amount;buckets.add(b);if(!b.future)count++;}
            else for(LocalDate d=start;!d.isAfter(end);d=d.plusDays(1)){Bucket b=new Bucket(d.toString(),period.equals("week")?new String[]{"一","二","三","四","五","六","日"}[d.getDayOfWeek().getValue()-1]:String.valueOf(d.getDayOfMonth()),d.isAfter(today()));for(Entry e:entries)if(e.date.equals(b.key))b.total+=e.amount;buckets.add(b);if(!b.future&&!d.isBefore(MIN_DATE))count++;}
            elapsed=count;average=count==0?0:BigDecimal.valueOf(total).divide(BigDecimal.valueOf(count),0,RoundingMode.HALF_UP).longValueExact();
        }
        public boolean current(){return !today().isBefore(start)&&!today().isAfter(end);}
        public LocalDate adjacent(int direction){LocalDate d=direction<0?start.minusDays(1):end.plusDays(1);return d.isBefore(MIN_DATE)||d.isAfter(today())?null:d;}
        public String title(){if(period.equals("year"))return start.getYear()+" 年";if(period.equals("month"))return monthLabel(YearMonth.from(start));return start+" — "+end.toString().substring(5);}
    }
    public static Report report(Data data,String period,LocalDate anchor,String type){return new Report(data,period,anchor,type);}
    public static String csv(List<Entry> entries){
        StringBuilder b=new StringBuilder("\uFEFF");csvLine(b,new String[]{"账目ID","日期","类型","类别","金额","币种","备注"});
        for(Entry e:entries)csvLine(b,new String[]{e.id,e.date,e.type.equals("expense")?"支出":"收入",e.category.name,plain(e.amount),"CNY",e.note});return b.toString();
    }
    private static void csvLine(StringBuilder b,String[] cells){for(int i=0;i<cells.length;i++){if(i>0)b.append(',');String s=cells[i];if(s.matches("(?s)^\\s*[=+\\-@].*"))s="'"+s;b.append('"').append(s.replace("\"","\"\"")).append('"');}b.append("\r\n");}
    public static Data demo(){
        List<Entry> entries=new ArrayList<>();Map<String,Budget> budgets=new LinkedHashMap<>();LocalDate now=today();YearMonth current=YearMonth.from(now);
        for(int year=now.getYear()-2;year<=now.getYear();year++)for(int m=1;m<=12;m++){
            YearMonth ym=YearMonth.of(year,m);if(!ym.isBefore(current))continue;
            String[] cats={"housing","food","transport","shopping","fun","salary"};long[] amounts={150000,(650+(year*7+m*31)%400)*100L,(90+m*19%160)*100L,(100+m*67%500)*100L,(60+m*37%200)*100L,(10000+(year-(now.getYear()-2))*1000)*100L};int[] days={2,5,9,16,22,1};
            for(int i=0;i<cats.length;i++)entries.add(demoEntry("demo-"+ym+"-"+cats[i],cats[i],amounts[i],ym.atDay(days[i]),"演示 · "+category(cats[i]).name));
        }
        String[] cats={"food","food","transport","food","food","transport","food","daily","fun","health","housing","shopping","utilities","salary"};
        long[] values={3200,2200,600,1800,6800,3600,2500,15600,7800,8000,150000,18900,10000,1200000};int[] days={4,4,4,4,3,3,3,2,2,2,1,1,1,1};
        String[] notes={"工作日午餐","楼下的咖啡","地铁通勤","早餐也要好好吃","和朋友吃晚饭","下雨打车回家","水果与小零食","超市采购","一场喜欢的电影","常备药品","本月房租","换季针织衫","电费充值","本月工资"};
        for(int i=0;i<cats.length;i++)entries.add(demoEntry("demo-current-"+i,cats[i],values[i],current.atDay(Math.min(days[i],now.getDayOfMonth())),"演示 · "+notes[i]));
        Map<String,Long> limits=new LinkedHashMap<>();limits.put("food",120000L);limits.put("transport",30000L);limits.put("shopping",50000L);limits.put("housing",180000L);budgets.put(current.toString(),new Budget(500000L,limits));return new Data(entries,budgets,false);
    }
    private static Entry demoEntry(String id,String category,long amount,LocalDate date,String note){String timestamp=date+"T12:00:00Z";return new Entry(id,category(category).type,category,amount,date.toString(),note,timestamp,timestamp);}
}
