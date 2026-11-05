package com.feiyuaq.niuma;

/**
 * 一天的排班：休息，或者一段（可含不带薪休息）的上班区间。
 * 另外支持「同一天的夜间值班」——在白班/中班结束之后再上一段，最多两段。
 * 时间以「当天 0 点为 0」的分钟数计，可超过 1440 表示跨零点。
 */
public class Plan {
    public boolean work = false;
    public int start = 0, end = 0;      // 上班 / 下班（分钟）
    public int bs = -1, be = -1;        // 不带薪休息（午休等）

    /** 同一天的夜间值班（第二段），默认没有 */
    public boolean work2 = false;
    public int start2 = 0, end2 = 0;

    public static Plan rest() {
        return new Plan();
    }

    public static Plan of(int start, int end, int bs, int be) {
        Plan p = new Plan();
        p.work = true;
        p.start = start;
        p.end = end;
        if (be > bs) {
            p.bs = Math.max(start, bs);
            p.be = Math.min(end, be);
            if (p.be <= p.bs) {
                p.bs = -1;
                p.be = -1;
            }
        }
        return p;
    }

    /** 在当天这段班之后再加一段（夜间值班）。可以在下班后紧接着上，也可以隔一段时间 */
    public Plan plusNight(int s2, int e2) {
        work2 = true;
        start2 = s2;
        end2 = e2 <= s2 ? s2 + 1 : e2;
        return this;
    }

    /** "R" / "W,start,end,breakStart,breakEnd" / "W,start,end,bs,be,nightStart,nightEnd" */
    public static Plan parse(String s) {
        if (s == null || s.length() == 0 || s.charAt(0) == 'R') return rest();
        String[] a = s.split(",");
        try {
            int st = Integer.parseInt(a[1].trim());
            int en = Integer.parseInt(a[2].trim());
            int bs = a.length > 3 ? Integer.parseInt(a[3].trim()) : -1;
            int be = a.length > 4 ? Integer.parseInt(a[4].trim()) : -1;
            if (en <= st) en = st + 1;
            Plan p = of(st, en, bs, be);
            if (a.length >= 7) {
                int s2 = Integer.parseInt(a[5].trim());
                int e2 = Integer.parseInt(a[6].trim());
                if (e2 <= s2) e2 = s2 + 1;
                p.plusNight(s2, e2);
            }
            return p;
        } catch (Exception e) {
            return rest();
        }
    }

    public String encode() {
        if (!work) return "R";
        String s = "W," + start + "," + end + "," + bs + "," + be;
        if (work2) s += "," + start2 + "," + end2;
        return s;
    }

    public int paidMinutes() {
        int len = end - start;
        if (bs >= 0 && be > bs) len -= (be - bs);
        int total = Math.max(0, len);
        if (work2 && end2 > start2) total += (end2 - start2);
        return total;
    }

    public boolean crossesMidnight() {
        return work && (end > 1440 || (work2 && end2 > 1440));
    }

    public String timeText() {
        if (!work) return "休息";
        String s = range(start, end);
        if (bs >= 0) s += "（休 " + D.hm(bs) + "–" + D.hm(be) + "）";
        if (work2) s += " +值班 " + range(start2, end2);
        return s;
    }

    /** 一段区间的可读写法，24:00 不折成 00:00，跨零点的写成「次日」 */
    public static String range(int a, int b) {
        String left = D.hm(a);
        String right = b == 1440 ? "24:00" : (b > 1440 ? "次日" + D.hm(b) : D.hm(b));
        return left + "–" + right;
    }

    /** 给排班格子用的短标签 */
    public String label() {
        if (!work) return "休";
        int len = end - start;
        if (start == 0 && len <= 540) return "夜班";
        if (len >= 900 && start >= 840) return "中夜";
        if (len >= 660 && start <= 540) return "白班";
        if (start <= 540 && len <= 540) return "早班";
        if (start >= 840 && len <= 540) return "中班";
        if (bs >= 0 && start <= 540 && len <= 660) return "长白";
        return "自定";
    }

    public String shortLabel() {
        if (!work) return "休";
        int len = end - start;
        if (start == 0 && len <= 540) return "夜";
        if (len >= 900 && start >= 840) return "中夜";
        if (len >= 660 && start <= 540) return "白";
        if (start <= 540 && len <= 540) return "早";
        if (start >= 840 && len <= 540) return "中";
        return "班";
    }

    /** 带值班标记的短标签，给格子 / 小部件用 */
    public String gridLabel() {
        return shortLabel() + (work2 ? "·值" : "");
    }
}
