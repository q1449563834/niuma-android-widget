package com.feiyuaq.niuma;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/** 临时自检页：把引擎在几个关键场景里跑一遍，结果打到 logcat 和屏幕上 */
public class CheckActivity extends Activity {

    private static final String TAG = "NIUMA_CHECK";
    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Shanghai");

    private final StringBuilder out = new StringBuilder();
    private int pass = 0, fail = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        runAll();
        TextView tv = new TextView(this);
        tv.setText(out.toString());
        tv.setTextSize(10);
        tv.setTextColor(0xFF101010);
        tv.setBackgroundColor(0xFFFFFFFF);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        setContentView(sv);
    }

    private long at(int y, int m, int d, int hh, int mm) {
        Calendar c = Calendar.getInstance(TZ);
        c.clear();
        c.set(y, m - 1, d, hh, mm, 0);
        return c.getTimeInMillis();
    }

    private void check(String name, boolean ok, String detail) {
        if (ok) pass++;
        else fail++;
        String line = (ok ? "[PASS] " : "[FAIL] ") + name + "  " + detail;
        Log.i(TAG, line);
        out.append(line).append('\n');
    }

    private static String planStr(Plan p) {
        if (p == null) return "R";
        return "W," + p.start + "," + p.end;
    }

    private Prefs base() {
        Prefs p = new Prefs();
        p.salaryMode = Prefs.PAY_MONTHLY;
        p.amount = 7000;
        p.monthDays = 21.75;
        p.mode = Prefs.MODE_CYCLE;
        p.customCycle = Prefs.CYCLE16;
        p.threeCycle = Prefs.THREE_CYCLE;
        p.anchor = "2026-11-05";
        p.overtime = false;
        p.overrideMode = 0;
        p.workMask = 0x3E;
        p.lunchEnabled = true;
        p.lunchStart = 12 * 60;
        p.lunchEnd = 13 * 60;
        p.fixedStart = 8 * 60 + 30;
        p.fixedEnd = 17 * 60 + 30;
        return p;
    }

    private void runAll() {
        fixedShift();
        threeShift();
        cycleLayout();
        overnight();
        hourlyVsMonthly();
        monotonic();
        payDayCheck();
        String summary = "通过 " + pass + " / 失败 " + fail;
        Log.i(TAG, "SUMMARY " + summary);
        out.append("\n==== ").append(summary).append(" ====\n");
    }

    // 长白班：08:30–17:30，午休 12:00–13:00 不计薪
    private void fixedShift() {
        Prefs p = base();
        p.mode = Prefs.MODE_FIXED;
        double day = 7000 / 21.75;

        Engine.Snapshot lunch = Engine.compute(p, at(2026, 11, 5, 12, 30));
        check("长白班 午休不计薪", lunch.state == Engine.ST_BREAK && lunch.speed == 0,
                "state=" + lunch.stateText + " speed=" + lunch.speed);

        Engine.Snapshot working = Engine.compute(p, at(2026, 11, 5, 14, 0));
        check("长白班 午休后继续计薪", working.state == Engine.ST_WORK && working.speed > 0,
                "state=" + working.stateText + " speed=" + working.speed);

        Engine.Snapshot after = Engine.compute(p, at(2026, 11, 5, 18, 0));
        check("长白班 下班后金额=今日满",
                after.state == Engine.ST_AFTER && Math.abs(after.earned - after.fullDayPay) < 1e-6
                        && Math.abs(after.fullDayPay - day) < 1e-6,
                "earned=" + after.earned + " full=" + after.fullDayPay + " 日薪=" + day);

        check("长白班 计薪 8 小时", Math.abs(after.paidSeconds - 8 * 3600) < 1,
                "paidSeconds=" + after.paidSeconds);
    }

    // 三班倒：早 08–16 / 中 16–24 / 夜 00–08
    private void threeShift() {
        Prefs p = base();
        p.mode = Prefs.MODE_THREE;
        p.anchor = "2026-11-05"; // D1 早班

        Engine.Snapshot early = Engine.compute(p, at(2026, 11, 5, 10, 0));
        check("三班倒 D1 早班在岗", early.state == Engine.ST_WORK
                && early.shiftStart == 480 && early.shiftEnd == 960,
                "state=" + early.stateText + " " + early.shiftStart + "-" + early.shiftEnd);

        Engine.Snapshot mid = Engine.compute(p, at(2026, 11, 7, 18, 0)); // D3 中班
        check("三班倒 D3 中班在岗", mid.state == Engine.ST_WORK
                && mid.shiftStart == 960 && mid.shiftEnd == 1440 && mid.speed > 0,
                "state=" + mid.stateText + " " + mid.shiftStart + "-" + mid.shiftEnd);

        Engine.Snapshot rest = Engine.compute(p, at(2026, 11, 11, 10, 0)); // D7 休
        check("三班倒 D7 休息", rest.state == Engine.ST_REST && rest.earned == 0,
                "state=" + rest.stateText);
    }

    // 16 天循环落位：白 白 休 白 休 中夜 休 休 ×2
    private void cycleLayout() {
        Prefs p = base();
        String[] exp = {"W,480,1200", "W,480,1200", "R", "W,480,1200", "R", "W,960,1920", "R", "R",
                "W,480,1200", "W,480,1200", "R", "W,480,1200", "R", "W,960,1920", "R", "R"};
        int bad = 0;
        StringBuilder detail = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            Calendar c = Calendar.getInstance(TZ);
            c.clear();
            c.set(2026, 10, 5, 0, 0, 0);
            c.add(Calendar.DAY_OF_MONTH, i);
            String got = planStr(Engine.planFor(p, c));
            if (!got.equals(exp[i])) {
                bad++;
                detail.append("D").append(i + 1).append(" 期望").append(exp[i])
                        .append(" 实际").append(got).append("; ");
            }
        }
        check("16 天循环排班落位", bad == 0, bad == 0 ? "16 天全部对上" : detail.toString());

        Engine.Snapshot d6 = Engine.compute(p, at(2026, 11, 10, 18, 0));
        check("循环第 6 天是中夜 16h", d6.hasShift && d6.shiftStart == 960 && d6.shiftEnd == 1920
                && Math.abs(d6.shiftHours - 16) < 1e-9,
                "start=" + d6.shiftStart + " end=" + d6.shiftEnd + " hours=" + d6.shiftHours);
    }

    // 中夜 16:00–次日 08:00：跨零点不断档
    private void overnight() {
        Prefs p = base();
        p.anchor = "2026-10-31"; // 2026-11-05 = 第 6 天，中夜
        Engine.Snapshot s18 = Engine.compute(p, at(2026, 11, 5, 18, 0));
        Engine.Snapshot s23 = Engine.compute(p, at(2026, 11, 5, 23, 30));
        Engine.Snapshot s07 = Engine.compute(p, at(2026, 11, 6, 7, 0));
        Engine.Snapshot s09 = Engine.compute(p, at(2026, 11, 6, 9, 0));

        check("中夜 18:00 在岗且已计薪", s18.state == Engine.ST_WORK && s18.earned > 0
                        && s18.earned < s18.fullDayPay,
                "earned=" + s18.earned + " full=" + s18.fullDayPay);
        check("中夜 23:30 比 18:00 多", s23.earned > s18.earned,
                s18.earned + " -> " + s23.earned);
        check("跨零点后仍在计薪（不归零）", s07.state == Engine.ST_WORK && s07.earned > 0,
                "state=" + s07.stateText + " earned=" + s07.earned);
        check("次日 09:00 已下班", s09.state != Engine.ST_WORK, "state=" + s09.stateText);

        // 当天 24:00 拿到的应该正好是中夜班一半（8h/16h）
        Engine.Snapshot s2359 = Engine.compute(p, at(2026, 11, 5, 23, 59));
        double half = 7000 / 21.75 / 2;
        check("当天零点前只结算 8 小时工资", Math.abs(s2359.earned - half) < half * 0.01,
                "earned=" + s2359.earned + " 期望≈" + half);
    }

    private void hourlyVsMonthly() {
        Prefs p = base();
        p.mode = Prefs.MODE_CYCLE;
        p.anchor = "2026-11-05";

        p.salaryMode = Prefs.PAY_HOURLY;
        p.amount = 30;
        Engine.Snapshot white12 = Engine.compute(p, at(2026, 11, 5, 20, 0)); // 白班 12h
        Engine.Snapshot night16 = Engine.compute(p, at(2026, 11, 10, 23, 0)); // 中夜 16h
        check("时薪：12h 白班=360", Math.abs(white12.fullDayPay - 360) < 1e-6,
                "full=" + white12.fullDayPay);
        check("时薪：16h 中夜按当天只算 8h=240",
                Math.abs(night16.fullDayPay - 240) < 1e-6, "full=" + night16.fullDayPay);

        p.salaryMode = Prefs.PAY_MONTHLY;
        p.amount = 7000;
        Engine.Snapshot m12 = Engine.compute(p, at(2026, 11, 5, 20, 0));
        Engine.Snapshot m16 = Engine.compute(p, at(2026, 11, 10, 23, 0));
        check("月薪：白班当天=日薪", Math.abs(m12.fullDayPay - 7000 / 21.75) < 1e-6,
                "full=" + m12.fullDayPay);
        check("月薪：中夜当天=半日薪", Math.abs(m16.fullDayPay - 7000 / 21.75 / 2) < 1e-6,
                "full=" + m16.fullDayPay);
    }

    // 单调性：上班时段内每 5 分钟取一次，金额不许回退（跨零点也不能断）
    private void monotonic() {
        Prefs p = base();
        p.anchor = "2026-10-31"; // 11-05 = 中夜 16h

        int bad = 0;
        double prev = -1, maxJump = 0;
        for (int m = 16 * 60; m <= 32 * 60; m += 5) {  // 16:00 → 次日 08:00
            int day = 5 + m / (24 * 60);
            Engine.Snapshot s = Engine.compute(p, at(2026, 11, day, (m % (24 * 60)) / 60, m % 60));
            if (prev >= 0 && s.earned < prev - 1e-9) bad++;
            if (prev >= 0) maxJump = Math.max(maxJump, s.earned - prev);
            prev = s.earned;
        }
        check("中夜 16h 跨零点单调不减", bad == 0,
                "回退=" + bad + " 单步最大增量=" + String.format("%.5f", maxJump));

        Engine.Snapshot s2355 = Engine.compute(p, at(2026, 11, 5, 23, 55));
        Engine.Snapshot s0005 = Engine.compute(p, at(2026, 11, 6, 0, 5));
        check("零点前后不归零", s0005.earned > s2355.earned,
                s2355.earned + " → " + s0005.earned);

        p = base(); // 11-05 白班
        bad = 0;
        prev = -1;
        for (int m = 8 * 60; m <= 20 * 60; m += 5) {
            Engine.Snapshot s = Engine.compute(p, at(2026, 11, 5, m / 60, m % 60));
            if (prev >= 0 && s.earned < prev - 1e-9) bad++;
            prev = s.earned;
        }
        check("白班 12h 单调不减", bad == 0, "回退=" + bad);

        // 16 天里每个班的上班区间都巡检一遍
        p = base();
        p.anchor = "2026-11-05";
        int bad2 = 0;
        String badWhere = "";
        for (int i = 0; i < 16; i++) {
            Calendar c = Calendar.getInstance(TZ);
            c.clear();
            c.set(2026, 10, 5, 0, 0, 0);
            c.add(Calendar.DAY_OF_MONTH, i);
            Plan plan = Engine.planFor(p, c);
            if (plan == null) continue;
            double last = -1;
            for (int m = plan.start; m <= plan.end; m += 10) {
                Calendar t2 = Calendar.getInstance(TZ);
                t2.clear();
                t2.set(2026, 10, 5, 0, 0, 0);
                t2.add(Calendar.DAY_OF_MONTH, i);
                t2.add(Calendar.MINUTE, m);
                Engine.Snapshot s = Engine.compute(p, t2.getTimeInMillis());
                if (last >= 0 && s.earned < last - 1e-9) {
                    bad2++;
                    badWhere += "D" + (i + 1) + "@" + m + "min; ";
                }
                last = s.earned;
            }
        }
        check("16 天每个班次内单调不减", bad2 == 0,
                bad2 == 0 ? "8 个班次全部单调" : badWhere);
    }

    private void payDayCheck() {
        Prefs p = base();
        int d = Engine.daysToPayDay(p, at(2026, 11, 5, 10, 0));
        check("发薪日 10 号 → 还有 5 天", d == 5, "days=" + d);
    }
}
