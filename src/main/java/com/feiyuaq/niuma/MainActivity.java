package com.feiyuaq.niuma;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {

    private Prefs p;
    private Engine.Snapshot snap;
    private long snapAt = 0;

    private ScrollView scroll;
    private LinearLayout content;

    private OdometerView odo;
    private TextView tvState, tvHint, tvClock, tvSpeed, tvProgress, tvHourly, tvMonth, tvMonthSub, tvQuote;
    private ProgressView bar;
    private SegView segOverride;
    private final WidgetPreview[] previews = new WidgetPreview[4];

    // 四种小部件：标签、列数、行数、provider
    private static final String[] W_LABEL = {"1×2 迷你条", "1×4 细条", "2×2 方卡", "2×4 完整卡"};
    private static final int[] W_COLS = {2, 4, 2, 4};
    private static final int[] W_ROWS = {1, 1, 2, 2};
    private static final String[] W_BTN = {
            "添加 1×2 迷你条到桌面", "添加 1×4 细条到桌面",
            "添加 2×2 方卡到桌面", "添加 2×4 完整卡到桌面"};

    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean running = false;

    private final Runnable frame = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            long now = System.currentTimeMillis();
            if (now - snapAt > 900) recompute();
            double shown = snap.earned;
            if (snap.speed > 0) shown += Math.max(0, (now - snapAt) / 1000.0) * snap.speed;
            if (odo != null) {
                odo.setMasked(p.masked);
                odo.setMoney(shown, snap.speed, p.decimals, p.currency, p.odometer);
            }
            ui.postDelayed(this, 16);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        p = Prefs.load(this);
        buildUi();
        TickerService.ensure(this);
        askNotification();
    }

    /** Android 13 起前台服务的通知要用户授权，不授权不影响计薪，只是通知栏看不到常驻提示 */
    private void askNotification() {
        if (Build.VERSION.SDK_INT < 33) return;
        try {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 9);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        p = Prefs.load(this);
        rebuild(true);
        running = true;
        ui.removeCallbacks(frame);
        ui.post(frame);
        TickerService.ensure(this);
    }

    @Override
    protected void onPause() {
        running = false;
        ui.removeCallbacks(frame);
        super.onPause();
    }

    // =============================================================== 界面

    private void buildUi() {
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.PAPER);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 16);
        content.setPadding(pad, Ui.dp(this, 18), pad, Ui.dp(this, 26));
        scroll.addView(content);
        setContentView(scroll);
        rebuild(false);
    }

    private void rebuild(boolean keepScroll) {
        int oldY = keepScroll ? scroll.getScrollY() : 0;
        content.removeAllViews();

        // 标题
        LinearLayout head = Ui.row(this);
        LinearLayout headTxt = Ui.col(this);
        headTxt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView title = Ui.tv(this, "牛马脱敏器", 23, Ui.INK, true);
        headTxt.addView(title);
        headTxt.addView(Ui.tv(this, "安卓小部件版 · 每一秒都在到账", 12, Ui.SUB, false));
        head.addView(headTxt);
        TextView ver = Ui.tv(this, "1.0", 11, Ui.GRAY, false);
        head.addView(ver);
        content.addView(head);
        content.addView(Ui.space(this, 14));

        content.addView(buildHero());
        content.addView(buildSchedule());
        content.addView(buildSalary());
        content.addView(buildWidgets());
        content.addView(buildDisplay());
        content.addView(buildAbout());

        recompute();
        if (keepScroll) scroll.post(new Runnable() {
            public void run() {
                scroll.scrollTo(0, oldY);
            }
        });
    }

    // ------------------------------------------------------------ 主卡

    private LinearLayout buildHero() {
        LinearLayout card = Ui.card(this);

        LinearLayout top = Ui.row(this);
        TextView dot = new TextView(this);
        dot.setWidth(Ui.dp(this, 9));
        dot.setHeight(Ui.dp(this, 9));
        dot.setBackground(Ui.round(Ui.LEAF, 5, this));
        tvDot = dot;
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.dp(this, 9), Ui.dp(this, 9));
        dlp.rightMargin = Ui.dp(this, 8);
        dot.setLayoutParams(dlp);
        tvState = Ui.tv(this, "休息日", 16, Ui.LEAF, true);
        top.addView(dot);
        top.addView(tvState);
        tvClock = Ui.tv(this, "", 11, Ui.GRAY, false);
        tvClock.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvClock.setLayoutParams(clp);
        top.addView(tvClock);
        card.addView(top);

        tvHint = Ui.tv(this, "", 12, Ui.SUB, false);
        Ui.margins(tvHint, this, 0, 6, 0, 12);
        card.addView(tvHint);

        card.addView(Ui.tv(this, "今日已赚", 11, Ui.GRAY, false));
        odo = new OdometerView(this);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 62));
        olp.topMargin = Ui.dp(this, 2);
        odo.setLayoutParams(olp);
        card.addView(odo);

        tvSpeed = Ui.tv(this, "", 11, Ui.SUB, false);
        Ui.margins(tvSpeed, this, 0, 2, 0, 10);
        card.addView(tvSpeed);

        LinearLayout prow = Ui.row(this);
        tvProgress = Ui.tv(this, "", 11, Ui.SUB, false);
        tvHours = Ui.num(this, "", 11, Ui.SUB, false);
        tvHours.setGravity(Gravity.RIGHT);
        prow.addView(tvProgress, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        prow.addView(tvHours);
        card.addView(prow);

        bar = new ProgressView(this);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 7));
        blp.topMargin = Ui.dp(this, 8);
        bar.setLayoutParams(blp);
        card.addView(bar);

        card.addView(Ui.divider(this));

        LinearLayout stats = Ui.row(this);
        stats.addView(stat("时薪", tvHourly = Ui.num(this, "—", 15, Ui.INK, true)), weight());
        stats.addView(stat("今日满", tvFull = Ui.num(this, "—", 15, Ui.INK, true)), weight());
        stats.addView(stat("本月累计", tvMonth = Ui.num(this, "—", 15, Ui.BLUE, true)), weight());
        card.addView(stats);
        tvMonthSub = Ui.tv(this, "", 10, Ui.GRAY, false);
        Ui.margins(tvMonthSub, this, 0, 8, 0, 0);
        card.addView(tvMonthSub);

        tvQuote = Ui.tv(this, "", 11, Ui.LEAF, false);
        tvQuote.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
        tvQuote.setGravity(Gravity.CENTER);
        Ui.margins(tvQuote, this, 4, 12, 4, 2);
        card.addView(tvQuote);

        Ui.margins(card, this, 0, 0, 0, 12);
        return card;
    }

    private View tvDot;
    private TextView tvHours, tvFull;

    private View stat(String label, TextView value) {
        LinearLayout l = Ui.col(this);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.addView(value);
        TextView lb = Ui.tv(this, label, 10, Ui.GRAY, false);
        lb.setGravity(Gravity.CENTER);
        l.addView(lb);
        return l;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    // ------------------------------------------------------------ 班制

    private LinearLayout buildSchedule() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionLabel("班制"));
        SegView seg = new SegView(this);
        seg.setOptions(new String[]{"长白班", "APN", "自定义循环"}, p.mode);
        seg.setOnSelect(new SegView.OnSelect() {
            public void onSelect(int index) {
                p.mode = index;
                if (index == Prefs.MODE_THREE && Cycle.days(p.threeCycle).length == 0)
                    p.threeCycle = p.apnCycle();
                if (index == Prefs.MODE_CYCLE && Cycle.days(p.customCycle).length == 0)
                    p.customCycle = Prefs.CYCLE16;
                save();
                rebuild(true);
            }
        });
        Ui.margins(seg, this, 0, 8, 0, 12);
        card.addView(seg);

        if (p.mode == Prefs.MODE_FIXED) buildFixedConfig(card);
        else buildCycleConfig(card);
        return card;
    }

    private void buildFixedConfig(LinearLayout card) {
        card.addView(timeRow(card, "上班时间", p.fixedStart, new SetInt() {
            public void set(int v) {
                p.fixedStart = v;
                save();
                rebuild(true);
            }
        }));
        card.addView(timeRow(card, "下班时间", p.fixedEnd, new SetInt() {
            public void set(int v) {
                p.fixedEnd = v;
                save();
                rebuild(true);
            }
        }));

        LinearLayout lr = switchRow("午休（不计薪）", p.lunchEnabled, new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean v) {
                p.lunchEnabled = v;
                save();
                rebuild(true);
            }
        });
        card.addView(lr);
        if (p.lunchEnabled) {
            card.addView(timeRow(card, "午休开始", p.lunchStart, new SetInt() {
                public void set(int v) {
                    p.lunchStart = v;
                    save();
                    rebuild(true);
                }
            }));
            card.addView(timeRow(card, "午休结束", p.lunchEnd, new SetInt() {
                public void set(int v) {
                    p.lunchEnd = v;
                    save();
                    rebuild(true);
                }
            }));
        }

        card.addView(Ui.space(this, 8));
        card.addView(Ui.tv(this, "固定上班日", 12, Ui.INK, true));
        LinearLayout wk = Ui.row(this);
        for (int i = 1; i <= 7; i++) {
            final int dow = i % 7;   // 1=周一 … 0=周日
            final boolean on = p.isFixedWorkday(dow);
            final TextView cell = new TextView(this);
            cell.setText(D.WEEK[dow].substring(1));
            cell.setTextSize(12);
            cell.setGravity(Gravity.CENTER);
            cell.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            cell.setTextColor(on ? 0xFFFFFBF0 : Ui.SUB);
            cell.setBackground(on ? Ui.round(Ui.LEAF, 8, this) : Ui.round(0x11000000, 8, this));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 34), 1f);
            lp.rightMargin = Ui.dp(this, 4);
            cell.setLayoutParams(lp);
            cell.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    p.workMask ^= (1 << dow);
                    save();
                    rebuild(true);
                }
            });
            wk.addView(cell);
        }
        Ui.margins(wk, this, 0, 8, 0, 2);
        card.addView(wk);
        card.addView(Ui.tv(this, "点一下切换工作日。默认周一至周五，午休 12:00–13:00。", 10, Ui.GRAY, false));
    }

    /** APN 三班倒：三个班的上下班时间都能改，改完按新时间重排 8 天循环 */
    private void buildApnTimes(LinearLayout card) {
        card.addView(Ui.tv(this, "三班时间", 12, Ui.INK, true));
        card.addView(timeRow(card, "白班上班", p.apnDayStart, apnSetter(0)));
        card.addView(timeRow(card, "白班下班", p.apnDayEnd, apnSetter(1)));
        card.addView(timeRow(card, "中班上班", p.apnMidStart, apnSetter(2)));
        card.addView(timeRow(card, "中班下班", p.apnMidEnd, apnSetter(3)));
        card.addView(timeRow(card, "夜班上班", p.apnNightStart, apnSetter(4)));
        card.addView(timeRow(card, "夜班下班", p.apnNightEnd, apnSetter(5)));
        card.addView(Ui.tv(this, "下班比上班早就是跨零点（如中班 16:00–00:00、夜班 20:00–04:00）。"
                + "改时间会按新时间重排整个循环，手动改过的格子会被覆盖。", 10, Ui.GRAY, false));
        card.addView(Ui.space(this, 10));
    }

    private SetInt apnSetter(final int which) {
        return new SetInt() {
            public void set(int v) {
                switch (which) {
                    case 0: p.apnDayStart = v; break;
                    case 1: p.apnDayEnd = v; break;
                    case 2: p.apnMidStart = v; break;
                    case 3: p.apnMidEnd = v; break;
                    case 4: p.apnNightStart = v; break;
                    default: p.apnNightEnd = v; break;
                }
                p.threeCycle = p.apnCycle();
                save();
                rebuild(true);
            }
        };
    }

    private void buildCycleConfig(LinearLayout card) {
        if (p.mode == Prefs.MODE_THREE) buildApnTimes(card);

        String[] days = Cycle.days(p.cycleForMode());
        int n = days.length;
        if (n == 0) {
            p.setCycleForMode(p.mode == Prefs.MODE_THREE ? p.apnCycle() : Prefs.CYCLE16);
            days = Cycle.days(p.cycleForMode());
            n = days.length;
            save();
        }

        double hours = Cycle.hoursPerCycle(p);
        int work = 0;
        for (String d : days) if (Plan.parse(d).work) work++;

        LinearLayout sum = Ui.col(this);
        sum.addView(Ui.tv(this, n + " 天一个循环 · " + work + " 个班 · " + fmt(hours, 1) + " 小时 / 周期", 12, Ui.INK, true));
        sum.addView(Ui.tv(this, "点任意一格改班次；循环第 1 天 = " + p.anchor, 10, Ui.GRAY, false));
        Ui.margins(sum, this, 0, 2, 0, 10);
        card.addView(sum);

        card.addView(dayGrid(days));

        LinearLayout ops = Ui.row(this);
        ops.addView(smallBtn("加一天", new View.OnClickListener() {
            public void onClick(View v) {
                String[] d = Cycle.days(p.cycleForMode());
                String[] d2 = new String[d.length + 1];
                System.arraycopy(d, 0, d2, 0, d.length);
                d2[d.length] = "R";
                p.setCycleForMode(Cycle.join(d2));
                save();
                rebuild(true);
            }
        }), weightBtn());
        ops.addView(smallBtn("减一天", new View.OnClickListener() {
            public void onClick(View v) {
                String[] d = Cycle.days(p.cycleForMode());
                if (d.length <= 2) {
                    toast("循环至少 2 天");
                    return;
                }
                String[] d2 = new String[d.length - 1];
                System.arraycopy(d, 0, d2, 0, d2.length);
                p.setCycleForMode(Cycle.join(d2));
                save();
                rebuild(true);
            }
        }), weightBtn());
        ops.addView(smallBtn("把今天设为第 1 天", new View.OnClickListener() {
            public void onClick(View v) {
                p.anchor = D.key(Calendar.getInstance());
                save();
                rebuild(true);
            }
        }), weightBtn());
        Ui.margins(ops, this, 0, 10, 0, 6);
        card.addView(ops);

        LinearLayout ops2 = Ui.row(this);
        ops2.addView(smallBtn(p.mode == Prefs.MODE_THREE ? "恢复 APN 模板" : "恢复 16 天模板", new View.OnClickListener() {
            public void onClick(View v) {
                confirm("覆盖当前排班？", new Runnable() {
                    public void run() {
                        p.setCycleForMode(p.mode == Prefs.MODE_THREE ? p.apnCycle() : Prefs.CYCLE16);
                        save();
                        rebuild(true);
                    }
                });
            }
        }), weightBtn());
        ops2.addView(smallBtn("选循环起始日", new View.OnClickListener() {
            public void onClick(View v) {
                pickDate(p.anchor);
            }
        }), weightBtn());
        ops2.addView(smallBtn("编辑说明", new View.OnClickListener() {
            public void onClick(View v) {
                showCycleHelp();
            }
        }), weightBtn());
        Ui.margins(ops2, this, 0, 0, 0, 4);
        card.addView(ops2);

        if (p.mode == Prefs.MODE_CYCLE)
            card.addView(Ui.tv(this, "默认模板：白、白、休、白、休、中夜(16h)、休、休，重复两轮 → 16 天。"
                    + "点格子可改成早/中/夜/休息或自定义时间。", 10, Ui.GRAY, false));
    }

    private LinearLayout dayGrid(String[] days) {
        LinearLayout grid = Ui.col(this);
        int cols = 4;
        for (int i = 0; i < days.length; i += cols) {
            LinearLayout row = Ui.row(this);
            for (int j = 0; j < cols; j++) {
                int idx = i + j;
                View cell;
                if (idx < days.length) cell = dayCell(idx, Plan.parse(days[idx]));
                else {
                    cell = new View(this);
                }
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f);
                lp.rightMargin = Ui.dp(this, 5);
                lp.bottomMargin = Ui.dp(this, 5);
                cell.setLayoutParams(lp);
                row.addView(cell);
            }
            grid.addView(row);
        }
        return grid;
    }

    /** 格子里那一行时间：24:00 不折成 00:00，跨零点用「明」省地方 */
    private static String cellTime(Plan plan) {
        if (plan.end <= 1440) return D.hm(plan.start) + "-" + (plan.end == 1440 ? "24:00" : D.hm(plan.end));
        return D.hm(plan.start) + "-明" + D.hm(plan.end);
    }

    private View dayCell(final int index, Plan plan) {
        LinearLayout l = Ui.col(this);
        l.setGravity(Gravity.CENTER);
        boolean work = plan.work;
        l.setBackground(Ui.round(work ? 0xFFEFF0DF : 0x0F000000, 10, this));
        TextView d = Ui.tv(this, "D" + (index + 1), 9, Ui.GRAY, false);
        d.setGravity(Gravity.CENTER);
        l.addView(d);
        TextView lab = Ui.tv(this, plan.work ? plan.shortLabel() + (plan.work2 ? "·值" : "") : "休",
                15, work ? Ui.INK : Ui.GRAY, true);
        lab.setGravity(Gravity.CENTER);
        l.addView(lab);
        TextView tm = Ui.tv(this, plan.work ? cellTime(plan) : "休息", 8.5f, Ui.GRAY, false);
        tm.setGravity(Gravity.CENTER);
        l.addView(tm);
        l.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editDay(index);
            }
        });
        return l;
    }

    // ------------------------------------------------------------ 薪资

    private LinearLayout buildSalary() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionLabel("薪资"));

        SegView seg = new SegView(this);
        seg.setOptions(new String[]{"日薪", "月薪", "时薪"}, p.salaryMode);
        seg.setOnSelect(new SegView.OnSelect() {
            public void onSelect(int index) {
                p.salaryMode = index;
                save();
                rebuild(true);
            }
        });
        Ui.margins(seg, this, 0, 8, 0, 12);
        card.addView(seg);

        String label = p.salaryMode == Prefs.PAY_HOURLY ? "时薪（元/小时）"
                : (p.salaryMode == Prefs.PAY_MONTHLY ? "月薪（元/月）" : "日薪（元/天）");
        card.addView(inputRow(label, num(p.amount), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL,
                new SetDouble() {
                    public void set(double v) {
                        p.amount = v;
                        save();
                        refreshTexts();
                    }
                }));

        if (p.salaryMode == Prefs.PAY_MONTHLY) {
            card.addView(inputRow("月计薪天数", num(p.monthDays),
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, new SetDouble() {
                        public void set(double v) {
                            p.monthDays = v;
                            save();
                            refreshTexts();
                        }
                    }));
        }

        card.addView(inputRow("发薪日（每月几号）", String.valueOf(p.payDay),
                InputType.TYPE_CLASS_NUMBER, new SetDouble() {
                    public void set(double v) {
                        p.payDay = (int) Math.max(1, Math.min(31, v));
                        save();
                        refreshTexts();
                    }
                }));
        card.addView(inputRow("小数位数（2–6）", String.valueOf(p.decimals),
                InputType.TYPE_CLASS_NUMBER, new SetDouble() {
                    public void set(double v) {
                        p.decimals = (int) Math.max(2, Math.min(6, v));
                        save();
                        refreshTexts();
                    }
                }));
        card.addView(inputRow("货币符号", p.currency, InputType.TYPE_CLASS_TEXT, null, new SetString() {
            public void set(String v) {
                p.currency = v.length() > 2 ? v.substring(0, 2) : v;
                save();
                rebuild(true);
            }
        }));

        LinearLayout lr = switchRow("下班后继续按倍率加班", p.overtime, new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean v) {
                p.overtime = v;
                save();
                refreshTexts();
            }
        });
        card.addView(lr);
        if (p.overtime) {
            card.addView(inputRow("加班倍率", num(p.overtimeRate),
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, new SetDouble() {
                        public void set(double v) {
                            p.overtimeRate = v <= 0 ? 1.5 : v;
                            save();
                            refreshTexts();
                        }
                    }));
        }
        card.addView(Ui.tv(this, p.salaryMode == Prefs.PAY_HOURLY
                ? "时薪口径：每干一小时就多一小时的钱，16 小时中夜班 = 8 小时白班的两倍。"
                : "日薪/月薪口径：每个班次算一天的钱，金额平摊在这个班的计薪时长里。",
                10, Ui.GRAY, false));
        return card;
    }

    // ------------------------------------------------------------ 小部件

    private LinearLayout buildWidgets() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionLabel("桌面小部件"));

        for (int i = 0; i < W_LABEL.length; i++) {
            card.addView(Ui.tv(this, W_LABEL[i], 11, Ui.GRAY, false));
            WidgetPreview pv = new WidgetPreview(this);
            pv.config(W_COLS[i], W_ROWS[i], p, snap);
            previews[i] = pv;
            card.addView(previewRow(pv, W_COLS[i]));
        }

        card.addView(smallBtn(W_BTN[0], new View.OnClickListener() {
            public void onClick(View v) {
                pinWidget(NiumaWidget1x2.class);
            }
        }));
        card.addView(smallBtn(W_BTN[1], new View.OnClickListener() {
            public void onClick(View v) {
                pinWidget(NiumaWidget1x4.class);
            }
        }));
        card.addView(smallBtn(W_BTN[2], new View.OnClickListener() {
            public void onClick(View v) {
                pinWidget(NiumaWidget2x2.class);
            }
        }));
        card.addView(smallBtn(W_BTN[3], new View.OnClickListener() {
            public void onClick(View v) {
                pinWidget(NiumaWidget2x4.class);
            }
        }));

        card.addView(smallBtn("怎么把卡片放到桌面？", new View.OnClickListener() {
            public void onClick(View v) {
                showAddHelp();
            }
        }));

        LinearLayout lr = switchRow("秒级刷新（常驻前台服务）", p.ticker, new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean v) {
                p.ticker = v;
                save();
                if (v) TickerService.ensure(MainActivity.this);
                else TickerService.stop(MainActivity.this);
            }
        });
        card.addView(lr);
        card.addView(Ui.tv(this, "已放置小部件：" + WidgetUpdater.countWidgets(this) + " 个。"
                + "关掉秒级刷新后，桌面只在打开应用 / 每半小时自动刷新一次。", 10, Ui.GRAY, false));
        return card;
    }

    /** 预览自己会按 2 列只画半幅，这里只负责上下间距 */
    private View previewRow(WidgetPreview pv, int cols) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 6);
        lp.bottomMargin = Ui.dp(this, 14);
        pv.setLayoutParams(lp);
        return pv;
    }

    // ------------------------------------------------------------ 显示

    private LinearLayout buildDisplay() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionLabel("显示与内容"));

        card.addView(switchRow("老板来了（隐藏金额）", p.masked, new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean v) {
                p.masked = v;
                save();
                refreshTexts();
                WidgetUpdater.updateAll(MainActivity.this);
            }
        }));
        card.addView(switchRow("滚轮翻牌数字", p.odometer, new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean v) {
                p.odometer = v;
                save();
            }
        }));

        card.addView(smallBtn("编辑毒鸡汤语录", new View.OnClickListener() {
            public void onClick(View v) {
                editText("毒鸡汤（每行一条）", p.quotes, new SetString() {
                    public void set(String s) {
                        p.quotes = s;
                        save();
                        refreshTexts();
                    }
                });
            }
        }));
        card.addView(smallBtn("编辑心愿单（名称=价格）", new View.OnClickListener() {
            public void onClick(View v) {
                editText("心愿单（每行一个，名称=价格）", p.wishlist, new SetString() {
                    public void set(String s) {
                        p.wishlist = s;
                        save();
                        refreshTexts();
                    }
                });
            }
        }));
        card.addView(smallBtn("恢复全部默认设置", new View.OnClickListener() {
            public void onClick(View v) {
                confirm("恢复默认？当前设置会被覆盖。", new Runnable() {
                    public void run() {
                        Prefs def = new Prefs();
                        def.anchor = D.key(Calendar.getInstance());
                        def.save(MainActivity.this);
                        p = def;
                        rebuild(true);
                        WidgetUpdater.updateAll(MainActivity.this);
                    }
                });
            }
        }));
        return card;
    }

    private LinearLayout buildAbout() {
        LinearLayout card = Ui.card(this);
        card.addView(sectionLabel("关于"));
        card.addView(Ui.tv(this, "从 Windows 悬浮窗项目 feiyuaq/niuma 移植而来："
                + "同样的计薪逻辑、状态机、滚轮数字与毒鸡汤，换成了安卓桌面小部件。"
                + "所有数据只存在本机，不联网、不上传。", 11, Ui.SUB, false));
        Ui.margins(card, this, 0, 10, 0, 0);
        card.addView(Ui.tv(this, "原项目 MIT License · 界面新设计", 10, Ui.GRAY, false));
        return card;
    }

    // =============================================================== 刷新

    private void recompute() {
        snapAt = System.currentTimeMillis();
        snap = Engine.compute(p, snapAt);
        refreshTexts();
        for (WidgetPreview pv : previews) if (pv != null) pv.update(p, snap);
    }

    private void refreshTexts() {
        if (snap == null) return;
        int col = WidgetRenderer.colorFor(snap.state);
        tvState.setText(snap.stateText);
        tvState.setTextColor(col);
        if (tvDot != null) tvDot.setBackground(Ui.round(col, 5, this));

        Calendar now = Calendar.getInstance();
        tvClock.setText(D.key(now) + "  " + D.WEEK[now.get(Calendar.DAY_OF_WEEK) - 1]
                + "  " + String.format("%02d:%02d:%02d", now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), now.get(Calendar.SECOND)));
        tvHint.setText(snap.hint);

        int dec = Math.max(2, Math.min(6, p.decimals));
        tvSpeed.setText(snap.speed > 0
                ? "当前赚钱速度  " + p.currency + fmt(snap.speed, dec) + " / 秒  ·  "
                + p.currency + fmt(snap.speed * 60, 2) + " / 分钟"
                : "当前赚钱速度  " + p.currency + "0 / 秒（暂停计薪）");

        tvProgress.setText("今日工时进度 " + fmt(snap.progress * 100, 1) + "%");
        tvHours.setText(fmt(snap.paidElapsed / 3600, 2) + " / " + fmt(snap.paidSeconds / 3600, 2) + " 小时");
        bar.setProgress((float) snap.progress);

        tvHourly.setText(p.masked ? p.currency + "****" : p.currency + fmt(snap.hourlyPay, 2));
        tvFull.setText(p.masked ? p.currency + "****" : p.currency + fmt(snap.fullDayPay, 2));
        double month = Engine.monthEarned(p, snap);
        tvMonth.setText(p.masked ? p.currency + "****" : p.currency + fmt(month, 2));

        int pd = Engine.daysToPayDay(p, snapAt);
        tvMonthSub.setText("本月满勤约 " + (p.masked ? "****" : p.currency + fmt(Engine.monthTotal(p, snapAt), 0))
                + "  ·  " + (pd == 0 ? "今天发工资！" : "距离发薪日还有 " + pd + " 天"));

        List<String> quotes = p.quoteList();
        if (!quotes.isEmpty()) {
            int idx = (int) ((snapAt / (1000 * 60 * 10)) % quotes.size());
            tvQuote.setText("“" + quotes.get(idx) + "”");
        }
        odo.setMasked(p.masked);
        if (p.masked) odo.setMoney(snap.earned, 0, p.decimals, p.currency, p.odometer);
    }

    // =============================================================== 交互

    interface SetInt {
        void set(int v);
    }

    interface SetDouble {
        void set(double v);
    }

    interface SetString {
        void set(String v);
    }

    private void save() {
        p.save(this);
    }

    private void toast(String s) {
        android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void confirm(String msg, final Runnable ok) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        ok.run();
                    }
                }).show();
    }

    private TextView sectionLabel(String s) {
        TextView t = Ui.tv(this, s, 12, Ui.GRAY, true);
        return t;
    }

    private View timeRow(LinearLayout parent, String label, int minutes, final SetInt cb) {
        LinearLayout r = Ui.row(this);
        r.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));
        r.addView(Ui.tv(this, label, 13, Ui.INK, false), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView val = Ui.num(this, minutes == 1440 ? "24:00" : D.hm(minutes), 14, Ui.BLUE, true);
        r.addView(val);
        r.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    public void onTimeSet(TimePicker view, int hour, int minute) {
                        cb.set(hour * 60 + minute);
                    }
                }, minutes / 60 % 24, minutes % 60, true).show();
            }
        });
        return r;
    }

    private View inputRow(String label, String value, int inputType, final SetDouble cb) {
        return inputRow(label, value, inputType, cb, null);
    }

    private View inputRow(final String label, String value, int inputType, final SetDouble numCb, final SetString strCb) {
        LinearLayout r = Ui.row(this);
        r.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        r.addView(Ui.tv(this, label, 13, Ui.INK, false), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final EditText e = new EditText(this);
        e.setText(value);
        e.setTextSize(14);
        e.setTypeface(numCb != null ? Ui.NUM : Ui.CN);
        e.setTextColor(Ui.BLUE);
        e.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        e.setInputType(inputType);
        e.setBackground(Ui.round(0x11000000, 9, this));
        int h = Ui.dp(this, 38), w = Ui.dp(this, 116);
        e.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        e.setLayoutParams(lp);
        final Runnable commit = new Runnable() {
            public void run() {
                String s = e.getText().toString().trim();
                if (numCb != null) {
                    try {
                        numCb.set(Double.parseDouble(s));
                    } catch (Exception ignored) {
                    }
                } else if (strCb != null) {
                    if (!s.isEmpty()) strCb.set(s);
                }
            }
        };
        e.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) commit.run();
            }
        });
        // 输入时实时生效，省得用户还要点一下别处
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                if (numCb != null) {
                    try {
                        numCb.set(Double.parseDouble(s.toString()));
                    } catch (Exception ignored) {
                    }
                }
            }
        });
        r.addView(e);
        return r;
    }

    private LinearLayout switchRow(String label, boolean value, CompoundButton.OnCheckedChangeListener cb) {
        LinearLayout r = Ui.row(this);
        r.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        r.addView(Ui.tv(this, label, 13, Ui.INK, false), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch s = new Switch(this);
        s.setChecked(value);
        s.setShowText(false);
        s.setOnCheckedChangeListener(cb);
        r.addView(s);
        return r;
    }

    private LinearLayout.LayoutParams weightBtn() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                Ui.dp(this, 36), 1f);
        lp.rightMargin = Ui.dp(this, 6);
        return lp;
    }

    private View smallBtn(String text, View.OnClickListener l) {
        LinearLayout b = new LinearLayout(this);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.round(0x11000000, 9, this));
        b.addView(Ui.tv(this, text, 11.5f, Ui.BLUE, true));
        b.setOnClickListener(l);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 36)));
        Ui.margins(b, this, 0, 4, 0, 4);
        return b;
    }

    private void editText(String title, final String value, final SetString cb) {
        final EditText e = new EditText(this);
        e.setText(value);
        e.setTextSize(13);
        e.setTextColor(Ui.INK);
        e.setMinLines(6);
        e.setGravity(Gravity.TOP | Gravity.LEFT);
        e.setBackground(Ui.round(0x11000000, 10, this));
        e.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
        FrameLayout wrap = new FrameLayout(this);
        int pad = Ui.dp(this, 16);
        wrap.setPadding(pad, Ui.dp(this, 6), pad, 0);
        wrap.addView(e, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 180)));
        new AlertDialog.Builder(this).setTitle(title).setView(wrap)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        cb.set(e.getText().toString());
                        rebuild(true);
                    }
                }).show();
    }

    private void pickDate(final String current) {
        Calendar c = D.parse(current);
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int y, int m, int d) {
                Calendar n = Calendar.getInstance();
                n.set(y, m, d);
                p.anchor = D.key(n);
                save();
                rebuild(true);
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void editDay(final int index) {
        String[] days = Cycle.days(p.cycleForMode());
        Plan cur = Plan.parse(days[index]);

        final String[] labels = {"休息", "白班 08:00–20:00", "早班 08:00–16:00",
                "中班 16:00–24:00", "夜班 00:00–08:00", "中夜班 16:00–次日08:00",
                "自定义时间…", (cur.work2 ? "改夜间值班…" : "加夜间值班（当天第二段）…"),
                "去掉夜间值班"};
        new AlertDialog.Builder(this)
                .setTitle("第 " + (index + 1) + " 天")
                .setItems(labels, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        String plan;
                        switch (which) {
                            case 0: plan = "R"; break;
                            case 1: plan = Plan.of(8 * 60, 20 * 60, -1, -1).encode(); break;
                            case 2: plan = Plan.of(8 * 60, 16 * 60, -1, -1).encode(); break;
                            case 3: plan = Plan.of(16 * 60, 24 * 60, -1, -1).encode(); break;
                            case 4: plan = Plan.of(0, 8 * 60, -1, -1).encode(); break;
                            case 5: plan = Plan.of(16 * 60, 32 * 60, -1, -1).encode(); break;
                            case 6:
                                customDay(index);
                                return;
                            case 7:
                                askNightDuty(index);
                                return;
                            case 8: {
                                String[] dd = Cycle.days(p.cycleForMode());
                                Plan c = Plan.parse(dd[index]);
                                c.work2 = false;
                                dd[index] = c.encode();
                                p.setCycleForMode(Cycle.join(dd));
                                save();
                                rebuild(true);
                                return;
                            }
                            default:
                                customDay(index);
                                return;
                        }
                        String[] days2 = Cycle.days(p.cycleForMode());
                        days2[index] = plan;
                        p.setCycleForMode(Cycle.join(days2));
                        save();
                        rebuild(true);
                }
}).show();
    }

    /**
     * 夜间值班：同一天在白班/中班结束之后再加一段。
     * 原来这天是休息的，就把这段值班本身当成这天的班；原来已经有班，就追加成第二段。
     */
    private void askNightDuty(final int index) {
        String[] days = Cycle.days(p.cycleForMode());
        final Plan cur = Plan.parse(days[index]);
        int def;
        if (cur.work2) def = cur.start2;
        else if (cur.work && cur.end <= 1440) def = cur.end;   // 默认紧接着白班下班
        else if (cur.work) def = 18 * 60;
        else def = 18 * 60;
        pickMinutes("夜间值班上班时间", def, new SetInt() {
            public void set(final int st) {
                pickMinutes("夜间值班下班时间（超过 24:00 算次日）", st + 4 * 60, new SetInt() {
                    public void set(int en) {
                        if (en <= st) en += 24 * 60;
                        String[] d = Cycle.days(p.cycleForMode());
                        Plan plan = cur.work ? cur.plusNight(st, en) : Plan.of(st, en, -1, -1);
                        d[index] = plan.encode();
                        p.setCycleForMode(Cycle.join(d));
                        save();
                        rebuild(true);
                    }
                });
            }
        });
    }

    private void customDay(final int index) {
        String[] days = Cycle.days(p.cycleForMode());
        Plan old = Plan.parse(days[index]);
        int st = old.work ? old.start : 8 * 60;
        int en = old.work ? old.end : 20 * 60;
        pickMinutes("上班时间", st, new SetInt() {
            public void set(final int start) {
                pickMinutes("下班时间（超过 24:00 算次日）", en, new SetInt() {
                    public void set(int end) {
                        if (end <= start) end += 24 * 60;
                        String[] d = Cycle.days(p.cycleForMode());
                        d[index] = Plan.of(start, end, -1, -1).encode();
                        p.setCycleForMode(Cycle.join(d));
                        save();
                        rebuild(true);
                    }
                });
            }
        });
    }

    private void pickMinutes(String title, int minutes, final SetInt cb) {
        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hour, int minute) {
                cb.set(hour * 60 + minute);
            }
        }, minutes / 60 % 24, minutes % 60, true).show();
    }

    private void pinWidget(Class<?> provider) {
        final ComponentName cn = new ComponentName(this, provider);
        final AppWidgetManager awm = (AppWidgetManager) getSystemService(Context.APPWIDGET_SERVICE);
        final int before = awm == null ? 0 : awm.getAppWidgetIds(cn).length;
        if (Build.VERSION.SDK_INT >= 26 && awm != null && awm.isRequestPinAppWidgetSupported()
                && awm.requestPinAppWidget(cn, null, null)) {
            // 桌面支持「一键放置」，但有的启动器会静默吞掉请求——过一秒看看有没有真的放上去
            ui.postDelayed(new Runnable() {
                public void run() {
                    if (awm != null && awm.getAppWidgetIds(cn).length > before) {
                        Toast.makeText(MainActivity.this, "已放到桌面", Toast.LENGTH_SHORT).show();
                        rebuild(false);
                        TickerService.ensure(MainActivity.this);
                    } else {
                        showAddHelp();
                    }
                }
            }, 1200);
            return;
        }
        showAddHelp();
    }

    private void showAddHelp() {
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle("把小部件放到桌面")
                .setMessage("1. 回到桌面，长按空白处 → 选择「小部件 / 添加小部件」；\n"
                        + "2. 在列表里找到「牛马脱敏器」，会看到四个条目：\n"
                        + "   · 牛马脱敏器 1×2（最小格）\n"
                        + "   · 牛马脱敏器 2×2（方形卡）\n"
                        + "   · 牛马脱敏器 1×4（细条）\n"
                        + "   · 牛马脱敏器 2×4（完整卡）\n"
                        + "3. 按住想用的那个，拖到桌面空位松手即可；\n"
                        + "4. 点小部件会直接打开本应用。\n\n"
                        + "四种都会显示今天的进账，另外常驻一行「明日」，告诉你明天上什么班、还是休息。\n"
                        + "不同启动器入口名字略有差别，找不到就长按桌面空白处试一下。")
                .setPositiveButton("知道了", null).create();
        d.show();
    }

    private void showCycleHelp() {
        new AlertDialog.Builder(this)
                .setTitle("循环排班怎么填")
                .setMessage("每一格是一天，点一下改成你真实要上的班：\n"
                        + "· 白班 08:00–20:00（12h）\n"
                        + "· 早班 08:00–16:00 / 中班 16:00–24:00 / 夜班 00:00–08:00（8h）\n"
                        + "· 中夜班 16:00–次日 08:00（16h）\n"
                        + "· 自定义时间：自己填上下班，超 24:00 自动算次日\n"
                        + "· 加夜间值班：白班下班后再上一段（08:00–16:00 再值 18:00–22:00），"
                        + "两段算同一天、同一个班次的钱，只是摊到更多计薪分钟上\n\n"
                        + "APN 模式下三个班的上下班时间都能改，改完按新时间重排 白×2、中×2、夜×2、休×2 的 8 天循环。\n"
                        + "「循环第 1 天」决定从哪天开始数，改完之后按你的第一轮第一天选一下即可。"
                        + "不上的班就点成休息，休息日不计薪。")
                .setPositiveButton("知道了", null).show();
    }

    // =============================================================== 数字格式

    private static String fmt(double v, int dec) {
        return String.format(java.util.Locale.US, "%." + dec + "f", v);
    }

    private static String num(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) return String.valueOf((long) Math.round(v));
        return String.format(java.util.Locale.US, "%s", v);
    }
}
