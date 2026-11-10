package com.simaoqingnian.myaccount;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
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
    /** A row owns only its bottom separator, exactly one physical pixel thick. */
    public static void listItem(View row,int fill){row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x1A174E42),new ListItemBackground(fill),null));}
    private static final class ListItemBackground extends Drawable {
        private final int fill;private final Paint paint=new Paint();private int alpha=255;
        ListItemBackground(int fill){this.fill=fill;}
        @Override public void draw(Canvas canvas){
            android.graphics.Rect bounds=getBounds();paint.setColor(fill);paint.setAlpha(Color.alpha(fill)*alpha/255);canvas.drawRect(bounds,paint);
            paint.setColor(LINE);paint.setAlpha(alpha);canvas.drawRect(bounds.left,bounds.bottom-1,bounds.right,bounds.bottom,paint);
        }
        @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return alpha==255&&Color.alpha(fill)==255?PixelFormat.OPAQUE:PixelFormat.TRANSLUCENT;}
    }
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
    public static void insets(Activity activity,View root){insets(activity,root,null);}
    public static void insets(Activity activity,View root,java.util.function.Consumer<Boolean> imeVisibility){
        activity.getWindow().setStatusBarColor(PAPER);activity.getWindow().setNavigationBarColor(Build.VERSION.SDK_INT>=27?PAPER:GREEN);
        int flags=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if(Build.VERSION.SDK_INT>=27)flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
        if(Build.VERSION.SDK_INT>=30)activity.getWindow().setDecorFitsSystemWindows(false);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets system=insets.getInsets(WindowInsets.Type.systemBars()),ime=insets.getInsets(WindowInsets.Type.ime());v.setPadding(system.left,system.top,system.right,Math.max(system.bottom,ime.bottom));if(imeVisibility!=null)imeVisibility.accept(insets.isVisible(WindowInsets.Type.ime()));}
            else {v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());if(imeVisibility!=null)imeVisibility.accept(legacyImeVisible(root,insets));}
            return insets;
        });
        if(Build.VERSION.SDK_INT<30&&imeVisibility!=null){
            android.view.ViewTreeObserver.OnGlobalLayoutListener layout=()->{if(root.isAttachedToWindow())imeVisibility.accept(legacyImeVisible(root,root.getRootWindowInsets()));};
            root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
                @Override public void onViewAttachedToWindow(View v){v.getViewTreeObserver().addOnGlobalLayoutListener(layout);}
                @Override public void onViewDetachedFromWindow(View v){if(v.getViewTreeObserver().isAlive())v.getViewTreeObserver().removeOnGlobalLayoutListener(layout);}
            });
            if(root.isAttachedToWindow())root.getViewTreeObserver().addOnGlobalLayoutListener(layout);
        }
        root.requestApplyInsets();
    }
    private static boolean legacyImeVisible(View root,WindowInsets insets){
        int minimum=dp(root.getContext(),100);
        if(insets!=null&&insets.getSystemWindowInsetBottom()>Math.max(minimum,insets.getStableInsetBottom()))return true;
        android.graphics.Rect visible=new android.graphics.Rect();root.getWindowVisibleDisplayFrame(visible);
        return root.getRootView().getHeight()-visible.bottom>minimum;
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
                case "coffee":path(c,4,8,4,17,7,20,14,20,17,17,17,8,4,8);c.drawArc(14,8,23,16,-90,180,false,p);line(c,7,2,7,5);line(c,12,2,12,5);line(c,2,22,20,22);break;
                case "fruit":c.drawOval(4,7,20,22,p);path(c,12,8,12,3,16,1);c.drawOval(4,1,12,6,p);break;
                case "cookie":c.drawCircle(12,12,9,p);for(int x=7;x<=17;x+=5)c.drawCircle(x,x==12?7:13,.8f,p);c.drawCircle(12,18,.8f,p);break;
                case "vegetable":path(c,8,7,17,12,4,23,8,7);path(c,11,8,12,2,14,7,19,3,18,9,23,8,17,12);line(c,8,12,11,14);break;
                case "bowl":c.drawArc(3,4,21,20,0,180,false,p);line(c,3,12,21,12);line(c,8,21,16,21);line(c,7,3,7,7);line(c,12,2,12,6);line(c,17,3,17,7);break;
                case "takeout":path(c,5,8,19,8,17,21,7,21,5,8,3,5,21,5,19,8);path(c,8,5,10,2,14,2,16,5);break;
                case "shirt":path(c,8,3,3,5,1,11,6,13,6,22,18,22,18,13,23,11,21,5,16,3);c.drawArc(8,0,16,8,0,180,false,p);break;
                case "lipstick":rect(c,7,12,17,22,1);path(c,9,12,9,5,15,2,15,12);line(c,7,17,17,17);break;
                case "scissors":c.drawCircle(6,17,4,p);c.drawCircle(18,17,4,p);line(c,8,14,20,2);line(c,16,14,4,2);break;
                case "phone":rect(c,6,1,18,23,2);line(c,6,18,18,18);line(c,10,4,14,4);c.drawCircle(12,21,.4f,p);break;
                case "wifi":c.drawArc(1,3,23,25,225,90,false,p);c.drawArc(5,7,19,21,225,90,false,p);c.drawArc(9,11,15,17,225,90,false,p);c.drawCircle(12,20,1,p);break;
                case "subscription":rect(c,3,4,21,21,2);line(c,3,9,21,9);line(c,8,2,8,6);line(c,16,2,16,6);path(c,8,14,11,17,17,12);break;
                case "laptop":rect(c,4,3,20,17,1);path(c,4,17,1,21,23,21,20,17);line(c,10,19,14,19);break;
                case "car":path(c,3,11,6,4,18,4,21,11);rect(c,2,11,22,19,2);line(c,6,19,6,22);line(c,18,19,18,22);line(c,5,15,7,15);line(c,17,15,19,15);break;
                case "fuel":rect(c,3,3,14,22,1);rect(c,5,5,12,11,0);path(c,14,14,17,14,17,19,21,19,21,8,18,5);line(c,2,22,16,22);break;
                case "parking":rect(c,3,3,21,21,3);path(c,9,18,9,7,14,7,16,9,16,11,14,13,9,13);break;
                case "taxi":path(c,3,12,6,6,18,6,21,12);rect(c,2,12,22,20,2);rect(c,9,2,15,6,1);line(c,6,20,6,23);line(c,18,20,18,23);line(c,5,16,8,16);line(c,16,16,19,16);break;
                case "paw":c.drawOval(7,11,17,22,p);c.drawOval(2,6,7,12,p);c.drawOval(7,1,11,8,p);c.drawOval(13,1,17,8,p);c.drawOval(18,6,23,12,p);break;
                case "baby":c.drawCircle(12,13,9,p);c.drawArc(9,0,16,8,0,240,false,p);c.drawCircle(9,12,.5f,p);c.drawCircle(15,12,.5f,p);c.drawArc(8,12,16,19,0,180,false,p);break;
                case "ball":c.drawCircle(12,12,10,p);line(c,2,12,22,12);line(c,12,2,12,22);c.drawArc(-4,2,12,22,-80,160,false,p);c.drawArc(12,2,28,22,100,160,false,p);break;
                case "dumbbell":rect(c,3,6,7,18,1);rect(c,17,6,21,18,1);line(c,7,12,17,12);line(c,1,9,1,15);line(c,23,9,23,15);break;
                case "bank":path(c,2,8,12,2,22,8,2,8);line(c,4,11,4,19);line(c,9,11,9,19);line(c,15,11,15,19);line(c,20,11,20,19);line(c,2,22,22,22);break;
                case "tools":path(c,16,2,14,7,17,10,22,8,21,13,16,15,7,23,2,18,11,10,10,5,16,2);break;
                case "handHeart":path(c,2,16,7,16,10,13,14,13,16,15,10,18,18,18,22,15,22,19,16,23,8,23,5,20,2,20);path(c,8,5,10,2,13,4,16,2,19,5,13,11,8,5);break;
                case "receipt":path(c,5,1,8,3,11,1,14,3,17,1,20,3,20,23,17,21,14,23,11,21,8,23,5,21,5,1);line(c,8,8,17,8);line(c,8,12,17,12);line(c,8,16,14,16);break;
                case "package":path(c,2,7,12,2,22,7,22,18,12,23,2,18,2,7,12,12,22,7);line(c,12,12,12,23);path(c,7,4,17,9,17,14);break;
                case "refund":rect(c,3,9,21,22,2);path(c,16,3,9,3,5,7,1,3);line(c,5,7,5,1);c.drawCircle(12,15,3,p);break;
                case "percent":line(c,5,21,19,3);c.drawCircle(6,6,3,p);c.drawCircle(18,18,3,p);break;
                case "coins":c.drawOval(3,3,17,9,p);path(c,3,6,3,16,6,19,11,19);path(c,17,6,17,9);c.drawCircle(17,17,6,p);line(c,17,14,17,20);break;
                case "recycle":path(c,8,3,15,3,20,10,16,10,22,13,23,7,20,10);path(c,20,17,17,22,8,22,10,18,4,22,7,23);path(c,3,17,1,12,5,6,7,10,7,3,1,5);break;
                case "redpacket":rect(c,4,2,20,22,2);path(c,4,5,12,11,20,5);c.drawCircle(12,15,3,p);break;
                case "briefcase":rect(c,2,7,22,21,2);path(c,8,7,8,2,16,2,16,7);line(c,2,13,22,13);rect(c,10,11,14,15,0);break;
                case "pension":path(c,2,11,12,3,22,11);rect(c,5,10,19,23,1);line(c,8,15,16,15);line(c,12,12,12,19);break;
                case "ticket":path(c,2,5,22,5,22,10,19,12,22,14,22,19,2,19,2,14,5,12,2,10,2,5);line(c,15,7,15,9);line(c,15,11,15,13);line(c,15,15,15,17);break;
                case "flower":c.drawCircle(12,9,3,p);c.drawOval(9,0,15,6,p);c.drawOval(16,5,22,11,p);c.drawOval(13,12,19,18,p);c.drawOval(5,12,11,18,p);c.drawOval(2,5,8,11,p);line(c,12,16,12,24);break;
                case "cake":rect(c,3,11,21,22,1);path(c,3,15,6,18,9,15,12,18,15,15,18,18,21,15);line(c,8,7,8,11);line(c,16,7,16,11);path(c,8,2,6,5,8,7,10,5,8,2);path(c,16,2,14,5,16,7,18,5,16,2);break;
                case "music":path(c,8,19,8,5,21,2,21,16);line(c,8,9,21,6);c.drawOval(2,16,8,22,p);c.drawOval(15,13,21,19,p);break;
                case "camera":rect(c,2,6,22,21,2);path(c,7,6,9,2,15,2,17,6);c.drawCircle(12,13,5,p);c.drawCircle(19,9,.4f,p);break;
                case "watch":rect(c,7,6,17,18,3);path(c,8,6,9,1,15,1,16,6);path(c,8,18,9,23,15,23,16,18);path(c,12,8,12,12,15,14);break;
                case "train":rect(c,5,2,19,20,4);rect(c,7,5,17,12,1);line(c,12,5,12,12);line(c,8,20,5,23);line(c,16,20,19,23);c.drawCircle(9,16,.8f,p);c.drawCircle(15,16,.8f,p);break;
                case "bike":c.drawCircle(5,17,4,p);c.drawCircle(19,17,4,p);path(c,5,17,9,8,14,17,5,17);path(c,9,8,17,8,19,17);path(c,17,8,16,3,20,3);line(c,7,6,11,6);break;
                case "ship":path(c,2,13,12,9,22,13,19,20,5,20,2,13);path(c,7,11,7,4,17,4,17,11);line(c,12,1,12,4);path(c,1,23,5,21,9,23,13,21,17,23,22,21);break;
                case "umbrella":c.drawArc(2,2,22,22,180,180,false,p);line(c,2,12,22,12);path(c,12,12,12,21,15,23,18,21);line(c,12,0,12,2);break;
                case "sun":c.drawCircle(12,12,5,p);for(int a=0;a<8;a++){double r=a*Math.PI/4;line(c,12+(float)Math.cos(r)*8,12+(float)Math.sin(r)*8,12+(float)Math.cos(r)*11,12+(float)Math.sin(r)*11);}break;
                case "moon":shape.reset();shape.moveTo(17,2);shape.cubicTo(5,0,0,11,6,19);shape.cubicTo(12,26,22,20,23,14);shape.cubicTo(14,18,9,9,17,2);c.drawPath(shape,p);break;
                case "star":path(c,12,1,15,8,23,9,17,14,19,22,12,18,5,22,7,14,1,9,9,8,12,1);break;
                case "diamond":path(c,2,8,7,2,17,2,22,8,12,23,2,8,22,8);path(c,7,2,8,8,12,23,16,8,17,2);break;
                case "ring":c.drawCircle(12,15,7,p);path(c,8,3,16,3,18,6,12,10,6,6,8,3);break;
                case "brush":path(c,9,15,19,2,23,6,12,17);shape.reset();shape.moveTo(12,17);shape.cubicTo(10,10,3,14,5,18);shape.cubicTo(6,21,2,23,1,22);shape.cubicTo(8,25,14,23,12,17);c.drawPath(shape,p);break;
                case "bed":line(c,2,5,2,23);line(c,22,12,22,23);path(c,2,19,22,19,22,12,2,12);rect(c,4,7,10,12,1);line(c,12,9,20,9);break;
                case "sofa":rect(c,5,5,19,14,3);rect(c,2,11,7,20,2);rect(c,17,11,22,20,2);line(c,7,17,17,17);line(c,5,20,5,23);line(c,19,20,19,23);break;
                case "laundry":rect(c,4,1,20,23,2);line(c,4,6,20,6);c.drawCircle(12,14,6,p);c.drawArc(6,10,18,18,0,180,false,p);c.drawCircle(7,4,.4f,p);break;
                case "water":shape.reset();shape.moveTo(12,1);shape.cubicTo(9,7,3,11,4,17);shape.cubicTo(5,25,20,25,20,16);shape.cubicTo(20,11,15,6,12,1);c.drawPath(shape,p);c.drawArc(7,12,16,20,60,100,false,p);break;
                case "tree":path(c,12,1,4,9,8,9,2,17,10,17,10,23,14,23,14,17,22,17,16,9,20,9,12,1);break;
                case "building":rect(c,5,1,19,23,1);rect(c,9,18,15,23,0);for(int x=8;x<=16;x+=4)for(int y=5;y<=13;y+=4)line(c,x,y,x+1,y);break;
                case "key":c.drawCircle(7,7,5,p);path(c,10,11,21,22,23,20,20,17,18,19);c.drawCircle(6,6,.5f,p);break;
                case "medical":rect(c,2,6,22,22,2);path(c,8,6,8,2,16,2,16,6);line(c,12,10,12,18);line(c,8,14,16,14);break;
                case "globe":c.drawCircle(12,12,10,p);c.drawOval(7,2,17,22,p);line(c,2,12,22,12);c.drawArc(3,1,21,15,0,180,false,p);c.drawArc(3,9,21,23,180,180,false,p);break;
                case "grid":for(int x=3;x<=15;x+=12)for(int y=3;y<=15;y+=12)rect(c,x,y,x+6,y+6,1);break;
                default:for(int x=3;x<=15;x+=12)for(int y=3;y<=15;y+=12)rect(c,x,y,x+6,y+6,1);
            }c.restore();
        }
        private void line(Canvas c,float a,float b,float x,float y){c.drawLine(a,b,x,y,p);}
        private void rect(Canvas c,float a,float b,float x,float y,float radius){c.drawRoundRect(a,b,x,y,radius,radius,p);}
        private void path(Canvas c,float... xy){shape.reset();shape.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)shape.lineTo(xy[i],xy[i+1]);c.drawPath(shape,p);}
    }
}
