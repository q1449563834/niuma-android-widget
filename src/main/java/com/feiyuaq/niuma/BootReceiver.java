package com.feiyuaq.niuma;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 开机、以及本应用被覆盖安装之后，把秒级刷新服务重新拉起来。
 * 不加这个的话服务被系统杀掉（或手机重启）以后，小部件会停在最后一次画出来的数值上，
 * 直到用户再打开一次应用——Android 12+ 起后台广播里启动前台服务是被禁止的，
 * 只有开机广播这类系统广播在豁免名单里。
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        WidgetUpdater.updateAll(c);
        TickerService.ensure(c);
    }
}
