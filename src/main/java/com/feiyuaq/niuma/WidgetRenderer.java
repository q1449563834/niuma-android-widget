package com.feiyuaq.niuma;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;

import java.util.Locale;

/**
 * 小部件位图渲染：1x4 和 2x4 共用一套绘制逻辑，按逻辑尺寸等比缩放，
 * 这样任意桌面格子尺寸都能画得下，不会出现截断。
 *
 * 数字沿用原项目 Odometer 的「机械滚轮」算法：每一位独立取连续值，
 * 该位每秒变化不超过 12 个数时，在进位前 25% 用缓动曲线滚上去。
 */
public class WidgetRenderer {

    /** 4 列逻辑宽 */
    public static final int LOG_W_L = 344;
    /** 2 列逻辑宽 */
    public static final int LOG_W_S = 176;
    public static final int LOG_H1 = 88;
    public static final int LOG_H2 = 172;

    /** 某个格子组合的逻辑宽高：4 列 344 / 2 列 176，1 行 88 / 2 行 172 */
    static int logW(int cols) {
        return cols <= 2 ? LOG_W_S : LOG_W_L;
    }

    static int logH(int rows) {
        return rows == 1 ? LOG_H1 : LOG_H2;
    }

    private static final int PAPER = 0xFFFBF7EA;
    private static final int INK = 0xFF2A2925;
    private static final int SUB = 0xFF6B665A;
    private static final int CARROT = 0xFFD2541E;
    private static final int LEAF = 0xFF4E7E32;
    private static final int BLUE = 0xFF2D5592;
    private static final int GRAY = 0xFF8A8579;

    static final Typeface CN = Typeface.create("sans-serif", Typeface.NORMAL);
    static final Typeface CNB = Typeface.create("sans-serif", Typeface.BOLD);
    static final Typeface NUM = Typeface.MONOSPACE;

    public static int colorFor(int state) {
        switch (state) {
            case Engine.ST_WORK: return BLUE;
            case Engine.ST_OT: return CARROT;
            case Engine.ST_BEFORE: return GRAY;
            case Engine.ST_BETWEEN: return GRAY;
            default: return LEAF;
        }
    }

    /** 渲染并画出整块小部件。返回的 Bitmap 已经是最终尺寸。 */
    public static void draw(Canvas canvas, int wPx, int hPx, int cols, int rows, Prefs p,
                            Engine.Snapshot s, float density) {
        int W = logW(cols), H = logH(rows);
        Paint pa = new Paint(Paint.ANTI_ALIAS_FLAG);

        // 卡片底：暖纸色 + 1px 描边。
        // 必须完全不透明（alpha 255）：HyperOS / MIUI 上小部件是贴在壁纸上的，
        // 半透明会让卡片发灰、边缘透出壁纸，用户看到的就是「透明显示异常」。
        // 2×2 单独画成正方形：桌面一格不是正方形（K70 上约 106dp×91dp），
        // 2×2 的格子位是 212×182 这种扁矩形，卡片跟着铺满就会显得「不够正」。
        // 这里按短边取正方形居中，两侧留透明（和淘宝淘金币 2×2 的观感一致）。
        RectF card;
        boolean squareCard = (cols == 2 && rows == 2);
        if (squareCard) {
            float side = Math.min(wPx, hPx);
            float left = (wPx - side) / 2f, top = (hPx - side) / 2f;
            card = new RectF(left + 0.6f, top + 0.6f, left + side - 0.6f, top + side - 0.6f);
        } else {
            card = new RectF(0.6f, 0.6f, wPx - 0.6f, hPx - 0.6f);
        }
        pa.setColor(PAPER);
        pa.setAlpha(255);
        float rad = Math.min(card.width(), card.height()) * 0.17f;
        canvas.drawRoundRect(card, rad, rad, pa);
        pa.setAlpha(255);
        pa.setStyle(Paint.Style.STROKE);
        pa.setStrokeWidth(Math.max(1f, density * 0.5f));
        pa.setColor(0x33000000);
        canvas.drawRoundRect(card, rad, rad, pa);
        pa.setStyle(Paint.Style.FILL);

        // 内容按卡片区域等比缩放并居中（2×2 时就是按那个正方形来算，文字不会溢到透明区外）
        float sc = Math.min(card.width() / (W * density), card.height() / (H * density));
        if (sc < 0.45f) sc = 0.45f;

        canvas.save();
        canvas.translate(card.centerX() - W * density * sc / 2f,
                card.centerY() - H * density * sc / 2f);
        canvas.scale(sc * density, sc * density);
        if (cols == 4 && rows == 1) draw1x4(canvas, p, s);
        else if (cols == 4) draw2x4(canvas, p, s);
        else if (rows == 1) draw1x2(canvas, p, s);
        else draw2x2(canvas, p, s);
        canvas.restore();
    }

