package com.simaoqingnian.myaccount;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** SQLite writes and full replacement are transactional; callers run on a worker. */
public final class LedgerStore extends SQLiteOpenHelper {
    /** One ordered queue also keeps reads after writes across Activity recreation. */
    public static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private final Context context;
    public LedgerStore(Context context){super(context.getApplicationContext(),"heji.db",null,2);this.context=context.getApplicationContext();setWriteAheadLoggingEnabled(true);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE entries (id TEXT PRIMARY KEY NOT NULL,type TEXT NOT NULL,category_id TEXT NOT NULL,amount INTEGER NOT NULL CHECK(amount>0 AND amount<=99999999999),date TEXT NOT NULL,note TEXT NOT NULL,created_at TEXT NOT NULL,updated_at TEXT NOT NULL)");
        db.execSQL("CREATE INDEX entries_by_date ON entries(date,created_at)");
        db.execSQL("CREATE TABLE budgets (month TEXT PRIMARY KEY NOT NULL,json TEXT NOT NULL)");
        db.execSQL("CREATE TABLE preferences (key TEXT PRIMARY KEY NOT NULL,value TEXT NOT NULL)");
        createCategories(db);
    }
    private void createCategories(SQLiteDatabase db){db.execSQL("CREATE TABLE custom_categories (id TEXT PRIMARY KEY NOT NULL,name TEXT NOT NULL,type TEXT NOT NULL,icon TEXT NOT NULL,color TEXT NOT NULL,UNIQUE(type,name))");}
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){if(oldVersion==1&&newVersion==2){createCategories(db);return;}throw new IllegalStateException("不支持此数据库升级路径，请保留数据并联系开发者");}
    public Ledger.Data read() throws Exception {
        SQLiteDatabase db=getReadableDatabase();List<Ledger.Entry> entries=new ArrayList<>();Map<String,Ledger.Budget> budgets=new LinkedHashMap<>();boolean hide=false;
        List<Ledger.Category> custom=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT id,name,type,icon,color FROM custom_categories ORDER BY rowid",null)){while(c.moveToNext())custom.add(new Ledger.Category(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)));}Ledger.Catalog catalog=new Ledger.Catalog(custom);
        try(Cursor cursor=db.rawQuery("SELECT id,type,category_id,amount,date,note,created_at,updated_at FROM entries ORDER BY date DESC,created_at DESC",null)){
            while(cursor.moveToNext())entries.add(new Ledger.Entry(cursor.getString(0),cursor.getString(1),cursor.getString(2),cursor.getLong(3),cursor.getString(4),cursor.getString(5),cursor.getString(6),cursor.getString(7),catalog));
        }
        try(Cursor cursor=db.rawQuery("SELECT month,json FROM budgets",null)){while(cursor.moveToNext())budgets.put(cursor.getString(0),BackupCodec.budget(new JSONObject(cursor.getString(1)),catalog));}
        try(Cursor cursor=db.rawQuery("SELECT value FROM preferences WHERE key='hide'",null)){if(cursor.moveToFirst())hide="1".equals(cursor.getString(0));}
        return new Ledger.Data(entries,budgets,hide,custom);
    }
    private ContentValues entryValues(Ledger.Entry e){ContentValues v=new ContentValues();v.put("id",e.id);v.put("type",e.type);v.put("category_id",e.categoryId);v.put("amount",e.amount);v.put("date",e.date);v.put("note",e.note);v.put("created_at",e.createdAt);v.put("updated_at",e.updatedAt);return v;}
    public void save(Ledger.Entry e) throws Exception {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {if(!read().category(e.categoryId).type.equals(e.type))throw new IllegalArgumentException("项目与收支类型不匹配");boolean exists;try(Cursor c=db.rawQuery("SELECT 1 FROM entries WHERE id=?",new String[]{e.id})){exists=c.moveToFirst();}if(!exists){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM entries",null)){c.moveToFirst();if(c.getInt(0)>=Ledger.MAX_ENTRIES)throw new IllegalArgumentException("已达到 10,000 笔账目上限，请先导出备份");}db.insertOrThrow("entries",null,entryValues(e));}else if(db.update("entries",entryValues(e),"id=?",new String[]{e.id})!=1)throw new IllegalStateException("账目更新失败");db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    public void delete(String id){if(getWritableDatabase().delete("entries","id=?",new String[]{id})!=1)throw new IllegalArgumentException("这笔账目已经不存在");}
    public void saveBudget(String month,Ledger.Budget budget) throws Exception {new Ledger.Budget(budget.total,budget.limits,read().catalog);ContentValues v=new ContentValues();v.put("month",month);v.put("json",BackupCodec.budgetJson(budget).toString());if(getWritableDatabase().insertWithOnConflict("budgets",null,v,SQLiteDatabase.CONFLICT_REPLACE)<0)throw new IllegalStateException("预算保存失败");}
    private ContentValues categoryValues(Ledger.Category c){ContentValues v=new ContentValues();v.put("id",c.id);v.put("name",c.name);v.put("type",c.type);v.put("icon",c.icon);v.put("color",c.color);return v;}
    public void saveCategory(Ledger.Category category) throws Exception {
        if(!category.custom())throw new IllegalArgumentException("预置项目不能修改");SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{List<Ledger.Category> custom=new ArrayList<>(read().catalog.custom);boolean exists=false;for(int i=0;i<custom.size();i++)if(custom.get(i).id.equals(category.id)){if(!custom.get(i).type.equals(category.type))throw new IllegalArgumentException("不能更改项目的收支类型");custom.remove(i);exists=true;break;}custom.add(category);new Ledger.Catalog(custom);if(exists){if(db.update("custom_categories",categoryValues(category),"id=?",new String[]{category.id})!=1)throw new IllegalStateException("项目更新失败");}else db.insertOrThrow("custom_categories",null,categoryValues(category));db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    public void deleteCategory(String id) throws Exception {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{Ledger.Data current=read();if(!current.category(id).custom())throw new IllegalArgumentException("预置项目不能删除");for(Ledger.Entry e:current.entries)if(e.categoryId.equals(id))throw new IllegalArgumentException("该项目已有账目，可修改名称或图标，不能删除");for(Ledger.Budget b:current.budgets.values())if(b.limits.containsKey(id))throw new IllegalArgumentException("该项目已有分类预算，请先移除相应预算");if(db.delete("custom_categories","id=?",new String[]{id})!=1)throw new IllegalArgumentException("项目已不存在");db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    public void setHide(boolean hide){ContentValues v=new ContentValues();v.put("key","hide");v.put("value",hide?"1":"0");if(getWritableDatabase().insertWithOnConflict("preferences",null,v,SQLiteDatabase.CONFLICT_REPLACE)<0)throw new IllegalStateException("设置保存失败");}
    public File previousBackup(){return new File(context.getFilesDir(),"before-restore.json");}
    private void preserveCurrent() throws Exception {
        AtomicFile target=new AtomicFile(previousBackup());FileOutputStream stream=null;
        try{stream=target.startWrite();stream.write(BackupCodec.encode(read()).getBytes(StandardCharsets.UTF_8));target.finishWrite(stream);}catch(Exception e){if(stream!=null)target.failWrite(stream);throw e;}
    }
    public void replace(Ledger.Data data) throws Exception {
        preserveCurrent();SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            db.delete("entries",null,null);db.delete("budgets",null,null);db.delete("preferences",null,null);db.delete("custom_categories",null,null);
            for(Ledger.Category c:data.catalog.custom)db.insertOrThrow("custom_categories",null,categoryValues(c));
            for(Ledger.Entry entry:data.entries)db.insertOrThrow("entries",null,entryValues(entry));
            for(Map.Entry<String,Ledger.Budget> b:data.budgets.entrySet()){ContentValues v=new ContentValues();v.put("month",b.getKey());v.put("json",BackupCodec.budgetJson(b.getValue()).toString());db.insertOrThrow("budgets",null,v);}
            ContentValues p=new ContentValues();p.put("key","hide");p.put("value",data.hide?"1":"0");db.insertOrThrow("preferences",null,p);db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    public void addDemo() throws Exception {
        Ledger.Data demo=Ledger.demo(),current=read();Map<String,Ledger.Entry> merged=new LinkedHashMap<>();for(Ledger.Entry e:demo.entries)merged.put(e.id,e);for(Ledger.Entry e:current.entries)merged.put(e.id,e);
        if(merged.size()>Ledger.MAX_ENTRIES)throw new IllegalArgumentException("加入演示后会超过账目数量上限");Map<String,Ledger.Budget> budgets=new LinkedHashMap<>(demo.budgets);budgets.putAll(current.budgets);replace(new Ledger.Data(new ArrayList<>(merged.values()),budgets,current.hide,current.catalog.custom));
    }
}
