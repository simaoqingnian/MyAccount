package com.simaoqingnian.myaccount;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;

/** A miniature uses the same native material drawable as the real overview. */
public final class ThemePreview extends View {
    private final ThemePalette theme;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final ThemeSurface card;
    private final Bitmap peach;
    private final Rect peachBounds=new Rect(78,30,97,45);
    private static final Typeface REGULAR=Typeface.create("sans-serif",Typeface.NORMAL),BOLD=Typeface.create("sans-serif",Typeface.BOLD);
    public ThemePreview(Context context){this(context,ThemePalette.TITANIUM);}
    public ThemePreview(Context context,ThemePalette theme) {
        super(context);this.theme=theme;
        card=new ThemeSurface(context,theme,ThemeSurface.CARD,8/context.getResources().getDisplayMetrics().density);
        peach=theme==ThemePalette.PEACH?BitmapFactory.decodeResource(getResources(),R.drawable.peach_mascot):null;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/110f,getHeight()/150f);
        paint.setStyle(Paint.Style.FILL);paint.setColor(theme.paper);canvas.drawRoundRect(0,0,110,150,12,12,paint);
        text(canvas,"禾记",9,16,9,theme.ink,true);text(canvas,"2026年11月",53,16,6,theme.muted,false);
        card.setBounds(7,26,103,83);card.draw(canvas);
        text(canvas,"本月结余",13,40,6,theme.cardMuted,false);text(canvas,"¥9,690.00",13,62,15,theme.cardInk,true);
        if(peach!=null)canvas.drawBitmap(peach,null,peachBounds,paint);
        text(canvas,"收入",9,99,6,theme.muted,false);text(canvas,"支出",64,99,6,theme.muted,false);
        text(canvas,"¥12,000",9,111,8,theme.ink,true);text(canvas,"¥2,310",64,111,8,theme.ink,true);
        text(canvas,"本月预算",9,128,6,theme.muted,false);paint.setColor(theme.soft);canvas.drawRoundRect(9,135,101,139,2,2,paint);paint.setColor(theme.chart);canvas.drawRoundRect(9,135,52,139,2,2,paint);canvas.restore();
    }
    private void text(Canvas canvas,String value,float x,float y,float size,int color,boolean bold){paint.setColor(color);paint.setTextSize(size);paint.setTypeface(bold?BOLD:REGULAR);canvas.drawText(value,x,y,paint);}
}
