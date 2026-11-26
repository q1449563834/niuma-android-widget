package com.feiyuaq.niuma;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/** 循环班表：把一个周期拆成 N 天，逐日查表 */
public class Cycle {

    public static String[] days(String cycle) {
        if (cycle == null || cycle.trim().length() == 0) return new String[0];
        String c = cycle.replace("\n", "").replace(" ", "");
        return c.split("\\|");
    }

    public static String join(String[] days) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.length; i++) {
            if (i > 0) sb.append('|');
            sb.append(days[i]);
        }
        return sb.toString();
    }

    /** 今天落在循环的第几天（0 起） */
    public static int index(Prefs p, Calendar day) {
        String[] days = days(p.cycleForMode());
        int len = days.length;
        if (len == 0) return 0;
        Calendar anchor = D.parse(p.anchor);
        long diff = D.daysBetween(anchor, D.at(day, 0));
        int i = (int) (((diff % len) + len) % len);
        return i;
    }

    /**
     * 循环跑过一轮之后，把「循环第 1 天」（{@code p.anchor}）前移到「今天所在的这一轮的第一天」。
     * 只前移整数个周期，今天在循环里的第几天不变，所以排班/计薪结果完全不受影响，
     * 变的只是界面上那句「循环第 1 天 = …」的日期，不会永远停在几个月前的首次启动日。
     */
    public static boolean rollAnchor(Prefs p, Calendar today) {
        if (p.mode == Prefs.MODE_FIXED) return false;       // 长白班没有循环
        int len = days(p.cycleForMode()).length;
        if (len <= 0) return false;
        Calendar a = D.parse(p.anchor);
        long diff = D.daysBetween(a, D.at(today, 0));
        if (diff < len) return false;                        // 还在第一轮里，或「循环第 1 天」本来就在未来
        long cycles = diff / len;
        if (cycles <= 0 || cycles * len > 100000L) return false;
        a.add(Calendar.DAY_OF_MONTH, (int) (cycles * len));
        p.anchor = D.key(a);
        return true;
    }

    public static String dayAt(Prefs p, int index) {
        String[] days = days(p.cycleForMode());
        if (days.length == 0) return "R";
        return days[((index % days.length) + days.length) % days.length];
    }

    /** 循环里出现最多的上班班次，用于「今天临时补班」时套用 */
    public static String typicalWorkPlan(Prefs p) {
        if (p.mode == Prefs.MODE_FIXED) return p.fixedPlan();
        Map<String, Integer> count = new HashMap<String, Integer>();
        String best = null;
        int bestN = 0;
        for (String d : days(p.cycleForMode())) {
            Plan plan = Plan.parse(d);
            if (!plan.work) continue;
            String key = plan.encode();
            Integer n = count.get(key);
            n = n == null ? 1 : n + 1;
            count.put(key, n);
            if (n > bestN) {
                bestN = n;
                best = key;
            }
        }
        return best == null ? p.fixedPlan() : best;
    }

    /** 周期总工时（小时），用于界面上给个直观参考 */
    public static double hoursPerCycle(Prefs p) {
        double h = 0;
        for (String d : days(p.cycleForMode())) h += Plan.parse(d).paidMinutes() / 60.0;
        return h;
    }
}