    // ------------------------------------------------------------ 1x4

    private static void draw1x4(Canvas c, Prefs p, Engine.Snapshot s) {
        final float W = LOG_W_L, H = LOG_H1, pad = 16f;
        int col = colorFor(s.state);
        Paint pa = new Paint(Paint.ANTI_ALIAS_FLAG);
        pa.setSubpixelText(true);

        // 左侧状态竖条
        pa.setColor(col);
        RectF bar = new RectF(pad, 16f, pad + 3.5f, H - 16f);
        c.drawRoundRect(bar, 2f, 2f, pa);

        float x = pad + 13f;
        text(c, pa, s.stateText, x, 32f, 11.5f, col, CNB, W - pad, Paint.Align.LEFT);
        float stateW = measure(pa, s.stateText, 11.5f, CNB);
        // 已下班/休息日这类状态里 mini 和 stateText 是同一个词，避免画成「已下班 已下班」
        String tail = (s.mini == null || s.mini.equals(s.stateText)) ? "" : "  " + s.mini;
        if (tail.length() > 0) text(c, pa, tail, x + stateW, 32f, 10.5f, SUB, CN, W - pad, Paint.Align.LEFT);

        // 右侧百分比
        String pct = (int) Math.round(s.progress * 100) + "%";
        text(c, pa, pct, W - pad, 32f, 10f, SUB, NUM, W - pad, Paint.Align.RIGHT);

        // 金额
        if (p.masked) {
            text(c, pa, p.currency + " ****.**", x, 68f, 30f, CARROT, NUM, W - pad, Paint.Align.LEFT);
            drawTomorrow(c, pa, s.tomorrowText, W - pad, 68f, 9f, x + 150f);
        } else {
            float baseY = 68f;
            float numSize = 30f;
            float curW = text(c, pa, p.currency, x, baseY, 13f, SUB, CN, W, Paint.Align.LEFT);
            float mw = drawMoney(c, pa, s.earned, p.decimals, s.speed, x + curW, baseY, numSize);
            drawTomorrow(c, pa, s.tomorrowText, W - pad, baseY, 9f, x + curW + mw + 8f);
        }

        // 进度条
        if (s.paidSeconds > 0) {
            float by = H - 13f, bh = 3.6f;
            pa.setColor(0x22000000);
            c.drawRoundRect(new RectF(x, by, W - pad, by + bh), bh / 2, bh / 2, pa);
            float fw = (W - pad - x) * (float) s.progress;
            if (fw > 0.5f) {
                pa.setAlpha(255);
                pa.setShader(new LinearGradient(x, 0, W - pad, 0, 0xFF5E9A3A, col, Shader.TileMode.CLAMP));
                c.drawRoundRect(new RectF(x, by, x + fw, by + bh), bh / 2, bh / 2, pa);
                pa.setShader(null);
            }
        }
    }

    // ------------------------------------------------------------ 2x4

