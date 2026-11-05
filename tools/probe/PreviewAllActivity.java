package com.feiyuaq.niuma;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;

/**
 * 临时调试页（只在调试构建里存在）：按真实格子的 dp 尺寸把四种小部件画一遍，
 * 用来检查 1×2 / 2×2 这种窄卡片里文字有没有溢出、金额有没有被裁、「明日」那行放不放得下。
 *
 * 用法：把本文件拷到 src\main\java\com\feiyuaq\niuma\，Manifest 里加
 * <activity android:name=".PreviewAllActivity" android:exported="true" />
 * 再 am start -n com.feiyuaq.niuma/.PreviewAllActivity，最后 exec-out screencap -p 截图。
 */
public class PreviewAllActivity extends Activity {

    private static final String[] NAMES = {"1×2", "1×4", "2×2", "2×4"};
    // 小米桌面 4 列铺满 1440px 宽、1 行高约 84dp 时的实际尺寸（dp）
    private static final int[][] CELL = {{2, 1}, {4, 1}, {2, 2}, {4, 2}};

    private float d;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = getResources().getDisplayMetrics().density;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF2B3240);

        Prefs p = Prefs.load(this);
        addSection("默认设置 · " + Engine.planText(Engine.planFor(p, Calendar.getInstance())), p);

        // 最宽文案：今天白班 08:00–16:00 + 值班 18:00–22:00，明天中夜 16:00–次日 08:00
        Prefs q = p.copy();
        q.mode = Prefs.MODE_CYCLE;
        q.anchor = D.key(Calendar.getInstance());
        q.customCycle = "W,480,960,-1,-1,1080,1320|W,960,1920,-1,-1|R|R";
        addSection("最宽文案 · 今天 " + Engine.planText(Engine.planFor(q, Calendar.getInstance()))
                + " / 明天 " + Engine.planText(Engine.planFor(q, D.at(Calendar.getInstance(), 1))), q);

        ScrollView sv = new ScrollView(this);
        sv.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(sv);
    }

    private void addSection(String title, Prefs p) {
        int pad = (int) (12 * d);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(0xFFFFD08A);
        t.setTextSize(12);
        t.setPadding(pad, pad * 2, pad, pad);
        root.addView(t);

        Engine.Snapshot s = Engine.compute(p, System.currentTimeMillis());
        for (int i = 0; i < CELL.length; i++) {
            int cols = CELL[i][0], rows = CELL[i][1];
            int wDp = cols == 2 ? 186 : 372;
            int hDp = rows == 1 ? 82 : 168;
            int wPx = (int) (wDp * d), hPx = (int) (hDp * d);

            TextView label = new TextView(this);
            label.setText(NAMES[i] + "   " + String.format(java.util.Locale.US, "%,.4f", s.earned)
                    + "   " + s.stateText + "   已放置: " + widgetCount(cols, rows) + " 个");
            label.setTextColor(Color.WHITE);
            label.setTextSize(11);
            label.setPadding(pad, pad, pad, pad);

            Bitmap bmp = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            c.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
            WidgetRenderer.draw(c, wPx, hPx, cols, rows, p, s, d);

            ImageView iv = new ImageView(this);
            iv.setImageBitmap(bmp);
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(wPx, hPx);
            lp.leftMargin = pad;
            root.addView(label);
            root.addView(iv, lp);
        }
    }

    private int widgetCount(int cols, int rows) {
        Class<?> cls = cols == 2 ? (rows == 1 ? NiumaWidget1x2.class : NiumaWidget2x2.class)
                : (rows == 1 ? NiumaWidget1x4.class : NiumaWidget2x4.class);
        return WidgetUpdater.ids(this, cls).length;
    }
}
