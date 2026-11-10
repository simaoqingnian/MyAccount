package com.simaoqingnian.myaccount;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;
import java.util.function.IntConsumer;

public final class TrendChart extends View {
    private final Ledger.Report report;private final boolean hide;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path line=new Path(),area=new Path();private int selected;private final IntConsumer listener;
    public TrendChart(Context context){this(context,Ledger.report(Ledger.Data.empty(),"month",Ledger.today(),"expense"),false,0,i->{});}
    public TrendChart(Context context,Ledger.Report report,boolean hide,int selected,IntConsumer listener){super(context);this.report=report;this.hide=hide;this.selected=selected;this.listener=listener;setMinimumHeight(Ui.dp(context,174));setContentDescription("收支趋势。下方可按日期选择并查看精确金额。");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    private float x(int i){float a=Ui.dp(getContext(),39),b=getWidth()-Ui.dp(getContext(),12);return a+(b-a)*i/(report.buckets.size()-1);}
    private long peak(){long p=100;for(Ledger.Bucket b:report.buckets)p=Math.max(p,b.total);return p;}
    private float y(long value){return Ui.dp(getContext(),135)-(float)value/peak()*Ui.dp(getContext(),114);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float left=x(0),right=x(report.buckets.size()-1),bottom=y(0);paint.setTextSize(Ui.dp(getContext(),9));
        for(int i=0;i<3;i++){long amount=peak()*i/2;float yy=y(amount);paint.setColor(Ui.LINE);paint.setStrokeWidth(Ui.dp(getContext(),1));c.drawLine(left,yy,right,yy,paint);paint.setColor(Ui.MUTED);paint.setTextAlign(Paint.Align.RIGHT);String label=hide?"••":amount>=1_000_000?String.format(java.util.Locale.US,"%.1f万",amount/1_000_000d):String.valueOf(amount/100);c.drawText(label,left-Ui.dp(getContext(),5),yy+Ui.dp(getContext(),3),paint);}
        line.reset();area.reset();boolean begun=false;float lastX=left;
        for(int i=0;i<report.buckets.size();i++){Ledger.Bucket b=report.buckets.get(i);if(b.future)continue;float xx=x(i),yy=y(b.total);if(!begun){line.moveTo(xx,yy);area.moveTo(xx,bottom);area.lineTo(xx,yy);begun=true;}else{line.lineTo(xx,yy);area.lineTo(xx,yy);}lastX=xx;}
        area.lineTo(lastX,bottom);area.close();paint.setStyle(Paint.Style.FILL);paint.setColor((Ui.CHART&0xFFFFFF)|0x30000000);c.drawPath(area,paint);paint.setStyle(Paint.Style.STROKE);paint.setColor(Ui.CHART);paint.setStrokeWidth(Ui.dp(getContext(),1.7f));c.drawPath(line,paint);paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<report.buckets.size();i++){Ledger.Bucket b=report.buckets.get(i);float xx=x(i);if(!b.future){paint.setColor(Ui.CHART);c.drawCircle(xx,y(b.total),Ui.dp(getContext(),i==selected?4:2.5f),paint);if(i!=selected){paint.setColor(Ui.WHITE);c.drawCircle(xx,y(b.total),Ui.dp(getContext(),1.2f),paint);}}
            boolean show=!report.period.equals("month")||i==0||(i+1)%5==0||i==report.buckets.size()-1;if(show){paint.setTextAlign(Paint.Align.CENTER);paint.setColor(Ui.MUTED);c.drawText(b.label,xx,Ui.dp(getContext(),156),paint);}if(b.future){paint.setColor(Ui.MUTED);c.drawText("—",xx,bottom,paint);}}
    }
    @Override public boolean onTouchEvent(MotionEvent event){if(event.getAction()==MotionEvent.ACTION_UP){int best=0;float distance=Float.MAX_VALUE;for(int i=0;i<report.buckets.size();i++)if(!report.buckets.get(i).future&&Math.abs(x(i)-event.getX())<distance){best=i;distance=Math.abs(x(i)-event.getX());}selected=best;invalidate();listener.accept(best);performClick();return true;}return true;}
    @Override public boolean performClick(){return super.performClick();}
}
