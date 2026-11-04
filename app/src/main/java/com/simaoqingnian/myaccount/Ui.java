package com.simaoqingnian.myaccount;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int PAPER=Color.rgb(247,248,242),GREEN=Color.rgb(23,78,66),INK=Color.rgb(37,59,52),MUTED=Color.rgb(103,118,109),SAGE=Color.rgb(230,237,223),LINE=Color.rgb(230,233,224),DANGER=Color.rgb(176,68,53),WHITE=Color.WHITE;
    public static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    public static int color(String hex){return Color.parseColor(hex);}
    public static GradientDrawable background(int color,int radius,Context c){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    public static GradientDrawable border(int fill,int radius,Context c){GradientDrawable d=background(fill,radius,c);d.setStroke(dp(c,1),LINE);return d;}
    public static void pad(View v,int l,int t,int r,int b){Context c=v.getContext();v.setPadding(dp(c,l),dp(c,t),dp(c,r),dp(c,b));}
    public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    public static TextView text(Context c,String text,int size,int color){TextView t=new TextView(c);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setIncludeFontPadding(false);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    public static TextView bold(Context c,String text,int size,int color){TextView t=text(c,text,size,color);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    public static TextView action(Context c,String text,boolean primary,Runnable click){TextView t=bold(c,text,14,primary?WHITE:GREEN);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(c,48));pad(t,14,10,14,10);t.setBackground(new RippleDrawable(ColorStateList.valueOf(0x1A174E42),background(primary?GREEN:SAGE,13,c),null));t.setOnClickListener(v->click.run());t.setFocusable(true);return t;}
    public static TextView smallAction(Context c,String label,Runnable click){TextView t=action(c,label,false,click);t.setTextSize(12);t.setBackgroundColor(Color.TRANSPARENT);pad(t,6,6,6,6);return t;}
    public static EditText input(Context c,String hint){EditText e=new EditText(c);e.setSingleLine(true);e.setTextSize(15);e.setTextColor(INK);e.setHintTextColor(MUTED);e.setHint(hint);e.setBackground(border(WHITE,12,c));pad(e,13,10,13,10);e.setMinHeight(dp(c,48));return e;}
    public static LinearLayout card(Context c,int color){LinearLayout l=column(c);l.setBackground(background(color,20,c));pad(l,18,18,18,18);return l;}
    public static void space(LinearLayout l,int height){View v=new View(l.getContext());l.addView(v,new LinearLayout.LayoutParams(1,dp(l.getContext(),height)));}
    public static void divider(LinearLayout l){View v=new View(l.getContext());v.setBackgroundColor(LINE);l.addView(v,new LinearLayout.LayoutParams(-1,dp(l.getContext(),1)));}
    public static void weight(LinearLayout row,View v){row.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
    public static void insets(Activity activity,View root){
        activity.getWindow().setStatusBarColor(PAPER);activity.getWindow().setNavigationBarColor(Build.VERSION.SDK_INT>=27?PAPER:GREEN);
        int flags=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if(Build.VERSION.SDK_INT>=27)flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
        if(Build.VERSION.SDK_INT>=30)activity.getWindow().setDecorFitsSystemWindows(false);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets system=insets.getInsets(WindowInsets.Type.systemBars()),ime=insets.getInsets(WindowInsets.Type.ime());v.setPadding(system.left,system.top,system.right,Math.max(system.bottom,ime.bottom));}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });root.requestApplyInsets();
    }
    public static class Icon extends View {
        private final String name;private final int foreground;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path shape=new Path();
        public Icon(Context c){this(c,"leaf",GREEN);}
        public Icon(Context c,String name,int color){super(c);this.name=name;foreground=color;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();float width=getWidth()-getPaddingLeft()-getPaddingRight(),height=getHeight()-getPaddingTop()-getPaddingBottom(),size=Math.min(width,height);c.translate(getPaddingLeft()+(width-size)/2,getPaddingTop()+(height-size)/2);c.scale(size/24f,size/24f);p.setColor(foreground);p.setStrokeWidth(1.65f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            switch(name){
                case "plus":line(c,12,4,12,20);line(c,4,12,20,12);break;
                case "close":line(c,6,6,18,18);line(c,6,18,18,6);break;
                case "back":path(c,15,5,8,12,15,19);break;
                case "arrow":path(c,9,5,16,12,9,19);break;
                case "down":path(c,6,9,12,15,18,9);break;
                case "check":path(c,5,12,9,16,19,6);break;
                case "home":path(c,2,10,12,3,22,10);path(c,5,9,5,21,10,21,10,15,14,15,14,21,19,21,19,9);break;
                case "food":line(c,5,3,5,21);line(c,2,3,2,8);line(c,8,3,8,8);c.drawArc(2,5,8,11,0,180,false,p);path(c,17,3,14,8,14,12,18,12);line(c,18,3,18,21);break;
                case "bus":rect(c,4,4,20,18,3);line(c,4,10,20,10);line(c,7,18,7,21);line(c,17,18,17,21);c.drawCircle(8,14,1,p);c.drawCircle(16,14,1,p);break;
                case "bag":rect(c,4,7,20,21,2);c.drawArc(8,1,16,13,180,180,false,p);break;
                case "basket":path(c,3,9,21,9,18,21,6,21,3,9);line(c,7,9,10,3);line(c,17,9,14,3);line(c,9,13,9,17);line(c,15,13,15,17);break;
                case "bolt":path(c,14,2,5,14,12,14,10,22,19,9,12,9,14,2);break;
                case "book":path(c,12,5,7,3,2,4,2,20,7,19,12,21,17,19,22,20,22,4,17,3,12,5,12,21);break;
                case "heart":{shape.reset();shape.moveTo(12,21);shape.cubicTo(-5,10,2,-2,12,6);shape.cubicTo(22,-2,29,10,12,21);c.drawPath(shape,p);break;}
                case "plane":path(c,22,2,15,22,11,13,2,9,22,2,11,13);break;
                case "gift":rect(c,3,8,21,12,1);rect(c,5,12,19,21,1);line(c,12,8,12,21);c.drawOval(5,2,12,8,p);c.drawOval(12,2,19,8,p);break;
                case "wallet":rect(c,3,6,21,21,2);path(c,3,7,3,3,18,3);rect(c,14,11,21,16,1);break;
                case "calendar":rect(c,4,5,20,21,2);line(c,4,10,20,10);line(c,8,2,8,7);line(c,16,2,16,7);c.drawCircle(8,14,.4f,p);c.drawCircle(12,14,.4f,p);c.drawCircle(16,14,.4f,p);break;
                case "chart":line(c,4,20,4,10);line(c,10,20,10,4);line(c,16,20,16,12);line(c,2,21,22,21);break;
                case "list":for(int y=6;y<=18;y+=6){line(c,8,y,21,y);c.drawCircle(3,y,.4f,p);}break;
                case "user":c.drawCircle(12,7,4,p);c.drawArc(3,13,21,29,180,180,false,p);break;
                case "search":c.drawCircle(10,10,6,p);line(c,15,15,21,21);break;
                case "download":line(c,12,3,12,15);path(c,7,10,12,15,17,10);path(c,4,15,4,21,20,21,20,15);break;
                case "upload":line(c,12,16,12,4);path(c,7,9,12,4,17,9);path(c,4,15,4,21,20,21,20,15);break;
                case "shield":path(c,12,2,21,6,20,15,12,22,4,15,3,6,12,2);path(c,7,12,10,15,17,8);break;
                case "eye":c.drawOval(2,6,22,18,p);c.drawCircle(12,12,3,p);break;
                case "eyeOff":c.drawOval(2,6,22,18,p);line(c,3,3,21,21);break;
                case "note":rect(c,5,3,19,21,1);line(c,8,8,16,8);line(c,8,12,16,12);line(c,8,16,12,16);break;
                case "target":c.drawCircle(12,12,9,p);c.drawCircle(12,12,5,p);line(c,12,12,22,2);break;
                case "game":rect(c,2,7,22,19,5);line(c,6,12,10,12);line(c,8,10,8,14);c.drawCircle(16,11,.5f,p);c.drawCircle(18,14,.5f,p);break;
                case "leaf":{shape.reset();shape.moveTo(21,3);shape.cubicTo(4,1,1,9,5,17);shape.cubicTo(13,21,22,17,21,3);c.drawPath(shape,p);line(c,3,21,15,9);break;}
                default:for(int x=3;x<=15;x+=12)for(int y=3;y<=15;y+=12)rect(c,x,y,x+6,y+6,1);
            }c.restore();
        }
        private void line(Canvas c,float a,float b,float x,float y){c.drawLine(a,b,x,y,p);}
        private void rect(Canvas c,float a,float b,float x,float y,float radius){c.drawRoundRect(a,b,x,y,radius,radius,p);}
        private void path(Canvas c,float... xy){shape.reset();shape.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)shape.lineTo(xy[i],xy[i+1]);c.drawPath(shape,p);}
    }
}
