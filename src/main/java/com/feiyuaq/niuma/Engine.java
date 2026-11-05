package com.feiyuaq.niuma;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 计薪内核：移植自原项目 Engine.cs，并扩展成「按班次计薪」。
 *
 * 三种口径：
 *   日薪 / 月薪 —— 每个班次算一天的钱（月薪先按月计薪天数折成日薪），金额平摊到该班的计薪时长上；
 *   时薪        —— 每工作一小时就多一小时的钱，16 小时中夜班天然是 8 小时白班的两倍。
 * 无论哪种口径，金额都只随时间单调递增，绝不会往回跳。
 */
public class Engine {

    public static final int ST_REST = 0, ST_BEFORE = 1, ST_WORK = 2, ST_BREAK = 3,
            ST_BETWEEN = 4, ST_AFTER = 5, ST_OT = 6;

    public static class Snapshot {
        public int state = ST_REST;
        public String stateText = "休息日";
        public String hint = "";
        public String mini = "休息";

        public double earned = 0;        // 今天已赚
        public double speed = 0;         // 元/秒
        public double hourlyPay = 0;     // 元/小时（当前口径）
        public double fullDayPay = 0;    // 今天整天能拿多少
        public double paidSeconds = 0;   // 今天的计薪总秒数
        public double paidElapsed = 0;   // 今天已计薪秒数
        public double progress = 0;      // 0..1
        public double secondsToNext = 0; // 距离下一个节点
        public double overtimeSeconds = 0;

        public Plan todayPlan = null;    // 今天的排班（可能为空）
        public boolean hasShift = false;
        public int shiftStart = -1, shiftEnd = -1;
        public double shiftHours = 0;
        public long nowMs = 0;
        public String clock = "";
        public String weekText = "";
        /** 今天这一班的可读描述（含第二段夜间值班） */
        public String shiftText = "";
        /** 明天的排班，给小部件用 */
        public String tomorrowText = "";
        public String tomorrowShort = "";
        public boolean tomorrowWork = false;
        public Plan tomorrowPlan = null;
        /** 这一笔钱是从哪一刻开始累计的（跨零点的班次会往前推到班次开始） */
        public long fromMs = 0;
        /** fromMs 所在的自然日，用于「本月累计」避免重复计一天 */
        public long fromDayMs = 0;
    }

    public static class Span {
        double s, e, bs, be, rate;   // 相对「今天 00:00」的秒；bs/be < 0 表示没有不带薪休息
    }

    // ------------------------------------------------------------ 排班解析

    public static Plan planFor(Prefs p, Calendar day) {
        Plan plan;
        if (p.mode == Prefs.MODE_FIXED) {
            plan = p.isFixedWorkday(day.get(Calendar.DAY_OF_WEEK)) ? Plan.parse(p.fixedPlan()) : null;
        } else {
            String[] days = Cycle.days(p.cycleForMode());
            if (days.length == 0) plan = null;
            else plan = Plan.parse(days[Cycle.index(p, day)]);
        }
        if (p.overrideMode != 0 && p.overrideDate.equals(D.key(day))) {
            if (p.overrideMode == 1) plan = null;
            else if (plan == null) {
                Plan d = Plan.parse(Cycle.typicalWorkPlan(p));
                plan = d.work ? d : null;
            }
        }
        if (plan != null && !plan.work) plan = null;
        return plan;
    }

    static double shiftPay(Prefs p, Plan plan) {
        double paidHours = plan.paidMinutes() / 60.0;
        switch (p.salaryMode) {
            case Prefs.PAY_HOURLY:
                return p.amount * paidHours;
            case Prefs.PAY_MONTHLY:
                return p.monthDays > 0 ? p.amount / p.monthDays : 0;
            default:
                return p.amount;
        }
    }