    private static void draw2x4(Canvas c, Prefs p, Engine.Snapshot s) {
        final float W = LOG_W_L, H = LOG_H2, pad = 16f;
        int col = colorFor(s.state);
        Paint pa = new Paint(Paint.ANTI_ALIAS_FLAG);
        pa.setSubpixelText(true);

        // 状态胶囊
        float pillH = 22f, pillY = 14f;
        float tw = measure(pa, s.stateText, 11f, CNB);
        float pillW = tw + 20f;
        pa.setColor(col);
        pa.setAlpha(0x22);
        c.drawRoundRect(new RectF(pad, pillY, pad + pillW, pillY + pillH), pillH / 2, pillH / 2, pa);
        pa.setAlpha(255);
        text(c, pa, s.stateText, pad + 10f, pillY + 15f, 11f, col, CNB, W, Paint.Align.LEFT);

        // 右上角时间
        String tm = s.clock + " " + s.weekText;
        text(c, pa, tm, W - pad, pillY + 15f, 10f, GRAY, CN, W, Paint.Align.RIGHT);

        // 「今日已赚」+ 班次
        text(c, pa, "今日已赚", pad, 60f, 10f, GRAY, CN, W, Paint.Align.LEFT);
        if (s.hasShift) {
            text(c, pa, shiftRange(s), W - pad, 60f, 9.5f, GRAY, CN, W, Paint.Align.RIGHT);
        }

        // 金额
        if (p.masked) {
            text(c, pa, p.currency + " ****.**", pad, 106f, 40f, CARROT, NUM, W, Paint.Align.LEFT);
        } else {
            float curW = text(c, pa, p.currency, pad, 106f, 15f, SUB, CN, W, Paint.Align.LEFT);
            drawMoney(c, pa, s.earned, p.decimals, s.speed, pad + curW, 106f, 40f);
        }

        // 倒计时（左侧）+ 明日排班（右侧）
        String hint = s.hint;
        if (hint.length() > 30) hint = hint.substring(0, 30) + "…";
        float tomW = s.tomorrowText == null ? 0 : measure(pa, s.tomorrowText, 9.5f, CN);
        text(c, pa, hint, pad, 131f, 11f, INK, CN, W - pad - tomW - 12f, Paint.Align.LEFT);
        drawTomorrow(c, pa, s.tomorrowText, W - pad, 131f, 9.5f, pad);

        // 进度条
        float by = 141f, bh = 5f;
        pa.setColor(0x22000000);
        c.drawRoundRect(new RectF(pad, by, W - pad, by + bh), bh / 2, bh / 2, pa);
        float fw = (W - 2 * pad) * (float) s.progress;
        if (fw > 0.5f) {
            pa.setAlpha(255);
            pa.setShader(new LinearGradient(pad, 0, W - pad, 0, 0xFF5E9A3A, col, Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(pad, by, pad + fw, by + bh), bh / 2, bh / 2, pa);
            pa.setShader(null);
        }

        // 底部信息
        String l = "时薪 " + p.currency + fmtMoney(s.hourlyPay, 2)
                + " · 今日满 " + p.currency + fmtMoney(s.fullDayPay, 2);
        text(c, pa, l, pad, 164f, 9.5f, SUB, CN, W, Paint.Align.LEFT);
        int pd = Engine.daysToPayDay(p, s.nowMs);
        String r = pd == 0 ? "今天发工资！" : "距发薪 " + pd + " 天";
        text(c, pa, r, W - pad, 164f, 9.5f, SUB, CN, W, Paint.Align.RIGHT);
    }

    // ------------------------------------------------------------ 1x2（2 列 1 行）

    private static void draw1x2(Canvas c, Prefs p, Engine.Snapshot s) {
        final float W = LOG_W_S, H = LOG_H1, pad = 12f;
        int col = colorFor(s.state);
        Paint pa = new Paint(Paint.ANTI_ALIAS_FLAG);
        pa.setSubpixelText(true);

        // 左侧状态竖条
        pa.setColor(col);
        RectF bar = new RectF(pad, 13f, pad + 3f, H - 13f);
        c.drawRoundRect(bar, 1.5f, 1.5f, pa);

        float x = pad + 10f;
        text(c, pa, s.stateText, x, 26f, 10.5f, col, CNB, W - pad - 32f, Paint.Align.LEFT);
        String pct = (int) Math.round(s.progress * 100) + "%";
        text(c, pa, pct, W - pad, 26f, 9f, SUB, NUM, W - pad, Paint.Align.RIGHT);

        // 明日排班（这一格太小，只放「明日 白班 / 明日 休息」这种短说法）
        text(c, pa, s.tomorrowShort, x, 41f, 8.5f, SUB, CN, W - pad, Paint.Align.LEFT);

        // 金额：宽度会先按实际位数缩字号，保证 ¥1234.56789 也不会超出卡片
        if (p.masked) {
            text(c, pa, p.currency + " ****.**", x, 66f, 20f, CARROT, NUM, W - pad, Paint.Align.LEFT);
        } else {
            float curW = text(c, pa, p.currency, x, 66f, 10f, SUB, CN, W, Paint.Align.LEFT);
            float avail = W - pad - (x + curW + 1f);
            float numSize = fitSize(pa, s.earned, p.decimals, avail, 21f);
            drawMoney(c, pa, s.earned, p.decimals, s.speed, x + curW + 1f, 66f, numSize);
        }

        // 进度条
        if (s.paidSeconds > 0) {
            float by = H - 10f, bh = 3.2f;
            pa.setColor(0x22000000);
            c.drawRoundRect(new RectF(x, by, W - pad, by + bh), bh / 2, bh / 2, pa);
            float fw = (W - pad - x) * (float) s.progress;
            if (fw > 0.5f) {
                pa.setAlpha(255);
                pa.setShader(new LinearGradient(x, 0, W - pad, 0, 0xFF5E9A3A, col, Shader.TileMode.CLAMP));
                c.drawRoundRect(new RectF(x, by, x + fw, by + bh), bh / 2, bh / 2, pa);
                pa.setShader(null);
            }
        }
    }

    // ------------------------------------------------------------ 2x2（2 列 2 行）

    private static void draw2x2(Canvas c, Prefs p, Engine.Snapshot s) {
        final float W = LOG_W_S, H = LOG_H2, pad = 12f;
        int col = colorFor(s.state);
        Paint pa = new Paint(Paint.ANTI_ALIAS_FLAG);
        pa.setSubpixelText(true);

        // 状态胶囊 + 右上角时间
        float pillH = 20f, pillY = 12f;
        float tw = measure(pa, s.stateText, 10.5f, CNB);
        float pillW = tw + 16f;
        pa.setColor(col);
        pa.setAlpha(0x22);
        c.drawRoundRect(new RectF(pad, pillY, pad + pillW, pillY + pillH), pillH / 2, pillH / 2, pa);
        pa.setAlpha(255);
        text(c, pa, s.stateText, pad + 8f, pillY + 14f, 10.5f, col, CNB, W, Paint.Align.LEFT);
        text(c, pa, s.clock, W - pad, pillY + 14f, 9f, GRAY, CN, W, Paint.Align.RIGHT);

        // 「今日已赚」+ 班次
        text(c, pa, "今日已赚", pad, 52f, 9f, GRAY, CN, W, Paint.Align.LEFT);
        if (s.hasShift) {
            text(c, pa, shiftRange(s), W - pad, 52f, 8f, GRAY, CN, W, Paint.Align.RIGHT);
        }

        // 金额
        if (p.masked) {
            text(c, pa, p.currency + " ****.**", pad, 90f, 24f, CARROT, NUM, W - pad, Paint.Align.LEFT);
        } else {
            float curW = text(c, pa, p.currency, pad, 90f, 11f, SUB, CN, W, Paint.Align.LEFT);
            float avail = W - 2 * pad - (curW + 1f);
            float numSize = fitSize(pa, s.earned, p.decimals, avail, 30f);
            drawMoney(c, pa, s.earned, p.decimals, s.speed, pad + curW + 1f, 90f, numSize);
        }

        // 倒计时（最多两行）
        twoLine(c, pa, s.hint, pad, 112f, 14f, 9.5f, INK, CN, W - 2 * pad);

        // 进度条
        float by = 132f, bh = 4.5f;
        pa.setColor(0x22000000);
        c.drawRoundRect(new RectF(pad, by, W - pad, by + bh), bh / 2, bh / 2, pa);
        float fw = (W - 2 * pad) * (float) s.progress;
        if (fw > 0.5f) {
            pa.setAlpha(255);
            pa.setShader(new LinearGradient(pad, 0, W - pad, 0, 0xFF5E9A3A, col, Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(pad, by, pad + fw, by + bh), bh / 2, bh / 2, pa);
            pa.setShader(null);
        }

        // 明日排班
        drawTomorrow(c, pa, s.tomorrowText, W - pad, 147f, 8.5f, pad);

        // 底部：时薪 + 发薪日
        text(c, pa, "时薪 " + p.currency + fmtMoney(s.hourlyPay, 2), pad, 161f, 8.5f, SUB, CN,
                W - pad - 60f, Paint.Align.LEFT);
        int pd = Engine.daysToPayDay(p, s.nowMs);
        String r = pd == 0 ? "今天发工资！" : "距发薪 " + pd + " 天";
        text(c, pa, r, W - pad, 161f, 8.5f, SUB, CN, W, Paint.Align.RIGHT);
    }

    // ------------------------------------------------------------ 工具

    /** 按金额位数反算字号：位数多的时候自动缩小，保证不超出 maxW */
    static float fitSize(Paint p, double value, int decimals, float maxW, float base) {
        decimals = Math.max(0, Math.min(6, decimals));
        long ip = (long) Math.floor(Math.max(0, value));
        int digits = Math.max(1, String.valueOf(ip).length()) + decimals;
        float size = base;
        while (size > 8f) {
            p.setTextSize(size);
            p.setTypeface(NUM);
            float cellW = p.measureText("0");
            float w = cellW * digits + (decimals > 0 ? cellW * 0.38f : 0);
            if (w <= maxW) break;
            size -= 0.5f;
        }
        return size;
    }

    /** 窄卡片上的两行文字：先按宽度贪心折行，第二行还超宽就截断加省略号 */
    static void twoLine(Canvas c, Paint p, String s, float x, float y, float lineH,
                        float size, int color, Typeface tf, float maxW) {
        if (s == null || s.length() == 0) return;
        p.setTextSize(size);
        p.setTypeface(tf);
        p.setTextAlign(Paint.Align.LEFT);
        p.setColor(color);

        int fit = 1;
        while (fit < s.length() && p.measureText(s.substring(0, fit)) <= maxW) fit++;
        if (fit >= s.length()) {
            c.drawText(s, x, y, p);
            return;
        }

        int cut = Math.max(1, fit - 1);
        String line1 = s.substring(0, cut);
        String line2 = s.substring(cut);
        boolean clipped = false;
        while (line2.length() > 1 && p.measureText(line2 + "…") > maxW) {
            line2 = line2.substring(0, line2.length() - 1);
            clipped = true;
        }
        if (clipped) line2 = line2 + "…";
        c.drawText(line1, x, y, p);
        c.drawText(line2, x, y + lineH, p);
    }

    static String fmtMoney(double v, int dec) {
        return String.format(Locale.US, "%." + dec + "f", Math.max(0, v));
    }

    static float measure(Paint p, String s, float size, Typeface tf) {
        p.setTextSize(size);
        p.setTypeface(tf);
        return p.measureText(s);
    }

    /** 画一段文字，返回宽度；align 决定 x 是左端还是右端 */
    static float text(Canvas c, Paint p, String s, float x, float y, float size,
                      int color, Typeface tf, float limitX, Paint.Align align) {
        p.setTextSize(size);
        p.setTypeface(tf);
        p.setColor(color);
        p.setTextAlign(align);
        float w = p.measureText(s);
        if (limitX > 0 && x + w > limitX && align == Paint.Align.LEFT) {
            while (s.length() > 1 && p.measureText(s + "…") > (limitX - x)) s = s.substring(0, s.length() - 1);
            s = s + "…";
            w = p.measureText(s);
        }
        c.drawText(s, x, y, p);
        return w;
    }

    /** 班次时段；有第二段夜间值班时加个 +值 提示 */
    static String shiftRange(Engine.Snapshot s) {
        String t = Plan.range(s.shiftStart, s.shiftEnd);
        if (s.todayPlan != null && s.todayPlan.work2) t += " +值";
        return t;
    }

    /** 右对齐文字，leftmost 是最左允许到的位置；放不下就从尾巴开始截断加省略号 */
    static void textRight(Canvas c, Paint p, String s, float rightX, float y, float size,
                          int color, Typeface tf, float leftmost) {
        if (s == null || s.length() == 0) return;
        p.setTextSize(size);
        p.setTypeface(tf);
        p.setColor(color);
        p.setTextAlign(Paint.Align.RIGHT);
        float avail = rightX - leftmost;
        boolean cut = false;
        while (s.length() > 1 && p.measureText(s + (cut ? "…" : "")) > avail) {
            s = s.substring(0, s.length() - 1);
            cut = true;
        }
        c.drawText(cut ? s + "…" : s, rightX, y, p);
    }

    /** 画「明日 白班 08:00–20:00」这类明日排班信息，右侧对齐、前面留够空隙 */
    static void drawTomorrow(Canvas c, Paint p, String txt, float rightX, float y,
                             float size, float leftmost) {
        if (txt == null || txt.length() == 0) return;
        textRight(c, p, txt, rightX, y, size, SUB, CN, leftmost);
    }

    /** 机械滚轮数字，返回整段金额的宽度 */
    static float drawMoney(Canvas c, Paint p, double value, int decimals, double speed,
                           float x, float baseY, float size) {
        if (value < 0) value = 0;
        decimals = Math.max(0, Math.min(6, decimals));
        p.setTextSize(size);
        p.setTypeface(NUM);
        p.setColor(CARROT);

        long ip = (long) Math.floor(value);
        int intDigits = Math.max(1, String.valueOf(ip).length());
        float cellW = p.measureText("0");
        // 滚轮的格高按字面高（约 0.71 字号）来定，不能用字体行高，
        // 否则下一格数字会串进可视区，看着像重影。
        float cellH = size * 0.76f;
        float dotW = decimals > 0 ? cellW * 0.38f : 0;

        int n = intDigits + decimals;
        double scaled = value * Math.pow(10, decimals);
        double scaledSpeed = Math.max(0, speed) * Math.pow(10, decimals);

        for (int i = 0; i < n; i++) {
            int k = n - 1 - i;
            double p10 = Math.pow(10, k);
            double v = scaled / p10 + 1e-7;
            double fl = Math.floor(v);
            int digit = (int) (fl % 10);
            double f = Math.max(0, v - fl - 1e-7);
            double rate = scaledSpeed / p10;
            double offset = digit;
            if (rate > 0 && rate <= 12) {
                double w = Math.min(1.0, rate * 0.25);
                double pp = (f - (1 - w)) / w;
                if (pp > 0) {
                    pp = Math.min(1, pp);
                    offset += pp * pp * (3 - 2 * pp);
                }
            }
            float x0 = x + cellW * i + (i >= intDigits ? dotW : 0);
            float cx = x0 + cellW / 2f;
            c.save();
            c.clipRect(x0, baseY - cellH * 0.97f, x0 + cellW, baseY + cellH * 0.18f);
            p.setTextAlign(Paint.Align.CENTER);
            // offset ∈ [digit, digit+1)：当前数字向上滚出，下一位从下方滚入
            drawDigit(c, p, digit, cx, baseY + (float) (digit - offset) * cellH);
            drawDigit(c, p, (digit + 1) % 10, cx, baseY + (float) (digit + 1 - offset) * cellH);
            c.restore();

            if (i == intDigits - 1 && decimals > 0) {
                p.setTextAlign(Paint.Align.LEFT);
                p.setColor(0xFF6B665A);
                p.setTextSize(size * 0.85f);
                c.drawText(".", x0 + cellW - dotW * 0.15f, baseY, p);
                p.setColor(CARROT);
                p.setTextSize(size);
            }
        }
        return cellW * n + dotW;
    }

    static void drawDigit(Canvas c, Paint p, int d, float cx, float y) {
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(String.valueOf(d), cx, y, p);
    }
}
