package com.simaoqingnian.myaccount;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Same schemaVersion 1 as the browser prototype. Unknown fields are not stored. */
public final class BackupCodec {
    public static String encode(Ledger.Data data) throws Exception {
        JSONObject root=new JSONObject(),body=new JSONObject(),budgets=new JSONObject();JSONArray rows=new JSONArray();
        for(Ledger.Entry e:data.entries){JSONObject row=new JSONObject();row.put("id",e.id);row.put("type",e.type);row.put("categoryId",e.categoryId);row.put("amountMinor",e.amount);row.put("occurredOn",e.date);row.put("note",e.note);row.put("createdAt",e.createdAt);row.put("updatedAt",e.updatedAt);rows.put(row);}
        for(Map.Entry<String,Ledger.Budget> b:data.budgets.entrySet())budgets.put(b.getKey(),budgetJson(b.getValue()));
        body.put("transactions",rows);body.put("budgets",budgets);body.put("preferences",new JSONObject().put("hideAmounts",data.hide));root.put("schemaVersion",1);root.put("exportedAt",Instant.now().toString());root.put("data",body);return root.toString(2);
    }
    static JSONObject budgetJson(Ledger.Budget b) throws Exception {JSONObject object=new JSONObject(),limits=new JSONObject();object.put("totalMinor",b.total==null?JSONObject.NULL:b.total);for(Map.Entry<String,Long> e:b.limits.entrySet())limits.put(e.getKey(),e.getValue());object.put("categoryLimits",limits);return object;}
    static Ledger.Budget budget(JSONObject object) throws Exception {
        Long total=object.isNull("totalMinor")?null:integer(object.get("totalMinor"));JSONObject source=object.getJSONObject("categoryLimits");Map<String,Long> limits=new LinkedHashMap<>();Iterator<String> keys=source.keys();while(keys.hasNext()){String key=keys.next();limits.put(key,integer(source.get(key)));}return new Ledger.Budget(total,limits);
    }
    public static Ledger.Data decode(String text) throws Exception {
        JSONObject root;
        try{root=new JSONObject(text);}catch(Exception e){throw new IllegalArgumentException("文件不是有效的 JSON 备份");}
        if(integer(root.get("schemaVersion"))!=1)throw new IllegalArgumentException("不支持此备份版本");timestamp(root.getString("exportedAt"));JSONObject body=root.getJSONObject("data");JSONArray rows=body.getJSONArray("transactions");
        if(rows.length()>Ledger.MAX_ENTRIES)throw new IllegalArgumentException("备份超过 10,000 笔账目上限");
        List<Ledger.Entry> entries=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);String id=strictString(row,"id");if(!ids.add(id))throw new IllegalArgumentException("备份含重复账目 ID");
            entries.add(new Ledger.Entry(id,strictString(row,"type"),strictString(row,"categoryId"),integer(row.get("amountMinor")),strictString(row,"occurredOn"),strictString(row,"note"),timestamp(strictString(row,"createdAt")),timestamp(strictString(row,"updatedAt"))));
        }
        Map<String,Ledger.Budget> budgets=new LinkedHashMap<>();JSONObject source=body.getJSONObject("budgets");Iterator<String> keys=source.keys();while(keys.hasNext()){String key=keys.next();if(!key.matches("20[0-9]{2}-(0[1-9]|1[0-2])"))throw new IllegalArgumentException("预算月份无效");YearMonth.parse(key);budgets.put(key,budget(source.getJSONObject(key)));}
        Object hide=body.getJSONObject("preferences").get("hideAmounts");if(!(hide instanceof Boolean))throw new IllegalArgumentException("金额隐藏设置无效");return new Ledger.Data(entries,budgets,(Boolean)hide);
    }
    private static long integer(Object raw){if(!(raw instanceof Integer)&&!(raw instanceof Long))throw new IllegalArgumentException("备份金额和版本必须使用整数");return ((Number)raw).longValue();}
    private static String strictString(JSONObject object,String key) throws Exception {Object raw=object.get(key);if(!(raw instanceof String))throw new IllegalArgumentException("备份字段类型无效："+key);return (String)raw;}
    private static String timestamp(String raw){try{return OffsetDateTime.parse(raw).toInstant().toString();}catch(Exception e){throw new IllegalArgumentException("备份时间格式无效");}}
}
