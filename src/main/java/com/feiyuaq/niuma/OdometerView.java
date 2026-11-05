package com.feiyuaq.niuma;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** 应用内的滚轮金额：和小部件同一套数字绘制算法，60fps 连续滚动 */
public class OdometerView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private double value = 0;
    private double speed = 0;
    private int decimals = 5;
    private String currency = "¥";
    private boolean rolling = true;

    public OdometerView(Context c) {
        super(c);
        paint.setSubpixelText(true);
    }

    public OdometerView(Context c, AttributeSet a) {
        super(c, a);
    }

    public void setMoney(double v, double spd, int dec, String cur, boolean roll) {
        this.value = Math.max(0, v);
        this.speed = spd;
        this.decimals = dec;
        this.currency = cur;
        this.rolling = roll;
        invalidate();
    }

    public void setMasked(boolean m) {
        masked = m;
        invalidate();
    }

    private boolean masked = false;

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float h = getHeight(), w = getWidth();
        float padL = Ui.dp(getContext(), 2);
        if (masked) {
            paint.setTextSize(h * 0.52f);
            paint.setTypeface(WidgetRenderer.NUM);
            paint.setColor(Ui.CARROT);
            paint.setTextAlign(Paint.Align.LEFT);
            c.drawText(currency + " ****.**", padL, h * 0.68f, paint);
            return;
        }
        long ip = (long) Math.floor(value);
        int intDigits = Math.max(1, String.valueOf(ip).length());
        int n = intDigits + decimals;
        float curSize = h * 0.42f;
        paint.setTextSize(curSize);
        paint.setTypeface(WidgetRenderer.CN);
        paint.setColor(Ui.SUB);
        paint.setTextAlign(Paint.Align.LEFT);
        float curW = paint.measureText(currency) + Ui.dp(getContext(), 2);

        // 让整段数字正好塞进宽度：等宽字体一位约 0.6 字号宽
        float avail = w - padL - curW;
        float byWidth = avail / (n * 0.6f + 0.28f);
        float size = Math.min(h * 0.60f, byWidth);
        float baseline = h * 0.70f;

        paint.setTextSize(curSize);
        paint.setTypeface(WidgetRenderer.CN);
        paint.setColor(Ui.SUB);
        c.drawText(currency, padL, baseline, paint);

        if (!rolling) {
            paint.setTextSize(size);
            paint.setTypeface(WidgetRenderer.NUM);
            paint.setColor(Ui.CARROT);
            paint.setTextAlign(Paint.Align.LEFT);
            c.drawText(String.format(java.util.Locale.US, "%." + decimals + "f", value),
                    padL + curW, baseline, paint);
            return;
        }
        WidgetRenderer.drawMoney(c, paint, value, decimals, speed, padL + curW, baseline, size);
    }
}