    /**
     * 把一个 Plan 拆成计薪区间。一天最多两段：正常班 + 夜间值班。
     * 两段共用同一个 rate（整天还是按一个班次的钱算，平摊到所有计薪分钟上），
     * 所以 16h 中夜班 / 白班+值班 都是「一天的钱」，只是摊得更薄。
     */
    static void spanParts(Prefs p, Plan plan, double shiftSec, List<Span> out) {
        if (plan == null || !plan.work) return;
        double paid = plan.paidMinutes() * 60.0;
        if (paid <= 0) return;
        double rate = shiftPay(p, plan) / paid;

        Span a = new Span();
        a.s = plan.start * 60.0 + shiftSec;
        a.e = plan.end * 60.0 + shiftSec;
        a.bs = plan.bs >= 0 ? plan.bs * 60.0 + shiftSec : -1;
        a.be = plan.be >= 0 ? plan.be * 60.0 + shiftSec : -1;
        a.rate = rate;
        out.add(a);

        if (plan.work2 && plan.end2 > plan.start2) {
            Span b = new Span();
            b.s = plan.start2 * 60.0 + shiftSec;
            b.e = plan.end2 * 60.0 + shiftSec;
            b.bs = -1;
            b.be = -1;
            b.rate = rate;
            out.add(b);
        }
    }

    /** 今天这个自然日里所有会走的计薪区间（含昨天跨零点过来的那一截） */
    public static List<Span> spansOf(Prefs p, Calendar today) {
        List<Span> out = new ArrayList<Span>();
        Plan y = planFor(p, D.at(today, -1));
        if (y != null) {
            List<Span> yp = new ArrayList<Span>();
            spanParts(p, y, -86400, yp);
            for (Span sp : yp) if (sp.e > 0) out.add(sp);
        }
        Plan t = planFor(p, today);
        if (t != null) spanParts(p, t, 0, out);
        Collections.sort(out, new Comparator<Span>() {
            public int compare(Span a, Span b) {
                return Double.compare(a.s, b.s);
            }
        });
        return out;
    }

    static double paidIn(Span sp, double from, double to) {
        double a = Math.max(sp.s, from);
        double b = Math.min(sp.e, to);
        if (b <= a) return 0;
        double v = b - a;
        if (sp.bs >= 0 && sp.be > sp.bs) {
            double ba = Math.max(sp.bs, a), bb = Math.min(sp.be, b);
            if (bb > ba) v -= (bb - ba);
        }
        return Math.max(0, v);
    }

    // ---------------------------------------------------------------- 主计算

    public static Snapshot compute(Prefs p, long nowMs) {
        Snapshot r = new Snapshot();
        r.nowMs = nowMs;
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMs);
        Calendar today = D.at(now, 0);
        double t = (nowMs - today.getTimeInMillis()) / 1000.0;
        if (t < 0) t = 0;

        r.clock = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        r.weekText = D.WEEK[now.get(Calendar.DAY_OF_WEEK) - 1];

        Plan todayPlan = planFor(p, today);
        r.todayPlan = todayPlan;
        if (todayPlan != null) {
            r.hasShift = true;
            r.shiftStart = todayPlan.start;
            r.shiftEnd = todayPlan.end;
            r.shiftHours = todayPlan.paidMinutes() / 60.0;
            r.shiftText = planText(todayPlan);
        }

        // 明天的排班：小部件上常驻一行，让用户知道明天要不要早起
        Plan tomPlan = planFor(p, D.at(today, 1));
        r.tomorrowPlan = tomPlan;
        r.tomorrowWork = tomPlan != null;
        r.tomorrowText = "明日 " + planText(tomPlan);
        r.tomorrowShort = "明日 " + (tomPlan == null ? "休息"
                : tomPlan.label() + (tomPlan.work2 ? "+值" : ""));

        List<Span> spans = spansOf(p, today);

        // 跨零点的班次（中夜 16:00–次日 08:00）不能因为零点就归零：
        // 找出「当前这笔钱从什么时候开始攒」——最后一个还没断档的连续工作块。
        double fromSec = 0;
        double prevEnd = -1e18;
        boolean started = false;
        for (Span sp : spans) {
            if (sp.s > t) break;
            if (!started || sp.s - prevEnd > 6 * 3600) fromSec = Math.min(0, sp.s);
            prevEnd = Math.max(prevEnd, sp.e);
            started = true;
        }
        r.fromMs = today.getTimeInMillis() + (long) (fromSec * 1000);
        r.fromDayMs = D.at(today, fromSec < 0 ? -1 : 0).getTimeInMillis();

