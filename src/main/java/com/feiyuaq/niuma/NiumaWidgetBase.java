package com.feiyuaq.niuma;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * 1x4 / 2x4 / 1x2 / 2x2 共用同一套逻辑，只有格子数（列数 × 行数）不同。
 * 1x4 = 4 列 1 行（细长条），2x4 = 4 列 2 行（完整卡），
 * 1x2 = 2 列 1 行（迷你条），2x2 = 2 列 2 行（方卡）。
 */
public abstract class NiumaWidgetBase extends AppWidgetProvider {

    /** 占几列 */
    protected abstract int cols();

    /** 占几行 */
    protected abstract int rows();

    @Override
    public void onUpdate(Context context, AppWidgetManager awm, int[] appWidgetIds) {
        // 刷新方式（秒级前台服务 / 每分钟闹钟）由 RefreshScheduler 统一决定，这里只负责重画
        RefreshScheduler.ensure(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager awm, int id, Bundle newOptions) {
        WidgetUpdater.updateAll(context);
    }

    @Override
    public void onEnabled(Context context) {
        RefreshScheduler.ensure(context);
    }

    @Override
    public void onDisabled(Context context) {
        // 最后一块被移除：ensure 会同时停掉前台服务和分钟闹钟，不留后台
        RefreshScheduler.ensure(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        RefreshScheduler.ensure(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction())
                || Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())
                || Intent.ACTION_DATE_CHANGED.equals(intent.getAction())) {
            RefreshScheduler.ensure(context);
        }
    }
}
