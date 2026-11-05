package com.feiyuaq.niuma;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 全部设置。原项目用 settings.ini + 反射，这里改用 SharedPreferences + JSON，
 * 字段语义与原版一一对应，另外扩展了「班制 / 循环排班」相关字段。
 */
public class Prefs {

    public static final int PAY_DAILY = 0, PAY_MONTHLY = 1, PAY_HOURLY = 2;
    public static final int MODE_FIXED = 0, MODE_THREE = 1, MODE_CYCLE = 2;

    /** 三班倒：早 8h、中 8h、夜 8h、休 ×2，8 天一轮 */
    public static final String THREE_CYCLE =
            "W,480,960,-1,-1|W,480,960,-1,-1|W,960,1440,-1,-1|W,960,1440,-1,-1"
            + "|W,0,480,-1,-1|W,0,480,-1,-1|R|R";

    /** 用户描述的 16 天循环：白、白、休、白、休、中夜(16h)、休、休，重复两轮 */
    public static final String CYCLE16 =
            "W,480,1200,-1,-1|W,480,1200,-1,-1|R|W,480,1200,-1,-1|R|W,960,1920,-1,-1|R|R"
            + "|W,480,1200,-1,-1|W,480,1200,-1,-1|R|W,480,1200,-1,-1|R|W,960,1920,-1,-1|R|R";

    public String appName = "牛马脱敏器";

    // ---- 薪资 ----
    public int salaryMode = PAY_MONTHLY;
    public double amount = 7000;        // 日薪 / 月薪 / 时薪
    public double monthDays = 21.75;    // 月薪折算天数
    public int decimals = 5;
    public String currency = "¥";
    public int payDay = 10;
    public boolean overtime = false;
    public double overtimeRate = 1.5;

    // ---- 班制 ----
    public int mode = MODE_CYCLE;
    public int fixedStart = 8 * 60 + 30;   // 08:30
    public int fixedEnd = 17 * 60 + 30;    // 17:30
    public boolean lunchEnabled = true;
    public int lunchStart = 12 * 60;
    public int lunchEnd = 13 * 60;
    public int workMask = 0x3E;            // 周一~周五（bit0=周日）

    public String threeCycle = THREE_CYCLE;
    public String customCycle = CYCLE16;
    public String anchor = "";             // 循环第 1 天对应日期 yyyy-MM-dd

    // ---- APN（三班倒）三个班次的时间，可在设置里改 ----
    public int apnDayStart = 8 * 60, apnDayEnd = 16 * 60;      // 白班 08:00–16:00
    public int apnMidStart = 16 * 60, apnMidEnd = 24 * 60;     // 中班 16:00–24:00
    public int apnNightStart = 0, apnNightEnd = 8 * 60;        // 夜班 00:00–08:00

    // ---- 显示 ----
    public boolean masked = false;
    public boolean odometer = true;
    public boolean ticker = true;          // 常驻刷新（前台服务，1 秒一帧）

    public String overrideDate = "";
    public int overrideMode = 0;           // 0 正常 / 1 今天休息 / 2 今天上班

    public String wishlist = "一瓶可乐=3\n一个煎饼果子=8\n一杯奶茶=15\n一份猪脚饭=22\n一杯咖啡=30\n一张电影票=45\n一件T恤=79\n一顿火锅=120\n一双球鞋=399";
    public String quotes = "摸鱼不是偷懒，是在给老板省电。\n工资不多，但每一秒都在到账。\n上班是为了下班，下班是为了明天还能上班。\n今天的你，又为老板的新车贡献了一个轮毂。\n别慌，钱在路上，人在工位。\n带薪如厕，是打工人最后的倔强。\n只要我不努力，老板就过不上他想要的生活。\n工作是老板的，命是自己的。\n再坚持一下，离下班又近了一秒。\n今日份的牛马已上线，正在稳定产出。";

    private static final String FILE = "prefs";

    // ---------------------------------------------------------------- 存盘

