package com.simaoqingnian.myaccount;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;

public final class EntryActivity extends Activity {
    private final ExecutorService worker=LedgerStore.IO;private LedgerStore store;
    private String id,type="expense",category="",amount="",note="",date=EntrySession.CURRENT.date().toString(),createdAt,original,pendingStamp;
    private Ledger.Data ledger=Ledger.Data.empty();
    private List<String> recentCategories=new ArrayList<>();
    private EditText amountInput;private NoteInput noteInput;private GridLayout keyboard;private HorizontalScrollView noteHistory;private TextView error;private boolean saving,ready,pendingContinue,imeVisible,noteImeVisible,resumed;private LinearLayout root;
    private final List<TextView> noteChips=new ArrayList<>();
    private final Runnable keyboardSync=this::syncKeyboardForFocus;
    @Override public void onCreate(Bundle saved){super.onCreate(saved);store=new LedgerStore(this);id=getIntent().getStringExtra("entryId");
        recentCategories=Arrays.asList(getSharedPreferences("entry-ui",MODE_PRIVATE).getString("recent-projects","").split(","));
        if(saved!=null){id=saved.getString("id");type=saved.getString("type","expense");category=saved.getString("category","");amount=saved.getString("amount","");note=saved.getString("note","");date=saved.getString("date",date);createdAt=saved.getString("createdAt");original=saved.getString("original");ready=saved.getBoolean("ready");pendingStamp=saved.getString("pendingStamp");pendingContinue=saved.getBoolean("pendingContinue");}
        root=Ui.column(this);root.setBackgroundColor(Ui.PAPER);setContentView(root);Ui.insets(this,root);root.addView(Ui.text(this,"正在读取账目与历史备注…",15,Ui.MUTED));
        worker.execute(()->{try{Ledger.Data latest=store.read();Ledger.Entry found=null;if(id!=null){for(Ledger.Entry e:latest.entries)if(e.id.equals(id))found=e;if(!ready&&pendingStamp==null&&found==null)throw new IllegalArgumentException("这笔账目不存在");}Ledger.Entry entry=found;runOnUiThread(()->{if(isDestroyed())return;ledger=latest;
            if(pendingStamp!=null){boolean committed=entry!=null&&pendingStamp.equals(entry.updatedAt)&&entry.type.equals(type)&&entry.categoryId.equals(category)&&entry.date.equals(date)&&entry.note.equals(note.trim())&&entry.amount==Ledger.parseAmount(amount);boolean keepGoing=pendingContinue;pendingStamp=null;pendingContinue=false;if(committed){onSaved(entry,latest,keepGoing,false);return;}}
            if(entry!=null&&!ready){type=entry.type;category=entry.categoryId;amount=Ledger.plain(entry.amount);note=entry.note;date=entry.date;createdAt=entry.createdAt;}
            if(saved==null&&id==null)category=firstCategory();else try{ledger.category(category);}catch(IllegalArgumentException e){category=firstCategory();}
            if(original==null)original=fingerprint();ready=true;render();});}catch(Exception e){runOnUiThread(()->{if(isDestroyed())return;Toast.makeText(this,"读取失败："+e.getMessage(),Toast.LENGTH_LONG).show();finish();});}});
    }
    private String fingerprint(){return type+"\n"+category+"\n"+amount+"\n"+date+"\n"+note;}
    private void capture(){if(amountInput!=null)amount=amountInput.getText().toString();if(noteInput!=null)note=noteInput.getText().toString();}
    private void render(){
        if(root!=null)root.removeCallbacks(keyboardSync);keyboard=null;noteHistory=null;imeVisible=false;noteImeVisible=false;noteChips.clear();root=Ui.column(this);root.setBackgroundColor(Ui.PAPER);setContentView(root);LinearLayout currentRoot=root;Ui.insets(this,root,visible->{if(root==currentRoot)onImeVisibilityChanged(visible);});
        LinearLayout header=Ui.row(this);Ui.pad(header,17,7,17,5);TextView close=Ui.smallAction(this,"取消",this::requestClose);header.addView(close);TextView title=Ui.bold(this,id==null?"记一笔":"编辑账目",20,Ui.INK);title.setGravity(Gravity.CENTER);Ui.weight(header,title);header.addView(Ui.smallAction(this,"保存",this::save));root.addView(header);
        LinearLayout body=Ui.column(this);Ui.pad(body,18,0,18,4);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout types=Ui.row(this);types.setBackground(Ui.background(Ui.SAGE,12,this));for(String value:new String[]{"expense","income"}){TextView t=Ui.action(this,value.equals("expense")?"支出":"收入",type.equals(value),()->{if(saving)return;capture();type=value;category=firstCategory();render();});t.setMinHeight(Ui.dp(this,42));if(!type.equals(value))t.setBackgroundColor(android.graphics.Color.TRANSPARENT);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(this,42),1);p.setMargins(Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3));types.addView(t,p);}body.addView(types);Ui.space(body,4);
        LinearLayout amountRow=Ui.row(this);amountRow.addView(Ui.text(this,"¥",28,Ui.GREEN));amountInput=new EditText(this);amountInput.setId(R.id.entry_amount);amountInput.setText(amount);amountInput.setTextColor(Ui.GREEN);amountInput.setTextSize(38);amountInput.setSingleLine(true);amountInput.setIncludeFontPadding(false);Ui.pad(amountInput,0,0,0,0);amountInput.setHint("0.00");amountInput.setHintTextColor(0xFF9CA995);amountInput.setGravity(Gravity.END);amountInput.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amountInput.setShowSoftInputOnFocus(false);amountInput.setBackgroundColor(android.graphics.Color.TRANSPARENT);amountInput.setSelectAllOnFocus(true);amountInput.setContentDescription("记账金额");amountInput.setOnFocusChangeListener((v,focused)->{if(focused)syncKeyboardForFocus();});amountInput.setOnClickListener(v->syncKeyboardForFocus());Ui.weight(amountRow,amountInput);body.addView(amountRow,new LinearLayout.LayoutParams(-1,Ui.dp(this,56)));Ui.divider(body);Ui.space(body,4);
        LinearLayout categoryHeader=Ui.row(this);TextView selectedName=Ui.bold(this,"项目 · "+ledger.category(category).name,13,Ui.GREEN);selectedName.setSingleLine(true);selectedName.setEllipsize(android.text.TextUtils.TruncateAt.END);Ui.weight(categoryHeader,selectedName);TextView custom=Ui.smallAction(this,"＋ 自定义",()->editCategory(null));custom.setMinHeight(Ui.dp(this,32));Ui.pad(custom,6,2,6,2);categoryHeader.addView(custom);body.addView(categoryHeader);
        GridLayout categories=new GridLayout(this);categories.setColumnCount(5);int index=0;
        for(Ledger.Category c:RecentCategories.order(ledger.catalog,type,recentCategories)){
            LinearLayout choice=Ui.column(this);choice.setGravity(Gravity.CENTER);Ui.Icon symbol=new Ui.Icon(this,c.icon,category.equals(c.id)?Ui.WHITE:Ui.GREEN);symbol.setBackground(Ui.background(category.equals(c.id)?Ui.GREEN:Ui.SAGE,13,this));Ui.pad(symbol,9,9,9,9);choice.addView(symbol,new LinearLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,40)));TextView name=Ui.text(this,c.name,11,category.equals(c.id)?Ui.GREEN:Ui.MUTED);name.setGravity(Gravity.CENTER);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=Ui.dp(this,3);choice.addView(name,np);choice.setContentDescription(c.name+(category.equals(c.id)?"，已选择":"")+(c.custom()?"，长按编辑":""));choice.setOnClickListener(v->selectCategory(c.id));if(c.custom())choice.setOnLongClickListener(v->{editCategory(c);return true;});choice.setFocusable(true);GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(index/5),GridLayout.spec(index%5,1,1f));p.width=0;p.height=Ui.dp(this,60);categories.addView(choice,p);index++;
        }
        ScrollView categoryScroll=new ScrollView(this);categoryScroll.setFillViewport(false);categoryScroll.addView(categories);body.addView(categoryScroll,new LinearLayout.LayoutParams(-1,0,1));
        // Keep the date above both the amount keypad and the system IME.
        LinearLayout details=Ui.column(this);Ui.pad(details,18,3,18,5);root.addView(details,new LinearLayout.LayoutParams(-1,-2));
        noteInput=new NoteInput(this);noteInput.setId(R.id.entry_note);noteInput.setSingleLine(true);noteInput.setTextSize(14);noteInput.setTextColor(Ui.INK);noteInput.setHintTextColor(Ui.MUTED);noteInput.setHint("输入备注（可选）");noteInput.setBackground(Ui.border(Ui.WHITE,12,this));Ui.pad(noteInput,11,6,11,6);noteInput.setMinHeight(Ui.dp(this,38));noteInput.setText(note);noteInput.setContentDescription("账目备注，点击后显示可左右滑动的历史备注");noteInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});noteInput.setImeOptions(EditorInfo.IME_ACTION_DONE);noteInput.imeDismissed=this::showAmountKeyboard;
        noteInput.setOnFocusChangeListener((v,focused)->{if(!focused)noteImeVisible=false;setNoteHistoryVisible(focused);syncKeyboardForFocus();});noteInput.setOnClickListener(v->{setNoteHistoryVisible(true);syncKeyboardForFocus();});
        noteInput.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){showAmountKeyboard();return true;}return false;});
        noteInput.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){note=s.toString();updateNoteSelection();}public void afterTextChanged(Editable e){}});
        addNoteHistory(details);details.addView(noteInput);Ui.space(details,4);
        LinearLayout dateRow=Ui.row(this);dateRow.addView(dayButton(-1),new LinearLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,44)));
        TextView dateButton=Ui.action(this,"账目日期  "+date+"  ⌄",false,()->{if(saving)return;capture();LocalDate value=LocalDate.parse(date);DatePickerDialog picker=new DatePickerDialog(this,(view,y,m,d)->{capture();LocalDate selected=LocalDate.of(y,m+1,d);EntrySession.CURRENT.select(selected);date=selected.toString();render();},value.getYear(),value.getMonthValue()-1,value.getDayOfMonth());picker.getDatePicker().setMinDate(Ledger.MIN_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();});dateButton.setId(R.id.entry_date);dateButton.setBackground(Ui.border(Ui.WHITE,12,this));dateButton.setTextSize(12);dateButton.setMinHeight(Ui.dp(this,44));Ui.pad(dateButton,4,4,4,4);dateButton.setContentDescription("账目日期 "+date+"，点击选择日期，手动选定后持续沿用");LinearLayout.LayoutParams dateParams=new LinearLayout.LayoutParams(0,-2,1);dateParams.setMargins(Ui.dp(this,4),0,Ui.dp(this,4),0);dateRow.addView(dateButton,dateParams);dateRow.addView(dayButton(1),new LinearLayout.LayoutParams(Ui.dp(this,40),Ui.dp(this,44)));details.addView(dateRow);error=Ui.text(this,"",12,Ui.DANGER);error.setVisibility(View.GONE);error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);Ui.pad(error,0,4,0,0);details.addView(error);
        keyboard=new GridLayout(this);keyboard.setId(R.id.entry_keypad);keyboard.setColumnCount(4);keyboard.setRowCount(4);Ui.pad(keyboard,17,4,17,12);String[] keys={"7","8","9","4","5","6","1","2","3",".","0","清空"};
        for(int i=0;i<keys.length;i++){final String key=keys[i];TextView button=key(key,false,()->press(key));if(key.equals("清空"))button.setTextSize(13);GridLayout.LayoutParams p=keyParams(i/3,i%3,1);keyboard.addView(button,p);}
        keyboard.addView(key("⌫",false,()->press("backspace")),keyParams(0,3,1));LinearLayout actions=Ui.column(this);TextView save=key("保存",true,this::save);save.setId(R.id.entry_save);save.setTextSize(16);actions.addView(save,new LinearLayout.LayoutParams(-1,0,1));Ui.space(actions,6);TextView next=key("继续",false,()->save(true));next.setId(R.id.entry_continue);next.setContentDescription("保存当前账目并继续记下一笔");next.setTextSize(16);actions.addView(next,new LinearLayout.LayoutParams(-1,0,1));keyboard.addView(actions,keyParams(1,3,3));root.addView(keyboard);amountInput.requestFocus();syncKeyboardForFocus();postKeyboardSync();root.requestApplyInsets();
    }
    private void addNoteHistory(LinearLayout details){
        List<String> suggestions=Ledger.noteSuggestions(ledger,type,category);if(suggestions.isEmpty())return;
        HorizontalScrollView strip=new HorizontalScrollView(this);noteHistory=strip;strip.setVisibility(View.GONE);strip.setId(R.id.entry_note_history);strip.setHorizontalScrollBarEnabled(false);strip.setHorizontalFadingEdgeEnabled(true);strip.setContentDescription("历史备注，可左右滑动，点击采用");LinearLayout choices=Ui.row(this);Ui.pad(choices,0,2,0,2);
        for(String suggestion:suggestions){TextView chip=Ui.action(this,suggestion,false,()->{if(saving)return;noteInput.setText(suggestion);noteInput.setSelection(noteInput.length());showAmountKeyboard();});chip.setTextSize(12);chip.setMinHeight(Ui.dp(this,32));chip.setMaxWidth(Ui.dp(this,160));chip.setSingleLine(true);chip.setEllipsize(android.text.TextUtils.TruncateAt.END);Ui.pad(chip,10,4,10,4);chip.setTag(suggestion);chip.setTooltipText(suggestion);chip.setContentDescription("采用历史备注："+suggestion);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,Ui.dp(this,32));p.rightMargin=Ui.dp(this,6);choices.addView(chip,p);noteChips.add(chip);}
        strip.addView(choices);LinearLayout.LayoutParams historyParams=new LinearLayout.LayoutParams(-1,Ui.dp(this,36));historyParams.bottomMargin=Ui.dp(this,4);details.addView(strip,historyParams);updateNoteSelection();
    }
    private void setNoteHistoryVisible(boolean visible){if(noteHistory!=null)noteHistory.setVisibility(visible?View.VISIBLE:View.GONE);}
    private void updateNoteSelection(){for(TextView chip:noteChips){boolean selected=chip.getTag().equals(note);chip.setSelected(selected);chip.setTextColor(selected?Ui.WHITE:Ui.GREEN);chip.setBackground(Ui.background(selected?Ui.GREEN:Ui.SAGE,10,this));}}
    private void onImeVisibilityChanged(boolean visible){
        boolean noteFocused=noteInput!=null&&noteInput.hasFocus(),closed=noteImeVisible&&!visible;imeVisible=visible;noteImeVisible=visible&&noteFocused;
        if(keyboard!=null)keyboard.setVisibility(visible&&noteFocused?View.GONE:View.VISIBLE);
        // A late IME restore must not replace the amount keypad after window focus returns.
        if(visible&&!noteFocused)postKeyboardSync();else if(closed&&noteFocused&&resumed&&hasWindowFocus())showAmountKeyboard();
    }
    private void syncKeyboardForFocus(){
        if(isDestroyed())return;boolean noteFocused=noteInput!=null&&noteInput.hasFocus();
        int mode=getWindow().getAttributes().softInputMode;
        int desired=(mode&~WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE)|(noteFocused?WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED:WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if(mode!=desired)getWindow().setSoftInputMode(desired);
        if(amountInput!=null)amountInput.setShowSoftInputOnFocus(false);
        if(noteFocused)return;
        noteImeVisible=false;setNoteHistoryVisible(false);if(keyboard!=null)keyboard.setVisibility(View.VISIBLE);
        if(!resumed||!hasWindowFocus()||amountInput==null)return;
        if(Build.VERSION.SDK_INT>=30&&getWindow().getInsetsController()!=null)getWindow().getInsetsController().hide(WindowInsets.Type.ime());
        InputMethodManager input=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(input!=null)input.hideSoftInputFromWindow(amountInput.getWindowToken(),0);
    }
    private void postKeyboardSync(){if(root!=null){root.removeCallbacks(keyboardSync);if(resumed&&hasWindowFocus())root.post(keyboardSync);}}
    private void showAmountKeyboard(){if(isDestroyed()||amountInput==null)return;capture();setNoteHistoryVisible(false);if(amountInput.isEnabled()){amountInput.requestFocus();amountInput.setSelection(amountInput.length());}syncKeyboardForFocus();postKeyboardSync();}
    private TextView key(String label,boolean primary,Runnable action){TextView v=Ui.action(this,label,primary,action);v.setTextSize(22);if(!primary)v.setBackground(Ui.border(Ui.WHITE,11,this));Ui.pad(v,2,4,2,4);return v;}
    private GridLayout.LayoutParams keyParams(int row,int col,int span){GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(row,span),GridLayout.spec(col,1,1f));p.width=0;p.height=Ui.dp(this,span*49-6);p.setMargins(Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3),Ui.dp(this,3));return p;}
    private void press(String key){if(saving)return;capture();String next=amount;if(key.equals("清空"))next="";else if(key.equals("backspace"))next=next.isEmpty()?"":next.substring(0,next.length()-1);else if(key.equals(".")){if(next.contains("."))return;next=(next.isEmpty()?"0":next)+".";}else{if(amountInput.hasSelection())next=key;else next+=key;}
        if(next.isEmpty()||next.matches("[0-9]{0,9}(\\.[0-9]{0,2})?")){amount=next;amountInput.setText(amount);amountInput.setSelection(amount.length());}showError("");
    }
    private void save(){save(false);}
    private void save(boolean keepGoing){if(saving||!ready)return;capture();final Ledger.Entry entry;
        try{String now=Instant.now().toString();entry=new Ledger.Entry(id==null?java.util.UUID.randomUUID().toString():id,type,category,Ledger.parseAmount(amount),date,note,createdAt==null?now:createdAt,now,ledger.catalog);}catch(Exception e){showError(e.getMessage());return;}
        final boolean isNew=id==null;id=entry.id;createdAt=entry.createdAt;pendingStamp=entry.updatedAt;pendingContinue=keepGoing;
        saving=true;showError("正在保存…");setControlsEnabled(root,false);worker.execute(()->{boolean committed=false;try{store.save(entry);committed=true;Ledger.Data latest=keepGoing?store.read():ledger;runOnUiThread(()->{if(!isDestroyed())onSaved(entry,latest,keepGoing,isNew);});}catch(Exception e){final boolean stored=committed;runOnUiThread(()->{if(isDestroyed())return;saving=false;pendingStamp=null;pendingContinue=false;setControlsEnabled(root,true);showError(stored?"账目已保存，下一笔准备失败，请重试："+e.getMessage():getString(R.string.save_error,e.getMessage()));});}});
    }
    private void onSaved(Ledger.Entry entry,Ledger.Data latest,boolean keepGoing,boolean isNew){
        setResult(RESULT_OK,new Intent().putExtra("date",entry.date));pendingStamp=null;pendingContinue=false;saving=false;
        if(!keepGoing){Toast.makeText(this,isNew?"记好了，今天也在认真生活":"账目已更新",Toast.LENGTH_SHORT).show();finish();return;}
        ledger=latest;id=null;createdAt=null;type=entry.type;category=entry.categoryId;date=entry.date;amount="";note="";original=fingerprint();ready=true;render();amountInput.requestFocus();Toast.makeText(this,"已保存，继续记下一笔",Toast.LENGTH_SHORT).show();
    }
    private void setControlsEnabled(View view,boolean enabled){if(view!=error){boolean available=enabled;if(enabled&&(view.getId()==R.id.entry_previous_day||view.getId()==R.id.entry_next_day))available=EntrySession.CURRENT.adjacent(LocalDate.parse(date),view.getId()==R.id.entry_previous_day?-1:1)!=null;view.setEnabled(available);}if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)setControlsEnabled(group.getChildAt(i),enabled);}}
    private String firstCategory(){return RecentCategories.first(ledger.catalog,type,recentCategories).id;}
    private TextView dayButton(int direction){TextView button=Ui.action(this,direction<0?"‹":"›",false,()->changeDay(direction));button.setTextSize(23);button.setMinHeight(Ui.dp(this,44));Ui.pad(button,0,0,0,0);button.setId(direction<0?R.id.entry_previous_day:R.id.entry_next_day);button.setContentDescription(direction<0?"前一天":"后一天");button.setEnabled(EntrySession.CURRENT.adjacent(LocalDate.parse(date),direction)!=null);button.setAlpha(button.isEnabled()?1:.3f);return button;}
    private void changeDay(int direction){if(saving)return;capture();LocalDate next=EntrySession.CURRENT.adjacent(LocalDate.parse(date),direction);if(next==null)return;EntrySession.CURRENT.select(next);date=next.toString();render();}
    private void requestClose(){if(saving)return;if(!ready){finish();return;}capture();if(!fingerprint().equals(original))new AlertDialog.Builder(this).setTitle("放弃本次输入？").setMessage("尚未保存的内容会丢失。").setNegativeButton("继续编辑",null).setPositiveButton("放弃",(d,w)->finish()).show();else finish();}
    private void selectCategory(String selected){if(saving)return;capture();category=selected;rememberCategory(selected);render();}
    private void rememberCategory(String selected){recentCategories=RecentCategories.select(recentCategories,selected);getSharedPreferences("entry-ui",MODE_PRIVATE).edit().putString("recent-projects",String.join(",",recentCategories)).apply();}
    private void showError(String message){if(error==null)return;error.setText(message);error.setVisibility(message==null||message.isEmpty()?View.GONE:View.VISIBLE);}
    private void editCategory(Ledger.Category existing){if(saving)return;capture();CategoryActivity.showEditor(this,store,ledger,type,existing,c->{worker.execute(()->{try{Ledger.Data latest=store.read();runOnUiThread(()->{if(isDestroyed())return;ledger=latest;category=c==null?firstCategory():c.id;if(c!=null)rememberCategory(c.id);render();});}catch(Exception e){runOnUiThread(()->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());}});});}
    public static final class NoteInput extends EditText {
        Runnable imeDismissed;
        public NoteInput(Context context){super(context);}
        @Override public boolean onKeyPreIme(int code,KeyEvent event){boolean handled=super.onKeyPreIme(code,event);if(code==KeyEvent.KEYCODE_BACK&&event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled()&&imeDismissed!=null)post(imeDismissed);return handled;}
    }
    @Override protected void onResume(){super.onResume();resumed=true;syncKeyboardForFocus();postKeyboardSync();}
    @Override protected void onPause(){resumed=false;if(root!=null)root.removeCallbacks(keyboardSync);super.onPause();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused){syncKeyboardForFocus();postKeyboardSync();}else if(root!=null)root.removeCallbacks(keyboardSync);}
    @Override public void onBackPressed(){if(imeVisible||(noteInput!=null&&noteInput.hasFocus())){showAmountKeyboard();return;}requestClose();}
    @Override protected void onSaveInstanceState(Bundle out){capture();out.putBoolean("ready",ready);out.putString("id",id);out.putString("type",type);out.putString("category",category);out.putString("amount",amount);out.putString("note",note);out.putString("date",date);out.putString("createdAt",createdAt);out.putString("original",original);if(saving){out.putString("pendingStamp",pendingStamp);out.putBoolean("pendingContinue",pendingContinue);}super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){if(root!=null)root.removeCallbacks(keyboardSync);worker.execute(store::close);super.onDestroy();}
}
