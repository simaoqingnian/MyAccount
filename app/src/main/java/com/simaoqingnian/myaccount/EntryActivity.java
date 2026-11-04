package com.simaoqingnian.myaccount;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;

public final class EntryActivity extends Activity {
    private final ExecutorService worker=LedgerStore.IO;private LedgerStore store;
    private String id,type="expense",category="food",amount="",note="",date=Ledger.today().toString(),createdAt,original;
    private EditText amountInput,noteInput;private TextView error;private boolean saving;private LinearLayout root;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);store=new LedgerStore(this);id=getIntent().getStringExtra("entryId");
        if(saved!=null){id=saved.getString("id");type=saved.getString("type","expense");category=saved.getString("category","food");amount=saved.getString("amount","");note=saved.getString("note","");date=saved.getString("date",date);createdAt=saved.getString("createdAt");original=saved.getString("original");render();}
        else if(id!=null){root=Ui.column(this);root.setBackgroundColor(Ui.PAPER);setContentView(root);Ui.insets(this,root);root.addView(Ui.text(this,"正在读取账目…",15,Ui.MUTED));worker.execute(()->{try{Ledger.Entry found=null;for(Ledger.Entry e:store.read().entries)if(e.id.equals(id))found=e;if(found==null)throw new IllegalArgumentException("这笔账目不存在");Ledger.Entry e=found;runOnUiThread(()->{if(isDestroyed())return;type=e.type;category=e.categoryId;amount=Ledger.plain(e.amount);note=e.note;date=e.date;createdAt=e.createdAt;original=fingerprint();render();});}catch(Exception e){runOnUiThread(()->{Toast.makeText(this,"读取失败："+e.getMessage(),Toast.LENGTH_LONG).show();finish();});}});}
        else {original=fingerprint();render();}
    }
    private String fingerprint(){return type+"\n"+category+"\n"+amount+"\n"+date+"\n"+note;}
    private void capture(){if(amountInput!=null)amount=amountInput.getText().toString();if(noteInput!=null)note=noteInput.getText().toString();}
    private void render(){
        root=Ui.column(this);root.setBackgroundColor(Ui.PAPER);setContentView(root);Ui.insets(this,root);
        LinearLayout header=Ui.row(this);Ui.pad(header,17,7,17,5);TextView close=Ui.smallAction(this,"取消",this::requestClose);header.addView(close);TextView title=Ui.bold(this,id==null?"记一笔":"编辑账目",20,Ui.INK);title.setGravity(Gravity.CENTER);Ui.weight(header,title);header.addView(Ui.smallAction(this,"保存",this::save));root.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);LinearLayout body=Ui.column(this);Ui.pad(body,22,4,22,12);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout types=Ui.row(this);types.setBackground(Ui.background(Ui.SAGE,12,this));for(String value:new String[]{"expense","income"}){TextView t=Ui.action(this,value.equals("expense")?"支出":"收入",type.equals(value),()->{capture();type=value;category=value.equals("expense")?"food":"salary";render();});t.setMinHeight(Ui.dp(this,42));if(!type.equals(value))t.setBackgroundColor(android.graphics.Color.TRANSPARENT);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(this,42),1);p.setMargins(Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3));types.addView(t,p);}body.addView(types);Ui.space(body,11);
        LinearLayout amountRow=Ui.row(this);amountRow.addView(Ui.text(this,"¥",28,Ui.GREEN));amountInput=new EditText(this);amountInput.setId(R.id.entry_amount);amountInput.setText(amount);amountInput.setTextColor(Ui.GREEN);amountInput.setTextSize(38);amountInput.setSingleLine(true);amountInput.setHint("0.00");amountInput.setHintTextColor(0xFF9CA995);amountInput.setGravity(Gravity.END);amountInput.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amountInput.setShowSoftInputOnFocus(false);amountInput.setBackgroundColor(android.graphics.Color.TRANSPARENT);amountInput.setSelectAllOnFocus(true);amountInput.setContentDescription("记账金额");Ui.weight(amountRow,amountInput);body.addView(amountRow,new LinearLayout.LayoutParams(-1,Ui.dp(this,67)));Ui.divider(body);Ui.space(body,13);
        GridLayout categories=new GridLayout(this);categories.setColumnCount(4);int index=0;
        for(Ledger.Category c:Ledger.categories(type)){
            LinearLayout choice=Ui.column(this);choice.setGravity(Gravity.CENTER);Ui.Icon symbol=new Ui.Icon(this,c.icon,category.equals(c.id)?Ui.WHITE:Ui.GREEN);symbol.setBackground(Ui.background(category.equals(c.id)?Ui.GREEN:Ui.SAGE,13,this));Ui.pad(symbol,9,9,9,9);choice.addView(symbol,new LinearLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,40)));TextView name=Ui.text(this,c.name,11,category.equals(c.id)?Ui.GREEN:Ui.MUTED);name.setGravity(Gravity.CENTER);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=Ui.dp(this,5);choice.addView(name,np);choice.setContentDescription(c.name+(category.equals(c.id)?"，已选择":""));choice.setOnClickListener(v->{capture();category=c.id;render();});choice.setFocusable(true);GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(index/4),GridLayout.spec(index%4,1,1f));p.width=0;p.height=Ui.dp(this,70);categories.addView(choice,p);index++;
        }body.addView(categories);Ui.space(body,7);
        noteInput=Ui.input(this,"写点备注，记住这一笔");noteInput.setId(R.id.entry_note);noteInput.setText(note);noteInput.setContentDescription("账目备注");noteInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});body.addView(noteInput);Ui.space(body,9);
        TextView dateButton=Ui.action(this,"账目日期     "+date+"   ›",false,()->{capture();LocalDate value=LocalDate.parse(date);DatePickerDialog picker=new DatePickerDialog(this,(view,y,m,d)->{capture();date=LocalDate.of(y,m+1,d).toString();render();},value.getYear(),value.getMonthValue()-1,value.getDayOfMonth());picker.getDatePicker().setMinDate(Ledger.MIN_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();});dateButton.setBackground(Ui.border(Ui.WHITE,12,this));dateButton.setTextSize(12);body.addView(dateButton);error=Ui.text(this,"",12,Ui.DANGER);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);Ui.pad(error,0,7,0,0);body.addView(error);
        GridLayout keyboard=new GridLayout(this);keyboard.setColumnCount(4);keyboard.setRowCount(4);Ui.pad(keyboard,17,4,17,12);String[] keys={"7","8","9","4","5","6","1","2","3",".","0","清空"};
        for(int i=0;i<keys.length;i++){final String key=keys[i];TextView button=key(key,false,()->press(key));if(key.equals("清空"))button.setTextSize(13);GridLayout.LayoutParams p=keyParams(i/3,i%3,1);keyboard.addView(button,p);}
        keyboard.addView(key("⌫",false,()->press("backspace")),keyParams(0,3,1));TextView save=key("保存\n✓",true,this::save);save.setId(R.id.entry_save);save.setTextSize(16);keyboard.addView(save,keyParams(1,3,3));root.addView(keyboard);
    }
    private TextView key(String label,boolean primary,Runnable action){TextView v=Ui.action(this,label,primary,action);v.setTextSize(22);if(!primary)v.setBackground(Ui.border(Ui.WHITE,11,this));Ui.pad(v,2,4,2,4);return v;}
    private GridLayout.LayoutParams keyParams(int row,int col,int span){GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(row,span),GridLayout.spec(col,1,1f));p.width=0;p.height=Ui.dp(this,span*49-6);p.setMargins(Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3));return p;}
    private void press(String key){if(saving)return;capture();String next=amount;if(key.equals("清空"))next="";else if(key.equals("backspace"))next=next.isEmpty()?"":next.substring(0,next.length()-1);else if(key.equals(".")){if(next.contains("."))return;next=(next.isEmpty()?"0":next)+".";}else{if(amountInput.hasSelection())next=key;else next+=key;}
        if(next.isEmpty()||next.matches("[0-9]{0,9}(\\.[0-9]{0,2})?")){amount=next;amountInput.setText(amount);amountInput.setSelection(amount.length());}error.setText("");
    }
    private void save(){if(saving)return;capture();final Ledger.Entry entry;
        try{String now=Instant.now().toString();entry=new Ledger.Entry(id==null?java.util.UUID.randomUUID().toString():id,type,category,Ledger.parseAmount(amount),date,note,createdAt==null?now:createdAt,now);}catch(Exception e){error.setText(e.getMessage());return;}
        final boolean isNew=id==null;id=entry.id;createdAt=entry.createdAt;
        saving=true;error.setText("正在保存…");worker.execute(()->{try{store.save(entry);runOnUiThread(()->{if(isDestroyed())return;setResult(RESULT_OK,new Intent().putExtra("date",entry.date));Toast.makeText(this,isNew?"记好了，今天也在认真生活":"账目已更新",Toast.LENGTH_SHORT).show();finish();});}catch(Exception e){runOnUiThread(()->{if(isDestroyed())return;saving=false;error.setText(getString(R.string.save_error,e.getMessage()));});}});
    }
    private void requestClose(){if(saving)return;capture();if(!fingerprint().equals(original))new AlertDialog.Builder(this).setTitle("放弃本次输入？").setMessage("尚未保存的内容会丢失。").setNegativeButton("继续编辑",null).setPositiveButton("放弃",(d,w)->finish()).show();else finish();}
    @Override public void onBackPressed(){requestClose();}
    @Override protected void onSaveInstanceState(Bundle out){capture();out.putString("id",id);out.putString("type",type);out.putString("category",category);out.putString("amount",amount);out.putString("note",note);out.putString("date",date);out.putString("createdAt",createdAt);out.putString("original",original);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){worker.execute(store::close);super.onDestroy();}
}