    public static Prefs load(Context c) {
        Prefs p = new Prefs();
        SharedPreferences sp = c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        String raw = sp.getString("json", null);
        if (raw == null) {
            p.anchor = D.key(Calendar.getInstance());
            return p;
        }
        try {
            JSONObject o = new JSONObject(raw);
            p.appName = o.optString("appName", p.appName);
            p.salaryMode = o.optInt("salaryMode", p.salaryMode);
            p.amount = o.optDouble("amount", p.amount);
            p.monthDays = o.optDouble("monthDays", p.monthDays);
            p.decimals = o.optInt("decimals", p.decimals);
            p.currency = o.optString("currency", p.currency);
            p.payDay = o.optInt("payDay", p.payDay);
            p.overtime = o.optBoolean("overtime", p.overtime);
            p.overtimeRate = o.optDouble("overtimeRate", p.overtimeRate);
            p.mode = o.optInt("mode", p.mode);
            p.fixedStart = o.optInt("fixedStart", p.fixedStart);
            p.fixedEnd = o.optInt("fixedEnd", p.fixedEnd);
            p.lunchEnabled = o.optBoolean("lunchEnabled", p.lunchEnabled);
            p.lunchStart = o.optInt("lunchStart", p.lunchStart);
            p.lunchEnd = o.optInt("lunchEnd", p.lunchEnd);
            p.workMask = o.optInt("workMask", p.workMask);
            p.threeCycle = o.optString("threeCycle", p.threeCycle);
            p.customCycle = o.optString("customCycle", p.customCycle);
            p.apnDayStart = o.optInt("apnDayStart", p.apnDayStart);
            p.apnDayEnd = o.optInt("apnDayEnd", p.apnDayEnd);
            p.apnMidStart = o.optInt("apnMidStart", p.apnMidStart);
            p.apnMidEnd = o.optInt("apnMidEnd", p.apnMidEnd);
            p.apnNightStart = o.optInt("apnNightStart", p.apnNightStart);
            p.apnNightEnd = o.optInt("apnNightEnd", p.apnNightEnd);
            p.anchor = o.optString("anchor", p.anchor);
            p.masked = o.optBoolean("masked", p.masked);
            p.odometer = o.optBoolean("odometer", p.odometer);
            p.ticker = o.optBoolean("ticker", p.ticker);
            p.overrideDate = o.optString("overrideDate", p.overrideDate);
            p.overrideMode = o.optInt("overrideMode", p.overrideMode);
            p.wishlist = o.optString("wishlist", p.wishlist);
            p.quotes = o.optString("quotes", p.quotes);
        } catch (Exception ignored) {
        }
        if (p.anchor == null || p.anchor.length() < 10)
            p.anchor = D.key(Calendar.getInstance());
        return p;
    }

