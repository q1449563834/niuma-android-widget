package com.feiyuaq.niuma;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** 日期小工具：全部按本地时区、零点对齐 */
public class D {
    static final SimpleDateFormat KEY = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    static final String[] WEEK = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};

    public static String key(Calendar c) {
        return KEY.format(c.getTime());
    }

    public static Calendar at(Calendar base, int offsetDay) {
        Calendar c = (Calendar) base.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_MONTH, offsetDay);
        return c;
    }

    public static Calendar parse(String key) {
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(KEY.parse(key));
        } catch (Exception e) {
            c.setTimeInMillis(System.currentTimeMillis());
        }
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /** 两个日期相差几天（b - a），不受夏令时影响 */
    public static long daysBetween(Calendar a, Calendar b) {
        long ms = b.getTimeInMillis() - a.getTimeInMillis();
        return Math.round(ms / 86400000.0);
    }

    public static String hm(int minutes) {
        int m = ((minutes % 1440) + 1440) % 1440;
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60);
    }

    public static String hms(double seconds) {
        if (seconds < 0) seconds = 0;
        long t = (long) Math.floor(seconds);
        long h = t / 3600, m = t % 3600 / 60, s = t % 60;
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        return String.format(Locale.US, "%d:%02d", m, s);
    }

    /** 中文时长：3 小时 21 分 */
    public static String cn(double seconds, boolean withSeconds) {
        long t = (long) Math.floor(Math.max(0, seconds));
        long h = t / 3600, m = t % 3600 / 60, s = t % 60;
        if (withSeconds) {
            if (h > 0) return h + "小时" + String.format(Locale.US, "%02d", m) + "分" + String.format(Locale.US, "%02d", s) + "秒";
            if (m > 0) return m + "分" + String.format(Locale.US, "%02d", s) + "秒";
            return s + "秒";
        }
        if (h > 0) return h + " 小时 " + m + " 分";
        if (m > 0) return m + " 分钟";
        return "不到 1 分钟";
    }
}