        double paidFull = 0, paidElapsed = 0, fullDayPay = 0, earned = 0;
        for (Span sp : spans) {
            paidFull += paidIn(sp, fromSec, 86400);
            double pe = paidIn(sp, fromSec, t);
            paidElapsed += pe;
            earned += pe * sp.rate;
            fullDayPay += paidIn(sp, fromSec, 86400) * sp.rate;
        }
        r.paidSeconds = paidFull;
        r.paidElapsed = Math.min(paidElapsed, paidFull);
        r.fullDayPay = fullDayPay;
        r.progress = paidFull > 0 ? Math.min(1.0, paidElapsed / paidFull) : 0;

        if (spans.isEmpty()) {
            r.state = ST_REST;
            r.stateText = "休息日";
            r.hint = "今天不用当牛马，好好休息。";
            r.mini = "休息";
            r.earned = 0;
            r.speed = 0;
            return r;
        }

        Span first = spans.get(0), last = spans.get(spans.size() - 1);

        // ---- 状态机
        boolean inSpan = false, inBreak = false, beforeAny = t < first.s;
        Span cur = null;
        for (Span sp : spans) {
            if (t >= sp.s && t < sp.e) {
                inSpan = true;
                cur = sp;
                if (sp.bs >= 0 && t >= sp.bs && t < sp.be) inBreak = true;
                break;
            }
            if (t >= sp.e && t < last.e && spans.indexOf(sp) < spans.size() - 1) {
                Span nx = spans.get(spans.indexOf(sp) + 1);
                if (t < nx.s) {
                    r.state = ST_BETWEEN;
                    r.stateText = "中间休息";
                    r.hint = "距离下一班还有 " + D.cn(nx.s - t, true);
                    r.mini = "休息中";
                    r.secondsToNext = nx.s - t;
                    r.speed = 0;
                    r.earned = earned;
                    r.hourlyPay = 0;
                    return r;
                }
            }
        }

        if (beforeAny) {
            r.state = ST_BEFORE;
            r.stateText = "还没上班";
            r.hint = "距离上班还有 " + D.cn(first.s - t, true);
            r.mini = "未上班";
            r.secondsToNext = first.s - t;
            r.speed = 0;
            r.earned = earned;
            r.hourlyPay = first.rate * 3600;
            return r;
        }

        if (inSpan && cur != null) {
            if (inBreak) {
                boolean lunch = isLunch(cur, t);
                r.state = ST_BREAK;
                r.stateText = lunch ? "午休中" : "休息中";
                r.hint = (lunch ? "午休还剩 " : "不带薪休息还剩 ") + D.cn(cur.be - t, true) + "，暂停计薪";
                r.mini = lunch ? "午休" : "休息";
                r.secondsToNext = cur.be - t;
                r.speed = 0;
                r.earned = earned;
                r.hourlyPay = cur.rate * 3600;
            } else {
                r.state = ST_WORK;
                r.stateText = "上班中";
                r.hint = "距离下班还有 " + D.cn(cur.e - t, true);
                r.mini = D.hms(cur.e - t) + " 后下班";
                r.secondsToNext = cur.e - t;
                r.speed = cur.rate;
                r.earned = earned;
                r.hourlyPay = cur.rate * 3600;
            }
            return r;
        }

        // ---- 已经过了今天的最后一班
        if (p.overtime && last.e <= 86400 && t >= last.e) {
            r.state = ST_OT;
            r.overtimeSeconds = t - last.e;
            r.speed = last.rate * p.overtimeRate;
            r.earned = earned + r.overtimeSeconds * r.speed;
            r.stateText = "加班中";
            r.hint = "已加班 " + D.cn(r.overtimeSeconds, true) + "，按 " + fmtRate(p.overtimeRate) + " 倍计";
            r.mini = "加班中";
            r.hourlyPay = r.speed * 3600;
            return r;
        }