    public void save(Context c) {
        try {
            JSONObject o = new JSONObject();
            o.put("appName", appName);
            o.put("salaryMode", salaryMode);
            o.put("amount", amount);
            o.put("monthDays", monthDays);
            o.put("decimals", decimals);
            o.put("currency", currency);
            o.put("payDay", payDay);
            o.put("overtime", overtime);
            o.put("overtimeRate", overtimeRate);
            o.put("mode", mode);
            o.put("fixedStart", fixedStart);
            o.put("fixedEnd", fixedEnd);
            o.put("lunchEnabled", lunchEnabled);
            o.put("lunchStart", lunchStart);
            o.put("lunchEnd", lunchEnd);
            o.put("workMask", workMask);
            o.put("threeCycle", threeCycle);
            o.put("customCycle", customCycle);
            o.put("apnDayStart", apnDayStart);
            o.put("apnDayEnd", apnDayEnd);
            o.put("apnMidStart", apnMidStart);
            o.put("apnMidEnd", apnMidEnd);
            o.put("apnNightStart", apnNightStart);
            o.put("apnNightEnd", apnNightEnd);
            o.put("anchor", anchor);
            o.put("masked", masked);
            o.put("odometer", odometer);
            o.put("ticker", ticker);
            o.put("overrideDate", overrideDate);
            o.put("overrideMode", overrideMode);
            o.put("wishlist", wishlist);
            o.put("quotes", quotes);
            c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                    .putString("json", o.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public Prefs copy() {
        Prefs p = new Prefs();
        p.appName = appName;
        p.salaryMode = salaryMode;
        p.amount = amount;
        p.monthDays = monthDays;
        p.decimals = decimals;
        p.currency = currency;
        p.payDay = payDay;
        p.overtime = overtime;
        p.overtimeRate = overtimeRate;
        p.mode = mode;
        p.fixedStart = fixedStart;
        p.fixedEnd = fixedEnd;
        p.lunchEnabled = lunchEnabled;
        p.lunchStart = lunchStart;
        p.lunchEnd = lunchEnd;
        p.workMask = workMask;
        p.threeCycle = threeCycle;
        p.customCycle = customCycle;
        p.apnDayStart = apnDayStart;
        p.apnDayEnd = apnDayEnd;
        p.apnMidStart = apnMidStart;
        p.apnMidEnd = apnMidEnd;
        p.apnNightStart = apnNightStart;
        p.apnNightEnd = apnNightEnd;
        p.anchor = anchor;
        p.masked = masked;
        p.odometer = odometer;
        p.ticker = ticker;
        p.overrideDate = overrideDate;
        p.overrideMode = overrideMode;
        p.wishlist = wishlist;
        p.quotes = quotes;
        return p;
    }

    // ------------------------------------------------------------ 便捷方法

    public String cycleForMode() {
        return mode == MODE_THREE ? threeCycle : customCycle;
    }

    public void setCycleForMode(String value) {
        if (mode == MODE_THREE) threeCycle = value;
        else customCycle = value;
    }

    /** APN 三班倒：用三个班各自的上下班时间拼出 8 天循环（白×2、中×2、夜×2、休×2） */
    public String apnCycle() {
        String d = shift(apnDayStart, apnDayEnd);
        String m = shift(apnMidStart, apnMidEnd);
        String n = shift(apnNightStart, apnNightEnd);
        return d + "|" + d + "|" + m + "|" + m + "|" + n + "|" + n + "|R|R";
    }

    /** 一段班次的编码；下班时间早于上班时间视为跨零点（20:00–04:00） */
    public static String shift(int st, int en) {
        if (en <= st) en += 1440;
        return "W," + st + "," + en + ",-1,-1";
    }

    public double dailyPay() {
        if (salaryMode == PAY_MONTHLY) return monthDays > 0 ? amount / monthDays : 0;
        if (salaryMode == PAY_HOURLY) return amount * 8; // 仅作 8 小时基准展示
        return amount;
    }

    public boolean isFixedWorkday(int dow) {
        return (workMask & (1 << dow)) != 0;
    }

    public String fixedPlan() {
        int bs = lunchEnabled ? lunchStart : -1;
        int be = lunchEnabled ? lunchEnd : -1;
        return "W," + fixedStart + "," + fixedEnd + "," + bs + "," + be;
    }

    public List<String[]> wishItems() {
        List<String[]> list = new ArrayList<String[]>();
        for (String line : wishlist.split("\n")) {
            String l = line.trim().replace('＝', '=');
            int i = l.lastIndexOf('=');
            if (i <= 0) continue;
            try {
                double price = Double.parseDouble(l.substring(i + 1).trim());
                if (price > 0) list.add(new String[]{l.substring(0, i).trim(), String.valueOf(price)});
            } catch (Exception ignored) {
            }
        }
        java.util.Collections.sort(list, new java.util.Comparator<String[]>() {
            public int compare(String[] a, String[] b) {
                return Double.compare(Double.parseDouble(a[1]), Double.parseDouble(b[1]));
            }
        });
        return list;
    }

    public List<String> quoteList() {
        List<String> l = new ArrayList<String>();
        for (String q : quotes.split("\n"))
            if (q.trim().length() > 0) l.add(q.trim());
        return l;
    }

    /** 循环 / 逐日计划的编辑帮手 */
    public static JSONArray planToJson(String[] days) {
        JSONArray a = new JSONArray();
        for (String s : days) a.put(s);
        return a;
    }
}
