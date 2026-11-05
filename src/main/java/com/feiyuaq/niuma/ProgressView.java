package com.feiyuaq.niuma;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** 工时进度条：绿到橙的渐变，跟原项目一致 */
public class ProgressView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress = 0;

    public ProgressView(Context c) {
        super(c);
    }

    public void setProgress(float v) {
        progress = Math.max(0, Math.min(1, v));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float h = getHeight(), w = getWidth();
        float r = h / 2f;
        paint.setShader(null);
        paint.setColor(0x22000000);
        c.drawRoundRect(new RectF(0, 0, w, h), r, r, paint);
        float fw = w * progress;
        if (fw > r * 2) {
            paint.setAlpha(255);
            paint.setShader(new LinearGradient(0, 0, w, 0, 0xFF5E9A3A, Ui.CARROT, Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(0, 0, fw, h), r, r, paint);
            paint.setShader(null);
        } else if (fw > 0) {
            paint.setAlpha(255);
            paint.setColor(0xFF5E9A3A);
            c.drawRoundRect(new RectF(0, 0, Math.max(fw, r), h), r, r, paint);
        }
    }
}
