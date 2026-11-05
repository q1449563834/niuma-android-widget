package com.feiyuaq.niuma;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

/**
 * 桌面小部件的「秒针」。
 * RemoteViews 本身最短只能 30 分钟更新一次，想看到数字每秒都在涨，就得有个常驻刷新，
 * 这里用最低优先级的前台服务 + 1 秒定时器，熄屏时自动跳过重绘，不浪费电。
 */
public class TickerService extends Service {

    private static final String CHANNEL = "niuma_tick";
    private static final int NOTIF_ID = 1001;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean alive = false;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!alive) return;
            try {
                if (isScreenOn()) WidgetUpdater.updateAll(TickerService.this);
            } catch (Exception ignored) {
            }
            handler.postDelayed(this, 1000);
        }
    };

    private boolean isScreenOn() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= 20) return pm.isInteractive();
            return pm.isScreenOn();
        } catch (Exception e) {
            return true;
        }
    }

    public static void ensure(Context c) {
        Prefs p = Prefs.load(c);
        WidgetUpdater.updateAll(c);
        if (!p.ticker || WidgetUpdater.countWidgets(c) == 0) return;
        try {
            Intent i = new Intent(c, TickerService.class);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
            else c.startService(i);
        } catch (Throwable t) {
            // Android 12+ 在后台（比如系统 30 分钟一次的小部件广播里）是不允许拉前台服务的，
            // 这时把原因打出来，而不是静默失败让用户以为小部件坏了。
            android.util.Log.w("NiuMaTicker", "后台启动刷新服务被拒，等用户打开应用再恢复秒级刷新", t);
        }
    }

    public static void stop(Context c) {
        try {
            c.stopService(new Intent(c, TickerService.class));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        alive = true;
        // 通知/前台服务在国产 ROM（HyperOS 14）上各种策略不一，任何一步失败都不能把进程带崩，
        // 大不了没有常驻通知、刷新频率降级，也绝不能让用户点开应用就闪退。
        try {
            createChannel();
            startForeground(NOTIF_ID, buildNotification());
        } catch (Throwable t) {
            android.util.Log.w("NiuMaTicker", "前台服务启动失败，降级为普通服务", t);
        }
        handler.postDelayed(tick, 200);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "小部件实时刷新",
                NotificationManager.IMPORTANCE_MIN);
        ch.setDescription("让桌面上的金额每秒都在涨");
        ch.setShowBadge(false);
        ch.setSound(null, null);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        PendingIntent pi = WidgetUpdater.openApp(this);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) b = new Notification.Builder(this, CHANNEL);
        else b = new Notification.Builder(this);
        b.setContentTitle("牛马脱敏器")
                .setContentText("桌面小部件正在实时计薪")
                .setSmallIcon(R.drawable.ic_stat_niuma)
                .setOngoing(true)
                .setContentIntent(pi)
                .setPriority(Notification.PRIORITY_MIN);
        return b.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!alive) {
            alive = true;
            handler.postDelayed(tick, 200);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        alive = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
