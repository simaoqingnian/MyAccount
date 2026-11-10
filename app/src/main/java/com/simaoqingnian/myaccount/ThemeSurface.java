package com.simaoqingnian.myaccount;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Small shared material swatches, scaled within real, editable native components. */
public final class ThemeSurface extends Drawable {
    public static final int CARD=0, PRIMARY=1, TILE=2, NUMBER=3, SELECTED_TILE=4;
    private static Bitmap silver,gold;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds=new RectF();
    private final float radius,density;
    private final int fill,edge;
    private final BitmapShader shader;
    private final Bitmap texture;
    private int alpha=255;

    public ThemeSurface(Context context,ThemePalette theme,int kind,float radiusDp) {
        density=context.getResources().getDisplayMetrics().density;
        radius=radiusDp*density;
        boolean primary=kind==PRIMARY||kind==SELECTED_TILE;
        fill=kind==CARD?theme.card:primary?(theme==ThemePalette.PEACH?0xFFF5C4B4:theme.accent):kind==TILE?theme.tile:theme.surface;
        edge=kind==SELECTED_TILE?theme.accent:kind==CARD&&theme==ThemePalette.PEACH?0xFFF3CFBB:theme.line;
        if(theme==ThemePalette.TITANIUM) {
            if(silver==null) silver=BitmapFactory.decodeResource(context.getResources(),R.drawable.material_titanium);
            if(gold==null) gold=BitmapFactory.decodeResource(context.getResources(),R.drawable.material_champagne);
            texture=primary?gold:silver;
            shader=new BitmapShader(texture,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);
        } else {texture=null;shader=null;}
    }
    @Override protected void onBoundsChange(Rect value) {
        bounds.set(value);bounds.inset(.5f*density,.5f*density);
        if(shader!=null) {Matrix matrix=new Matrix();matrix.setScale(value.width()/(float)texture.getWidth(),value.height()/(float)texture.getHeight());matrix.postTranslate(value.left,value.top);shader.setLocalMatrix(matrix);}
    }
    @Override public void draw(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);paint.setShader(shader);paint.setColor(fill);paint.setAlpha(alpha);canvas.drawRoundRect(bounds,radius,radius,paint);
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density);paint.setColor(edge);paint.setAlpha(alpha);canvas.drawRoundRect(bounds,radius,radius,paint);
        if(shader!=null){paint.setColor(0xD9FFFFFF);paint.setAlpha(alpha*3/4);canvas.drawLine(bounds.left+radius,bounds.top+density,bounds.right-radius,bounds.top+density,paint);}
    }
    @Override public void getOutline(Outline outline){outline.setRoundRect(getBounds(),radius);outline.setAlpha(alpha/255f);}
    @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
