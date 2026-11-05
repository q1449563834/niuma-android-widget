package com.feiyuaq.niuma;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 分段选择器，替代原项目的按钮组 */
public class SegView extends LinearLayout {

    public interface OnSelect {
        void onSelect(int index);
    }

    private TextView[] items = new TextView[0];
    private int selected = 0;
    private OnSelect listener;

    public SegView(Context c) {
        super(c);
        setOrientation(HORIZONTAL);
        setBackground(Ui.round(0x14000000, 12, c));
        int pad = Ui.dp(c, 4);
        setPadding(pad, pad, pad, pad);
        setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void setOnSelect(OnSelect l) {
        listener = l;
    }

    public void setOptions(String[] opts, int select) {
        removeAllViews();
        items = new TextView[opts.length];
        Context c = getContext();
        for (int i = 0; i < opts.length; i++) {
            final int idx = i;
            TextView t = new TextView(c);
            t.setText(opts[i]);
            t.setTextSize(13);
            t.setGravity(Gravity.CENTER);
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            int ph = Ui.dp(c, 6), pv = Ui.dp(c, 8);
            t.setPadding(ph, pv, ph, pv);
            LayoutParams lp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            t.setLayoutParams(lp);
            t.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (selected == idx) return;
                    select(idx, true);
                }
            });
            items[i] = t;
            addView(t);
        }
        selected = select;
        paint();
    }

    public void select(int idx, boolean notify) {
        if (idx < 0 || idx >= items.length) return;
        selected = idx;
        paint();
        if (notify && listener != null) listener.onSelect(idx);
    }

    public int selected() {
        return selected;
    }

    private void paint() {
        Context c = getContext();
        for (int i = 0; i < items.length; i++) {
            boolean on = i == selected;
            items[i].setTextColor(on ? 0xFFFFFBF0 : Ui.SUB);
            items[i].setBackground(on ? Ui.round(Ui.BLUE, 9, c) : null);
        }
    }
}
