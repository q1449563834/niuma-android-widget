package com.feiyuaq.niuma;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * 统一决定「小部件到底怎么刷新」，避免两套逻辑各管一半：
 *
 *  - 秒级刷新开着 → 常驻前台服务 TickerService（1 秒一帧）
 *  - 秒级刷新关掉 → AlarmManager 每分钟一次 + MinuteTickReceiver
 *
 * 之前关掉秒级刷新后只在「打开应用」时才更新一次，因为 4 个 widget XML 里
 * updatePeriodMillis="0"，系统根本不会发周期性的 APPWIDGET_UPDATE——所以不是「半小时一次」，
 * 而是永远不动。注意 updatePeriodMillis 小于 30 分钟会被系统拉回 30 分钟，做不出分钟级，
 * 所以分钟级只能靠闹钟自己续期。
 */
public class RefreshScheduler {

    static final String ACTION_MINUTE_TICK = "com.feiyuaq.niuma.action.MINUTE_TICK";
    private static final int RC_MINUTE = 1001;
    /** 对齐到「整分钟 + 300ms」，避开系统时钟抖动落在上一分钟 */
    private static final long ALIGN_MS = 300;

    /** 权限 / 电量策略都可能让某一步失败，任何失败都不能把小部件带崩 */
    public static void ensure(Context c) {
        try {
            Prefs p = Prefs.load(c);
            WidgetUpdater.updateAllOnce(c);
            if (WidgetUpdater.countWidgets(c) == 0) {
                cancel(c);
                TickerService.stop(c);
                return;
            }
            if (p.ticker) {
                cancel(c);
                // 后台拉不起前台服务（Android 12+ 限制）时退回分钟闹钟，至少不会停更
                if (!TickerService.ensure(c)) armNext(c);
            } else {
                TickerService.stop(c);
                armNext(c);
            }
        } catch (Throwable t) {
            android.util.Log.w("NiuMaTicker", "安排小部件刷新失败", t);
        }
    }

    static long nextMinuteAt() {
        long now = System.currentTimeMillis();
        return (now / 60000 + 1) * 60000 + ALIGN_MS;
    }

    /** 上一次用的调度方式，只在变化时打一行日志，方便真机排查又不会每分钟刷屏 */
    private static int lastMode = -1;

    /** 安排下一次分钟刷新（收到闹钟后自己再调一次，形成自续期） */
    static void armNext(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long at = nextMinuteAt();
        PendingIntent pi = pending(c);
        try {
            int mode;
            if (canExact(am)) {
                mode = 0;
                if (Build.VERSION.SDK_INT >= 23)
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                else
                    am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                // 没有精确闹钟授权：退化成「尽量准时」，屏幕亮着时通常仍在一分钟内触发，
                // Doze 下会并入维护窗口（那时小部件也看不见）
                mode = 1;
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                mode = 2;
                am.set(AlarmManager.RTC_WAKEUP, at, pi);
            }
            if (mode != lastMode) {
                lastMode = mode;
                android.util.Log.i("NiuMaTicker", "分钟刷新闹钟已挂上：精度="
                        + (mode == 0 ? "exact" : mode == 1 ? "allow-while-idle" : "普通")
                        + "，下一次约 " + (at - System.currentTimeMillis()) + "ms 后");
            }
        } catch (Throwable t) {
            android.util.Log.w("NiuMaTicker", "设定分钟刷新闹钟失败", t);
        }
    }

    static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            am.cancel(pending(c));
        } catch (Exception ignored) {
        }
    }

    /** Android 12+ 起 setExact* 需要 SCHEDULE_EXACT_ALARM 授权 */
    static boolean canExact(AlarmManager am) {
        if (Build.VERSION.SDK_INT < 31) return true;
        try {
            return am.canScheduleExactAlarms();
        } catch (Throwable t) {
            return false;
        }
    }

    private static PendingIntent pending(Context c) {
        Intent i = new Intent(c, MinuteTickReceiver.class).setAction(ACTION_MINUTE_TICK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        // Android 12+ 漏了 FLAG_IMMUTABLE 会直接抛 IllegalArgumentException（本应用踩过一次闪退）
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, RC_MINUTE, i, flags);
    }
}
