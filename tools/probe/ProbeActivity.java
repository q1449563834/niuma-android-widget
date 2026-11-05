package com.feiyuaq.niuma;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

/** 临时校验页：① 真实 RemoteViews 渲染；② 用 AppWidgetHost 真绑定一个 2×4，验证秒级刷新 */
public class ProbeActivity extends Activity {

    private AppWidgetHost host;
    private int wid = -1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        float d = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1C18);
        root.setPadding((int) (12 * d), (int) (40 * d), (int) (12 * d), (int) (12 * d));

        Prefs p = Prefs.load(this);
        Engine.Snapshot snap = Engine.compute(p, System.currentTimeMillis());

        root.addView(caption("1×4 真实 RemoteViews"));
        root.addView(hostBox(cells(1, p, snap, (int) (328 * d), (int) (84 * d), d), (int) (84 * d)));

        root.addView(caption("2×4 真实 RemoteViews"));
        root.addView(hostBox(cells(2, p, snap, (int) (328 * d), (int) (168 * d), d), (int) (168 * d)));

        root.addView(caption("AppWidgetHost 真绑定（秒级刷新）"));
        root.addView(realHost((int) (328 * d), (int) (168 * d)));

        setContentView(root);
    }

    private TextView caption(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(0xFFE8E2CF);
        t.setTextSize(13);
        t.setPadding(0, (int) (10 * getResources().getDisplayMetrics().density), 0,
                (int) (6 * getResources().getDisplayMetrics().density));
        return t;
    }

    private FrameLayout hostBox(RemoteViews rv, int h) {
        FrameLayout box = new FrameLayout(this);
        box.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));
        View v = rv.apply(this, box);
        box.addView(v);
        return box;
    }

    private FrameLayout realHost(int w, int h) {
        FrameLayout box = new FrameLayout(this);
        box.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(this);
            host = new AppWidgetHost(this, 1024);
            host.startListening();
            wid = host.allocateAppWidgetId();
            boolean ok = m.bindAppWidgetIdIfAllowed(wid, new ComponentName(this, NiumaWidget2x4.class));
            if (!ok) {
                TextView t = new TextView(this);
                t.setText("绑定失败：没有 BIND_APPWIDGET 权限");
                t.setTextColor(0xFFFF8A65);
                box.addView(t);
                return box;
            }
            AppWidgetHostView v = host.createView(this, wid, m.getAppWidgetInfo(wid));
            v.setAppWidget(wid, m.getAppWidgetInfo(wid));
            box.addView(v, new FrameLayout.LayoutParams(w, h));
            WidgetUpdater.updateAll(this);
            TickerService.ensure(this);
        } catch (Exception e) {
            TextView t = new TextView(this);
            t.setText("异常：" + e);
            t.setTextColor(0xFFFF8A65);
            box.addView(t);
        }
        return box;
    }

    private RemoteViews cells(int cells, Prefs p, Engine.Snapshot s, int w, int h, float d) {
        Context ctx = this;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(0x00000000);
        WidgetRenderer.draw(c, w, h, cells, p, s, d);
        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.widget_root);
        rv.setImageViewBitmap(R.id.widget_image, bmp);
        return rv;
    }

    @Override
    protected void onDestroy() {
        try {
            if (host != null) {
                if (wid >= 0) host.deleteAppWidgetId(wid);
                host.stopListening();
            }
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }
}
