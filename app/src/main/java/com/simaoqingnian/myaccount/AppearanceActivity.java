package com.simaoqingnian.myaccount;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class AppearanceActivity extends ThemedActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.PAPER);setContentView(root);Ui.insets(this,root);
        LinearLayout header=Ui.row(this);Ui.pad(header,14,6,18,6);header.addView(Ui.smallAction(this,"返回",this::finish));Ui.weight(header,Ui.bold(this,"外观主题",21,Ui.INK));root.addView(header);
        ScrollView scroll=new ScrollView(this);LinearLayout content=Ui.column(this);Ui.pad(content,18,10,18,22);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content.addView(Ui.text(this,"让记账，更合心意。",14,Ui.MUTED));Ui.space(content,18);
        String[] descriptions={"细腻拉丝，浅银底色。\n石墨文字，香槟金点缀。","温暖纸色，清爽森林绿。\n保留熟悉的记账感受。","奶油底色，软桃粉点缀。\n陪你记下生活的小美好。"};
        for(int i=0;i<ThemePalette.ALL.length;i++) {
            ThemePalette palette=ThemePalette.ALL[i];boolean selected=palette==Ui.theme;
            LinearLayout choice=Ui.row(this);choice.setBackground(Ui.border(Ui.WHITE,18,this));Ui.pad(choice,12,12,12,12);
            if(selected){android.graphics.drawable.GradientDrawable border=Ui.border(Ui.WHITE,18,this);border.setStroke(Ui.dp(this,2),Ui.GREEN);choice.setBackground(border);}
            choice.addView(new ThemePreview(this,palette),new LinearLayout.LayoutParams(Ui.dp(this,100),Ui.dp(this,145)));
            LinearLayout labels=Ui.column(this);Ui.pad(labels,14,0,5,0);labels.addView(Ui.bold(this,palette.name,17,Ui.INK));
            if(palette==ThemePalette.TITANIUM){Ui.space(labels,5);labels.addView(Ui.text(this,"默认主题",10,Ui.GREEN));}
            Ui.space(labels,9);labels.addView(Ui.text(this,descriptions[i],11,Ui.MUTED));Ui.weight(choice,labels);
            Ui.Icon check=new Ui.Icon(this,selected?"check":"circle",selected?Ui.GREEN:Ui.MUTED);choice.addView(check,new LinearLayout.LayoutParams(Ui.dp(this,22),Ui.dp(this,22)));
            choice.setSelected(selected);choice.setContentDescription(palette.name+(selected?"，当前主题":"，点击切换"));choice.setFocusable(true);choice.setOnClickListener(v->select(palette));
            content.addView(choice);Ui.space(content,12);
        }
        TextView current=Ui.action(this,"已应用"+Ui.theme.name+"主题",true,this::finish);current.setContentDescription("已应用"+Ui.theme.name+"主题，点击返回");content.addView(current);Ui.space(content,12);
        TextView hint=Ui.text(this,"主题仅改变外观，账目与输入内容会保留。",11,Ui.MUTED);hint.setGravity(Gravity.CENTER);content.addView(hint);
    }
    private void select(ThemePalette palette) {
        if(palette==Ui.theme)return;
        if(!getSharedPreferences("appearance",MODE_PRIVATE).edit().putString("theme",palette.id).commit()) {Toast.makeText(this,"主题未保存，请重试",Toast.LENGTH_SHORT).show();return;}
        recreate();
    }
}
