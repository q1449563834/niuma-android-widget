package com.feiyuaq.niuma;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 关掉秒级刷新后的「分钟针」：每次闹钟响就把四块小部件重画一遍，再安排下一次。
 * 只做这一件事，不启动任何服务，所以关掉秒级刷新后进程只是被闹钟短暂唤起。
 */
public class MinuteTickReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        try {
            WidgetUpdater.updateAllOnce(c);
            Prefs p = Prefs.load(c);
            if (!p.ticker && WidgetUpdater.countWidgets(c) > 0) RefreshScheduler.armNext(c);
            else RefreshScheduler.cancel(c);
        } catch (Throwable t) {
            android.util.Log.w("NiuMaTicker", "分钟刷新失败", t);
            // 出错也要续上，否则一次异常之后小部件就彻底停更
            try {
                RefreshScheduler.armNext(c);
            } catch (Throwable ignored) {
            }
        }
    }
}
