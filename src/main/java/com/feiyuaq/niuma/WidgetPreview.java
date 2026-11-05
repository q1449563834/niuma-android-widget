package com.feiyuaq.niuma;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * 设置页里的小部件实时预览：和小部件本身用的是同一个渲染器。
 * 2 列宽的尺寸（1×2 / 2×2）只画右边的半幅，这样预览在设置页里的形状和桌面一致，
 * 不用依赖外层布局的 weight。
 */
public class WidgetPreview extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int cols = 4;
    private int rows = 1;
    private Prefs prefs;
    private Engine.Snapshot snap;

    public WidgetPreview(Context c) {
        super(c);
    }

    public void config(int cols, int rows, Prefs p, Engine.Snapshot s) {
        this.cols = cols;
        this.rows = rows;
        this.prefs = p;
        this.snap = s;
        invalidate();
        requestLayout();
    }

    public void update(Prefs p, Engine.Snapshot s) {
        this.prefs = p;
        this.snap = s;
        invalidate();
    }

    /** 2 列尺寸占 52% 宽度，4 列占满 */
    private float cardWidth() {
        float w = getWidth();
        return cols >= 4 ? w : w * 0.52f;
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        float cw = cols >= 4 ? w : w * 0.52f;
        // 高度按「逻辑宽高比」算，1×2 / 2×2 在设置页里的形状就和桌面上一致
        float ratio = WidgetRenderer.logH(rows) / (float) WidgetRenderer.logW(cols);
        setMeasuredDimension(w, (int) (cw * ratio));
    }

    @Override
    protected void onDraw(Canvas c) {
        if (prefs == null || snap == null) return;
        float w = cardWidth(), h = getHeight();
        float inset = Ui.dp(getContext(), 6);
        float r = Ui.dp(getContext(), 14);

        paint.setShader(new LinearGradient(0, 0, w, h, 0xFFE8E1CB, 0xFFD9CFB2, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(0, 0, w, h), r, r, paint);
        paint.setShader(null);

        c.save();
        c.clipRect(new RectF(inset, inset, w - inset, h - inset));
        c.translate(inset, inset);
        WidgetRenderer.draw(c, (int) (w - 2 * inset), (int) (h - 2 * inset), cols, rows, prefs, snap,
                getResources().getDisplayMetrics().density);
        c.restore();
    }
}
