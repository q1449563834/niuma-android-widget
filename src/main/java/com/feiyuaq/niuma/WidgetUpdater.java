package com.feiyuaq.niuma;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.util.LruCache;
import android.widget.RemoteViews;

/** 把计算好的快照画成位图，塞进 RemoteViews 的 ImageView 里 */
public class WidgetUpdater {

    /** RemoteViews 走 Binder，位图太大有些启动器会直接丢弃（表现就是整块小部件透明/空白） */
    private static final int MAX_PIXELS = 200000;

    private static final LruCache<Integer, Bitmap> CACHE = new LruCache<Integer, Bitmap>(4) {
        @Override
        protected int sizeOf(Integer key, Bitmap value) {
            return 1;
        }
    };

    public static void updateAll(Context ctx) {
        updateProvider(ctx, NiumaWidget1x2.class, 2, 1);
        updateProvider(ctx, NiumaWidget1x4.class, 4, 1);
        updateProvider(ctx, NiumaWidget2x2.class, 2, 2);
        updateProvider(ctx, NiumaWidget2x4.class, 4, 2);
    }

    public static void updateProvider(Context ctx, Class<?> cls, int cols, int rows) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(ctx);
            int[] ids = m.getAppWidgetIds(new ComponentName(ctx, cls));
            if (ids == null) return;
            Prefs p = Prefs.load(ctx);
            Engine.Snapshot snap = Engine.compute(p, System.currentTimeMillis());
            for (int id : ids) updateOne(ctx, m, id, cols, rows, p, snap);
        } catch (Exception e) {
            android.util.Log.w("NiuMaWidget", "更新小部件失败", e);
        }
    }

    public static void updateOne(Context ctx, AppWidgetManager m, int id, int cols, int rows,
                                 Prefs p, Engine.Snapshot snap) {
        float density = ctx.getResources().getDisplayMetrics().density;
        Bundle o = m.getAppWidgetOptions(id);
        int wDp = o != null ? o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) : 0;
        int hDp = o != null ? o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) : 0;
        if (wDp <= 0) wDp = cols == 2 ? 110 : 250;
        if (hDp <= 0) hDp = rows == 1 ? 44 : 110;

        int wPx = Math.max(1, Math.round(wDp * density));
        int hPx = Math.max(1, Math.round(hDp * density));
        if ((long) wPx * hPx > MAX_PIXELS) {
            double f = Math.sqrt((double) MAX_PIXELS / ((double) wPx * hPx));
            wPx = Math.max(1, (int) (wPx * f));
            hPx = Math.max(1, (int) (hPx * f));
        }

        Bitmap bmp = CACHE.get(id);
        if (bmp == null || bmp.getWidth() != wPx || bmp.getHeight() != hPx || bmp.isRecycled()) {
            bmp = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888);
            CACHE.put(id, bmp);
        }
        Canvas c = new Canvas(bmp);
        // 复用位图时必须真清空：drawColor(0x00000000) 是 SRC_OVER 透明色，等于什么都没做，
        // 上一帧的数字/进度条会残留成幽灵斑。
        c.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
        WidgetRenderer.draw(c, wPx, hPx, cols, rows, p, snap, density);

        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_root);
        rv.setImageViewBitmap(R.id.widget_image, bmp);
        rv.setOnClickPendingIntent(R.id.widget_image, openApp(ctx));
        rv.setContentDescription(R.id.widget_image, "牛马脱敏器：" + snap.stateText);
        m.updateAppWidget(id, rv);
    }

    public static PendingIntent openApp(Context ctx) {
        Intent i = new Intent(ctx, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Android 12（API 31）起，创建 PendingIntent 必须声明可变性，否则直接抛 IllegalArgumentException。
        // targetSdk 33 在 Android 14 上跑，漏了 FLAG_IMMUTABLE 会让「更新小部件」和「起前台服务」双双炸掉：
        // 小部件一直空白（透明），点开应用就闪退。国产 ROM（HyperOS / MIUI）上尤其明显。
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(ctx, 0, i, flags);
    }

    /** 桌面上某个 provider 已经放了几块 */
    public static int[] ids(Context ctx, Class<?> cls) {
        return AppWidgetManager.getInstance(ctx).getAppWidgetIds(new ComponentName(ctx, cls));
    }

    public static int countWidgets(Context ctx) {
        AppWidgetManager m = AppWidgetManager.getInstance(ctx);
        return m.getAppWidgetIds(new ComponentName(ctx, NiumaWidget1x2.class)).length
                + m.getAppWidgetIds(new ComponentName(ctx, NiumaWidget1x4.class)).length
                + m.getAppWidgetIds(new ComponentName(ctx, NiumaWidget2x2.class)).length
                + m.getAppWidgetIds(new ComponentName(ctx, NiumaWidget2x4.class)).length;
    }
}
