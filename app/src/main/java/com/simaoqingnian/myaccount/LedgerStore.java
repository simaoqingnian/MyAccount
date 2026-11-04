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
    public LedgerStore(Context context){super(context.getApplicationContext(),"heji.db",null,1);this.context=context.getApplicationContext();setWriteAheadLoggingEnabled(true);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE entries (id TEXT PRIMARY KEY NOT NULL,type TEXT NOT NULL,category_id TEXT NOT NULL,amount INTEGER NOT NULL CHECK(amount>0 AND amount<=99999999999),date TEXT NOT NULL,note TEXT NOT NULL,created_at TEXT NOT NULL,updated_at TEXT NOT NULL)");
        db.execSQL("CREATE INDEX entries_by_date ON entries(date,created_at)");
        db.execSQL("CREATE TABLE budgets (month TEXT PRIMARY KEY NOT NULL,json TEXT NOT NULL)");
        db.execSQL("CREATE TABLE preferences (key TEXT PRIMARY KEY NOT NULL,value TEXT NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new IllegalStateException("不支持此数据库升级路径，请保留数据并联系开发者");}
    public Ledger.Data read() throws Exception {
        SQLiteDatabase db=getReadableDatabase();List<Ledger.Entry> entries=new ArrayList<>();Map<String,Ledger.Budget> budgets=new LinkedHashMap<>();boolean hide=false;
        try(Cursor cursor=db.rawQuery("SELECT id,type,category_id,amount,date,note,created_at,updated_at FROM entries ORDER BY date DESC,created_at DESC",null)){
            while(cursor.moveToNext())entries.add(new Ledger.Entry(cursor.getString(0),cursor.getString(1),cursor.getString(2),cursor.getLong(3),cursor.getString(4),cursor.getString(5),cursor.getString(6),cursor.getString(7)));
        }
        try(Cursor cursor=db.rawQuery("SELECT month,json FROM budgets",null)){while(cursor.moveToNext())budgets.put(cursor.getString(0),BackupCodec.budget(new JSONObject(cursor.getString(1))));}
        try(Cursor cursor=db.rawQuery("SELECT value FROM preferences WHERE key='hide'",null)){if(cursor.moveToFirst())hide="1".equals(cursor.getString(0));}
        return new Ledger.Data(entries,budgets,hide);
    }
    private ContentValues entryValues(Ledger.Entry e){ContentValues v=new ContentValues();v.put("id",e.id);v.put("type",e.type);v.put("category_id",e.categoryId);v.put("amount",e.amount);v.put("date",e.date);v.put("note",e.note);v.put("created_at",e.createdAt);v.put("updated_at",e.updatedAt);return v;}
    public void save(Ledger.Entry e) throws Exception {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {boolean exists;try(Cursor c=db.rawQuery("SELECT 1 FROM entries WHERE id=?",new String[]{e.id})){exists=c.moveToFirst();}if(!exists){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM entries",null)){c.moveToFirst();if(c.getInt(0)>=Ledger.MAX_ENTRIES)throw new IllegalArgumentException("已达到 10,000 笔账目上限，请先导出备份");}db.insertOrThrow("entries",null,entryValues(e));}else if(db.update("entries",entryValues(e),"id=?",new String[]{e.id})!=1)throw new IllegalStateException("账目更新失败");db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    public void delete(String id){if(getWritableDatabase().delete("entries","id=?",new String[]{id})!=1)throw new IllegalArgumentException("这笔账目已经不存在");}
    public void saveBudget(String month,Ledger.Budget budget) throws Exception {ContentValues v=new ContentValues();v.put("month",month);v.put("json",BackupCodec.budgetJson(budget).toString());if(getWritableDatabase().insertWithOnConflict("budgets",null,v,SQLiteDatabase.CONFLICT_REPLACE)<0)throw new IllegalStateException("预算保存失败");}
    public void setHide(boolean hide){ContentValues v=new ContentValues();v.put("key","hide");v.put("value",hide?"1":"0");if(getWritableDatabase().insertWithOnConflict("preferences",null,v,SQLiteDatabase.CONFLICT_REPLACE)<0)throw new IllegalStateException("设置保存失败");}
    public File previousBackup(){return new File(context.getFilesDir(),"before-restore.json");}
    private void preserveCurrent() throws Exception {
        AtomicFile target=new AtomicFile(previousBackup());FileOutputStream stream=null;
        try{stream=target.startWrite();stream.write(BackupCodec.encode(read()).getBytes(StandardCharsets.UTF_8));target.finishWrite(stream);}catch(Exception e){if(stream!=null)target.failWrite(stream);throw e;}
    }
    public void replace(Ledger.Data data) throws Exception {
        preserveCurrent();SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            db.delete("entries",null,null);db.delete("budgets",null,null);db.delete("preferences",null,null);
            for(Ledger.Entry entry:data.entries)db.insertOrThrow("entries",null,entryValues(entry));
            for(Map.Entry<String,Ledger.Budget> b:data.budgets.entrySet()){ContentValues v=new ContentValues();v.put("month",b.getKey());v.put("json",BackupCodec.budgetJson(b.getValue()).toString());db.insertOrThrow("budgets",null,v);}
            ContentValues p=new ContentValues();p.put("key","hide");p.put("value",data.hide?"1":"0");db.insertOrThrow("preferences",null,p);db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    public void addDemo() throws Exception {
        Ledger.Data demo=Ledger.demo(),current=read();Map<String,Ledger.Entry> merged=new LinkedHashMap<>();for(Ledger.Entry e:demo.entries)merged.put(e.id,e);for(Ledger.Entry e:current.entries)merged.put(e.id,e);
        if(merged.size()>Ledger.MAX_ENTRIES)throw new IllegalArgumentException("加入演示后会超过账目数量上限");Map<String,Ledger.Budget> budgets=new LinkedHashMap<>(demo.budgets);budgets.putAll(current.budgets);replace(new Ledger.Data(new ArrayList<>(merged.values()),budgets,current.hide));
    }
}
