package com.simaoqingnian.myaccount;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import android.view.inputmethod.InputMethodManager;

public final class EntryActivity extends Activity {
    private final ExecutorService worker=LedgerStore.IO;private LedgerStore store;
    private String id,type="expense",category="food",amount="",note="",date=EntrySession.CURRENT.date().toString(),createdAt,original;
    private Ledger.Data ledger=Ledger.Data.empty();
    private List<String> recentCategories=new ArrayList<>();
    private EditText amountInput;private NoteInput noteInput;private GridLayout keyboard;private TextView error;private boolean saving,ready;private LinearLayout root;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);store=new LedgerStore(this);id=getIntent().getStringExtra("entryId");
        recentCategories=Arrays.asList(getSharedPreferences("entry-ui",MODE_PRIVATE).getString("recent-projects","").split(","));
        if(saved!=null){id=saved.getString("id");type=saved.getString("type","expense");category=saved.getString("category","food");amount=saved.getString("amount","");note=saved.getString("note","");date=saved.getString("date",date);createdAt=saved.getString("createdAt");original=saved.getString("original");ready=saved.getBoolean("ready");}
        root=Ui.column(this);root.setBackgroundColor(Ui.PAPER);setContentView(root);Ui.insets(this,root);root.addView(Ui.text(this,"正在读取账目与历史备注…",15,Ui.MUTED));
        worker.execute(()->{try{Ledger.Data latest=store.read();Ledger.Entry found=null;if(id!=null&&!ready){for(Ledger.Entry e:latest.entries)if(e.id.equals(id))found=e;if(found==null)throw new IllegalArgumentException("这笔账目不存在");}Ledger.Entry entry=found;runOnUiThread(()->{if(isDestroyed())return;ledger=latest;if(entry!=null){type=entry.type;category=entry.categoryId;amount=Ledger.plain(entry.amount);note=entry.note;date=entry.date;createdAt=entry.createdAt;}try{ledger.category(category);}catch(IllegalArgumentException e){category=type.equals("expense")?"food":"salary";}if(original==null)original=fingerprint();ready=true;render();});}catch(Exception e){runOnUiThread(()->{if(isDestroyed())return;Toast.makeText(this,"读取失败："+e.getMessage(),Toast.LENGTH_LONG).show();finish();});}});
    }
    private String fingerprint(){return type+"\n"+category+"\n"+amount+"\n"+date+"\n"+note;}
    private void capture(){if(amountInput!=null)amount=amountInput.getText().toString();if(noteInput!=null)note=noteInput.getText().toString();}
    private void render(){
        keyboard=null;root=Ui.column(this);root.setBackgroundColor(Ui.PAPER);setContentView(root);Ui.insets(this,root);
        LinearLayout header=Ui.row(this);Ui.pad(header,17,7,17,5);TextView close=Ui.smallAction(this,"取消",this::requestClose);header.addView(close);TextView title=Ui.bold(this,id==null?"记一笔":"编辑账目",20,Ui.INK);title.setGravity(Gravity.CENTER);Ui.weight(header,title);header.addView(Ui.smallAction(this,"保存",this::save));root.addView(header);
        LinearLayout body=Ui.column(this);Ui.pad(body,18,0,18,4);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout types=Ui.row(this);types.setBackground(Ui.background(Ui.SAGE,12,this));for(String value:new String[]{"expense","income"}){TextView t=Ui.action(this,value.equals("expense")?"支出":"收入",type.equals(value),()->{capture();type=value;category=value.equals("expense")?"food":"salary";render();});t.setMinHeight(Ui.dp(this,42));if(!type.equals(value))t.setBackgroundColor(android.graphics.Color.TRANSPARENT);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(this,42),1);p.setMargins(Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3));types.addView(t,p);}body.addView(types);Ui.space(body,4);
        LinearLayout amountRow=Ui.row(this);amountRow.addView(Ui.text(this,"¥",28,Ui.GREEN));amountInput=new EditText(this);amountInput.setId(R.id.entry_amount);amountInput.setText(amount);amountInput.setTextColor(Ui.GREEN);amountInput.setTextSize(38);amountInput.setSingleLine(true);amountInput.setIncludeFontPadding(false);Ui.pad(amountInput,0,0,0,0);amountInput.setHint("0.00");amountInput.setHintTextColor(0xFF9CA995);amountInput.setGravity(Gravity.END);amountInput.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amountInput.setShowSoftInputOnFocus(false);amountInput.setBackgroundColor(android.graphics.Color.TRANSPARENT);amountInput.setSelectAllOnFocus(true);amountInput.setContentDescription("记账金额");amountInput.setOnFocusChangeListener((v,focused)->{if(focused){if(keyboard!=null)keyboard.setVisibility(View.VISIBLE);InputMethodManager input=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(input!=null)input.hideSoftInputFromWindow(v.getWindowToken(),0);}});Ui.weight(amountRow,amountInput);body.addView(amountRow,new LinearLayout.LayoutParams(-1,Ui.dp(this,56)));Ui.divider(body);Ui.space(body,4);
        LinearLayout categoryHeader=Ui.row(this);TextView selectedName=Ui.bold(this,"项目 · "+ledger.category(category).name,13,Ui.GREEN);selectedName.setSingleLine(true);selectedName.setEllipsize(android.text.TextUtils.TruncateAt.END);Ui.weight(categoryHeader,selectedName);TextView custom=Ui.smallAction(this,"＋ 自定义",()->editCategory(null));custom.setMinHeight(Ui.dp(this,32));Ui.pad(custom,6,2,6,2);categoryHeader.addView(custom);body.addView(categoryHeader);
        GridLayout categories=new GridLayout(this);categories.setColumnCount(5);int index=0;
        for(Ledger.Category c:RecentCategories.order(ledger.catalog,type,recentCategories)){
            LinearLayout choice=Ui.column(this);choice.setGravity(Gravity.CENTER);Ui.Icon symbol=new Ui.Icon(this,c.icon,category.equals(c.id)?Ui.WHITE:Ui.GREEN);symbol.setBackground(Ui.background(category.equals(c.id)?Ui.GREEN:Ui.SAGE,13,this));Ui.pad(symbol,9,9,9,9);choice.addView(symbol,new LinearLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,40)));TextView name=Ui.text(this,c.name,11,category.equals(c.id)?Ui.GREEN:Ui.MUTED);name.setGravity(Gravity.CENTER);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=Ui.dp(this,3);choice.addView(name,np);choice.setContentDescription(c.name+(category.equals(c.id)?"，已选择":"")+(c.custom()?"，长按编辑":""));choice.setOnClickListener(v->selectCategory(c.id));if(c.custom())choice.setOnLongClickListener(v->{editCategory(c);return true;});choice.setFocusable(true);GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(index/5),GridLayout.spec(index%5,1,1f));p.width=0;p.height=Ui.dp(this,60);categories.addView(choice,p);index++;
        }
        ScrollView categoryScroll=new ScrollView(this);categoryScroll.setFillViewport(false);categoryScroll.addView(categories);body.addView(categoryScroll,new LinearLayout.LayoutParams(-1,0,1));
        // Keep the date above both the amount keypad and the system IME.
        LinearLayout details=Ui.column(this);Ui.pad(details,18,3,18,5);root.addView(details,new LinearLayout.LayoutParams(-1,-2));
        noteInput=new NoteInput(this);noteInput.setId(R.id.entry_note);noteInput.setSingleLine(true);noteInput.setTextSize(14);noteInput.setTextColor(Ui.INK);noteInput.setHintTextColor(Ui.MUTED);noteInput.setHint("点击选择历史备注，也可直接输入");noteInput.setBackground(Ui.border(Ui.WHITE,12,this));Ui.pad(noteInput,11,6,11,6);noteInput.setMinHeight(Ui.dp(this,38));noteInput.setText(note,false);noteInput.setContentDescription("账目备注，点击可选择当前项目的历史备注");noteInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});noteInput.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,Ledger.noteSuggestions(ledger,type,category)));noteInput.setDropDownHeight(Ui.dp(this,180));noteInput.setOnClickListener(v->showNotes());noteInput.setOnFocusChangeListener((v,focused)->{if(keyboard!=null)keyboard.setVisibility(focused?View.GONE:View.VISIBLE);if(focused)noteInput.post(this::showNotes);});noteInput.setOnItemClickListener((parent,v,position,row)->{note=noteInput.getText().toString();noteInput.setSelection(note.length());noteInput.dismissDropDown();});details.addView(noteInput);Ui.space(details,4);
        TextView dateButton=Ui.action(this,"账目日期     "+date+"   ›",false,()->{capture();LocalDate value=LocalDate.parse(date);DatePickerDialog picker=new DatePickerDialog(this,(view,y,m,d)->{capture();LocalDate selected=LocalDate.of(y,m+1,d);EntrySession.CURRENT.select(selected);date=selected.toString();render();},value.getYear(),value.getMonthValue()-1,value.getDayOfMonth());picker.getDatePicker().setMinDate(Ledger.MIN_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();});dateButton.setBackground(Ui.border(Ui.WHITE,12,this));dateButton.setTextSize(12);dateButton.setMinHeight(Ui.dp(this,44));Ui.pad(dateButton,10,4,10,4);dateButton.setContentDescription("账目日期 "+date+"，手动选定后持续沿用");details.addView(dateButton);error=Ui.text(this,"",12,Ui.DANGER);error.setVisibility(View.GONE);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);Ui.pad(error,0,4,0,0);details.addView(error);
        keyboard=new GridLayout(this);keyboard.setColumnCount(4);keyboard.setRowCount(4);Ui.pad(keyboard,17,4,17,12);String[] keys={"7","8","9","4","5","6","1","2","3",".","0","清空"};
        for(int i=0;i<keys.length;i++){final String key=keys[i];TextView button=key(key,false,()->press(key));if(key.equals("清空"))button.setTextSize(13);GridLayout.LayoutParams p=keyParams(i/3,i%3,1);keyboard.addView(button,p);}
        keyboard.addView(key("⌫",false,()->press("backspace")),keyParams(0,3,1));TextView save=key("保存\n✓",true,this::save);save.setId(R.id.entry_save);save.setTextSize(16);keyboard.addView(save,keyParams(1,3,3));root.addView(keyboard);
    }
    private TextView key(String label,boolean primary,Runnable action){TextView v=Ui.action(this,label,primary,action);v.setTextSize(22);if(!primary)v.setBackground(Ui.border(Ui.WHITE,11,this));Ui.pad(v,2,4,2,4);return v;}
    private GridLayout.LayoutParams keyParams(int row,int col,int span){GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(row,span),GridLayout.spec(col,1,1f));p.width=0;p.height=Ui.dp(this,span*49-6);p.setMargins(Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3));return p;}
    private void press(String key){if(saving)return;capture();String next=amount;if(key.equals("清空"))next="";else if(key.equals("backspace"))next=next.isEmpty()?"":next.substring(0,next.length()-1);else if(key.equals(".")){if(next.contains("."))return;next=(next.isEmpty()?"0":next)+".";}else{if(amountInput.hasSelection())next=key;else next+=key;}
        if(next.isEmpty()||next.matches("[0-9]{0,9}(\\.[0-9]{0,2})?")){amount=next;amountInput.setText(amount);amountInput.setSelection(amount.length());}showError("");
    }
    private void save(){if(saving)return;capture();final Ledger.Entry entry;
        try{String now=Instant.now().toString();entry=new Ledger.Entry(id==null?java.util.UUID.randomUUID().toString():id,type,category,Ledger.parseAmount(amount),date,note,createdAt==null?now:createdAt,now,ledger.catalog);}catch(Exception e){showError(e.getMessage());return;}
        final boolean isNew=id==null;id=entry.id;createdAt=entry.createdAt;
        saving=true;showError("正在保存…");worker.execute(()->{try{store.save(entry);runOnUiThread(()->{if(isDestroyed())return;setResult(RESULT_OK,new Intent().putExtra("date",entry.date));Toast.makeText(this,isNew?"记好了，今天也在认真生活":"账目已更新",Toast.LENGTH_SHORT).show();finish();});}catch(Exception e){runOnUiThread(()->{if(isDestroyed())return;saving=false;showError(getString(R.string.save_error,e.getMessage()));});}});
    }
    private void requestClose(){if(saving)return;if(!ready){finish();return;}capture();if(!fingerprint().equals(original))new AlertDialog.Builder(this).setTitle("放弃本次输入？").setMessage("尚未保存的内容会丢失。").setNegativeButton("继续编辑",null).setPositiveButton("放弃",(d,w)->finish()).show();else finish();}
    private void selectCategory(String selected){if(saving)return;capture();category=selected;rememberCategory(selected);render();}
    private void rememberCategory(String selected){recentCategories=RecentCategories.select(recentCategories,selected);getSharedPreferences("entry-ui",MODE_PRIVATE).edit().putString("recent-projects",String.join(",",recentCategories)).apply();}
    private void showError(String message){if(error==null)return;error.setText(message);error.setVisibility(message==null||message.isEmpty()?View.GONE:View.VISIBLE);}
    private void showNotes(){if(noteInput==null||!noteInput.hasFocus())return;ArrayAdapter<?> adapter=(ArrayAdapter<?>)noteInput.getAdapter();adapter.getFilter().filter(null,count->{if(!isDestroyed()&&noteInput.hasFocus()&&count>0){noteInput.setDropDownWidth(noteInput.getWidth());noteInput.showDropDown();}});}
    private void editCategory(Ledger.Category existing){if(saving)return;capture();CategoryActivity.showEditor(this,store,ledger,type,existing,c->{worker.execute(()->{try{Ledger.Data latest=store.read();runOnUiThread(()->{if(isDestroyed())return;ledger=latest;category=c==null?(type.equals("expense")?"food":"salary"):c.id;if(c!=null)rememberCategory(c.id);render();});}catch(Exception e){runOnUiThread(()->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());}});});}
    public static final class NoteInput extends AutoCompleteTextView {
        public NoteInput(Context context){super(context);}
        @Override public boolean enoughToFilter(){return true;}
    }
    @Override public void onBackPressed(){requestClose();}
    @Override protected void onSaveInstanceState(Bundle out){capture();out.putBoolean("ready",ready);out.putString("id",id);out.putString("type",type);out.putString("category",category);out.putString("amount",amount);out.putString("note",note);out.putString("date",date);out.putString("createdAt",createdAt);out.putString("original",original);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){worker.execute(store::close);super.onDestroy();}
}
