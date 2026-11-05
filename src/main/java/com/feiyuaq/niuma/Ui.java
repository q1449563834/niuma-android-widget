package com.feiyuaq.niuma;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 界面小工具：暖纸配色、圆角卡片、统一字号 */
public class Ui {
    public static final int PAPER = 0xFFF3ECD3;
    public static final int CARD = 0xFFFFFBF0;
    public static final int INK = 0xFF2A2925;
    public static final int SUB = 0xFF6B665A;
    public static final int CARROT = 0xFFD2541E;
    public static final int LEAF = 0xFF4E7E32;
    public static final int BLUE = 0xFF2D5592;
    public static final int GRAY = 0xFF8A8579;
    public static final int LINE = 0x1F000000;

    public static Typeface CN = Typeface.create("sans-serif", Typeface.NORMAL);
    public static Typeface CNB = Typeface.create("sans-serif", Typeface.BOLD);
    public static Typeface NUM = Typeface.MONOSPACE;

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    public static GradientDrawable round(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable round(int color, float radiusDp, int strokeColor, float strokeDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        d.setStroke(dp(c, strokeDp), strokeColor);
        return d;
    }

    public static TextView tv(Context c, String text, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(bold ? CNB : CN);
        t.setLineSpacing(dp(c, 2), 1f);
        return t;
    }

    public static TextView num(Context c, String text, float sp, int color, boolean bold) {
        TextView t = tv(c, text, sp, color, bold);
        t.setTypeface(NUM);
        return t;
    }

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }

    public static void margins(View v, Context c, float l, float t, float r, float b) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) lp).setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        }
    }

    public static View space(Context c, float h) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, h)));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 0.6f)));
        lp.topMargin = dp(c, 12);
        lp.bottomMargin = dp(c, 12);
        v.setLayoutParams(lp);
        v.setBackgroundColor(LINE);
        return v;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = col(c);
        l.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14));
        l.setBackground(round(CARD, 18, 0x14000000, 0.6f, c));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 12);
        l.setLayoutParams(lp);
        return l;
    }

    public static void bgTap(View v, Context c) {
        v.setBackground(round(0x00000000, 0, c));
        v.setClickable(true);
    }

    public static int stateColor(int state) {
        return WidgetRenderer.colorFor(state);
    }
}
