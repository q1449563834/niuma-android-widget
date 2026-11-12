package com.feiyuaq.niuma;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 开机、本应用被覆盖安装、以及系统时间 / 时区 / 日期变化之后，把刷新链路重新挂上。
 * Android 12+ 起后台广播里直接拉起前台服务是被禁止的，只有开机广播这类系统广播在豁免名单里；
 * 统一交给 RefreshScheduler：秒级刷新开着就拉服务，关着就挂分钟闹钟。
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        try {
            Prefs p = Prefs.load(c);
            // 覆盖安装 / 开机后通知渠道可能还没建，这里只更新数值，服务按设置再决定
            if (p.ticker) TickerService.ensure(c);
            RefreshScheduler.ensure(c);
        } catch (Throwable t) {
            android.util.Log.w("NiuMaTicker", "开机拉起刷新失败", t);
        }
    }
}