        r.state = ST_AFTER;
        r.stateText = "已下班";
        r.hint = "今天的牛马已下线，快跑！";
        r.mini = "已下班";
        r.speed = 0;
        r.earned = earned;
        r.hourlyPay = last.rate * 3600;
        return r;
    }

    /** 一天的排班写成一句人话：白班 08:00–20:00 / 中夜 16:00–次日08:00 / 白班 08:00–16:00 +值班 18:00–22:00 */
    public static String planText(Plan plan) {
        if (plan == null || !plan.work) return "休息";
        String s = plan.label() + " " + Plan.range(plan.start, plan.end);
        if (plan.work2) s += " +值班 " + Plan.range(plan.start2, plan.end2);
        return s;
    }

    static boolean isLunch(Span sp, double t) {        // 12:00–14:00 之间开始的不带薪休息，按「午休」叫
        double local = sp.bs;
        double m = ((local % 86400) + 86400) % 86400;
        return m >= 11 * 3600 && m <= 14 * 3600;
    }

    static String fmtRate(double d) {
        if (Math.abs(d - Math.round(d)) < 1e-9) return String.valueOf((long) Math.round(d));
        return String.valueOf(d);
    }

    // ------------------------------------------------------------ 统计

    /** 某一天「上满」能拿多少 */
    public static double dayPay(Prefs p, Calendar day) {
        Plan plan = planFor(p, day);
        return plan == null ? 0 : shiftPay(p, plan);
    }

    public static double monthEarned(Prefs p, Snapshot r) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(r.nowMs);
        Calendar fromDay = Calendar.getInstance();
        fromDay.setTimeInMillis(r.fromDayMs != 0 ? r.fromDayMs : D.at(now, 0).getTimeInMillis());
        Calendar c = (Calendar) D.at(now, 0).clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        double sum = 0;
        while (D.daysBetween(c, fromDay) > 0) {
            sum += dayPay(p, c);
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return sum + r.earned;
    }

    public static double monthTotal(Prefs p, long nowMs) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMs);
        int days = now.getActualMaximum(Calendar.DAY_OF_MONTH);
        double sum = 0;
        for (int i = 1; i <= days; i++) {
            Calendar c = (Calendar) now.clone();
            c.set(Calendar.DAY_OF_MONTH, i);
            sum += dayPay(p, D.at(c, 0));
        }
        return sum;
    }

    public static int daysToPayDay(Prefs p, long nowMs) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMs);
        Calendar today = D.at(now, 0);
        int pd = Math.max(1, Math.min(31, p.payDay));
        Calendar target = (Calendar) today.clone();
        target.set(Calendar.DAY_OF_MONTH, Math.min(pd, today.getActualMaximum(Calendar.DAY_OF_MONTH)));
        if (target.before(today)) {
            Calendar nm = (Calendar) today.clone();
            nm.add(Calendar.MONTH, 1);
            nm.set(Calendar.DAY_OF_MONTH, Math.min(pd, nm.getActualMaximum(Calendar.DAY_OF_MONTH)));
            target = nm;
        }
        return (int) Math.round(D.daysBetween(today, D.at(target, 0)));
    }

    /** 心愿单文案，逻辑同原项目 WishText */
    public static String wishText(Prefs p, Snapshot r) {
        List<String[]> items = p.wishItems();
        if (items.isEmpty()) return "";
        String best = null, next = null;
        double nextPrice = 0;
        for (String[] it : items) {
            double price = Double.parseDouble(it[1]);
            if (price <= r.earned) best = it[0];
            else {
                next = it[0];
                nextPrice = price;
                break;
            }
        }
        String bestOfDay = null;
        for (String[] it : items)
            if (Double.parseDouble(it[1]) <= r.fullDayPay) bestOfDay = it[0];

        switch (r.state) {
            case ST_REST:
                return "休息日，花钱的事交给明天的自己。";
            case ST_BEFORE:
                return bestOfDay != null ? "今天搬完砖，能换" + bestOfDay + "。" : "今天也是为梦想窝囊的一天。";
            case ST_WORK:
            case ST_BREAK:
            case ST_OT:
                if (next != null && r.speed > 0) {
                    double need = (nextPrice - r.earned) / r.speed;
                    if (r.state == ST_OT || need <= Math.max(0, r.paidSeconds - r.paidElapsed))
                        return "再过 " + D.cn(need, false) + "，" + next + "就赚回来了。";
                }
                if (best != null) return "今天已经赚到" + best + "了，稳住。";
                if (next != null)
                    return "离" + next + "还差 " + p.currency + String.format("%.2f", nextPrice - r.earned) + "。";
                return "";
            default:
                return best != null ? "今天赚到了" + best + "，收工！" : "今天收工，辛苦了。";
        }
    }
}
