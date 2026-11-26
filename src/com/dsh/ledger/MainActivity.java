package com.dsh.ledger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;

public class MainActivity extends Activity {

    private final Calendar month = Calendar.getInstance();
    private LinearLayout listBox;
    private TextView monthLabel;
    private TextView expVal, incVal, balVal;
    private EditText search;
    private LinearLayout budgetBox;
    private LinearLayout goalBox;
    private java.util.HashMap<String, String> emojiMap;
    private String appliedThemeKey;

    /** 主题 + 实际明暗的组合，用来判断是否需要重建 */
    private String themeKey() {
        String m = Ui.themeMode(this);
        boolean dark = "dark".equals(m) || ("system".equals(m) && Ui.isSystemDark(this));
        return m + "|" + dark;
    }

    @Override
    protected void onCreate(Bundle b) {
        Ui.applyTheme(this);
        appliedThemeKey = themeKey();
        super.onCreate(b);
        setContentView(build());
        if (android.os.Build.VERSION.SDK_INT >= 33 && !FocusNotify.canPost(this)) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 11);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == 11) FocusNotify.update(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 在设置里切了主题，回到主页时要整页重建，否则只有文字颜色变、卡片还是旧的
        if (appliedThemeKey != null && !themeKey().equals(appliedThemeKey)) {
            recreate();
            return;
        }
        refresh();
        FocusNotify.update(this);
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        Ui.polish(this, root);

        // 顶栏
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 6));
        TextView title = Ui.text(this, "小账本", 20, Ui.C_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1f));
        TextView stats = Ui.pill(this, "统计");
        stats.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, StatsActivity.class);
                i.putExtra("month", month.getTimeInMillis());
                startActivity(i);
            }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.rightMargin = Ui.dp(this, 6);
        bar.addView(stats, slp);
        TextView setting = Ui.pill(this, "设置");
        setting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        bar.addView(setting);
        root.addView(bar);

        // 汇总卡
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.glass(this, 22));
        card.setElevation(Ui.dp(this, 7));
        card.setElevation(Ui.dp(this, 10));
        card.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout c1 = column("本月支出", Ui.C_RED);
        LinearLayout c2 = column("本月收入", Ui.C_GREEN);
        LinearLayout c3 = column("本月结余", Ui.C_TEXT);
        row.addView(c1, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(c2, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(c3, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        expVal = (TextView) c1.getChildAt(1);
        incVal = (TextView) c2.getChildAt(1);
        balVal = (TextView) c3.getChildAt(1);

        budgetBox = new LinearLayout(this);
        budgetBox.setOrientation(LinearLayout.VERTICAL);
        budgetBox.setPadding(0, Ui.dp(this, 12), 0, 0);
        budgetBox.setVisibility(View.GONE);
        card.addView(budgetBox);

        goalBox = new LinearLayout(this);
        goalBox.setOrientation(LinearLayout.VERTICAL);
        goalBox.setPadding(0, Ui.dp(this, 10), 0, 0);
        card.addView(goalBox);

        LinearLayout cardWrap = new LinearLayout(this);
        cardWrap.setPadding(Ui.dp(this, 16), Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 8));
        cardWrap.addView(card, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(cardWrap);

        // 搜索
        LinearLayout searchWrap = new LinearLayout(this);
        searchWrap.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 6));
        search = new EditText(this);
        search.setHint("搜索备注或分类");
        search.setTextSize(13);
        search.setSingleLine(true);
        search.setBackground(Ui.glassSoft(this, 16));
        search.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 8));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable s) { refresh(); }
        });
        searchWrap.addView(search, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(searchWrap);

        // 月份切换
        LinearLayout monthRow = new LinearLayout(this);
        monthRow.setGravity(Gravity.CENTER);
        monthRow.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        TextView prev = Ui.text(this, "‹", 24, Ui.C_SUB);
        prev.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        prev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { month.add(Calendar.MONTH, -1); refresh(); }
        });
        monthLabel = Ui.text(this, "", 14, Ui.C_TEXT);
        monthLabel.setTypeface(Typeface.DEFAULT_BOLD);
        TextView next = Ui.text(this, "›", 24, Ui.C_SUB);
        next.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { month.add(Calendar.MONTH, 1); refresh(); }
        });
        monthRow.addView(prev);
        monthRow.addView(monthLabel);
        monthRow.addView(next);
        root.addView(monthRow);

        // 列表
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        listBox.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 12));
        sv.addView(listBox, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // 底部按钮
        TextView add = Ui.button(this, "＋  记一笔", Ui.C_ACCENT, Color.WHITE, 16);
        add.setTypeface(Typeface.DEFAULT_BOLD);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddActivity.class));
            }
        });
        LinearLayout bottom = new LinearLayout(this);
        bottom.setPadding(Ui.dp(this, 16), Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 18));
        bottom.addView(add, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        root.addView(bottom);

        // 内容 + 可拖动 AI 悬浮球
        FrameLayout frame = new FrameLayout(this);
        frame.addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        frame.addView(aiFab());
        return frame;
    }

    /** 可拖动、松手吸附边缘、位置记忆的 AI 悬浮球 */
    private View aiFab() {
        final int size = Ui.dp(this, 56);
        final ImageView v = new ImageView(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Ui.C_BTN, Ui.C_BTN2});
        bg.setShape(GradientDrawable.OVAL);
        bg.setStroke(Math.max(1, Ui.dp(this, 1)), 0x70FFFFFF);
        v.setBackground(bg);
        v.setElevation(Ui.dp(this, 10));
        v.setPadding(Ui.dp(this, 15), Ui.dp(this, 15), Ui.dp(this, 15), Ui.dp(this, 15));
        v.setContentDescription("AI 助手");
        int id = getResources().getIdentifier("ic_ai", "drawable", getPackageName());
        if (id != 0) v.setImageResource(id);

        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
        v.setLayoutParams(lp);

        final SharedPreferences p = getSharedPreferences("ledger", MODE_PRIVATE);
        v.post(new Runnable() {
            public void run() {
                View parent = (View) v.getParent();
                if (parent == null) return;
                int W = parent.getWidth(), H = parent.getHeight();
                int x = p.getInt("fab_x", -1), y = p.getInt("fab_y", -1);
                if (x < 0 || x > W - size) x = W - size - Ui.dp(MainActivity.this, 18);
                if (y < 0 || y > H - size) y = (int) (H * 0.62f);
                lp.leftMargin = x;
                lp.topMargin = y;
                v.setLayoutParams(lp);
            }
        });

        v.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int startLeft, startTop;
            boolean moved;

            public boolean onTouch(View view, MotionEvent e) {
                View parent = (View) view.getParent();
                if (parent == null) return false;
                int W = parent.getWidth();
                int H = parent.getHeight();
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX();
                        downY = e.getRawY();
                        startLeft = lp.leftMargin;
                        startTop = lp.topMargin;
                        moved = false;
                        view.animate().scaleX(1.07f).scaleY(1.07f).setDuration(80).start();
                        return true;
                    case MotionEvent.ACTION_MOVE: {
                        int nx = (int) (startLeft + (e.getRawX() - downX));
                        int ny = (int) (startTop + (e.getRawY() - downY));
                        int tol = Ui.dp(MainActivity.this, 6);
                        if (Math.abs(nx - startLeft) > tol || Math.abs(ny - startTop) > tol) moved = true;
                        nx = Math.max(0, Math.min(W - size, nx));
                        ny = Math.max(0, Math.min(H - size, ny));
                        lp.leftMargin = nx;
                        lp.topMargin = ny;
                        view.setLayoutParams(lp);
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                        if (!moved) {
                            startActivity(new Intent(MainActivity.this, ChatActivity.class));
                        } else {
                            int edge = lp.leftMargin + size / 2 < W / 2 ? 0 : W - size;
                            lp.leftMargin = edge;
                            view.setLayoutParams(lp);
                            p.edit().putInt("fab_x", lp.leftMargin)
                                    .putInt("fab_y", lp.topMargin).apply();
                        }
                        return true;
                }
                return false;
            }
        });
        return v;
    }

    private LinearLayout column(String label, int color) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView l = Ui.text(this, label, 11, Ui.C_SUB);
        TextView v = Ui.text(this, "¥0.00", 20, color);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(l);
        col.addView(v);
        return col;
    }

    private void refresh() {
        monthLabel.setText(Ui.monthKey(month));
        long[] r = Ui.monthRange(month);
        Db db = Db.get(this);
        emojiMap = db.emojiMap();
        String kw = search.getText().toString().trim();
        Cursor c = db.between(r[0], r[1], kw.length() == 0 ? null : kw);

        double exp = db.sum(r[0], r[1], Db.EXPENSE);
        double inc = db.sum(r[0], r[1], Db.INCOME);

        listBox.removeAllViews();
        String lastDay = null;
        int count = 0;

        while (c.moveToNext()) {
            Rec rec = Rec.from(c);
            count++;
            String day = Ui.dayKey(rec.ts);
            if (!day.equals(lastDay)) {
                TextView h = Ui.text(this, day, 12, Ui.C_SUB2);
                h.setPadding(Ui.dp(this, 4), Ui.dp(this, 14), 0, Ui.dp(this, 6));
                listBox.addView(h);
                lastDay = day;
            }
            listBox.addView(row(rec));
        }
        c.close();

        expVal.setText("¥" + Ui.money(exp));
        incVal.setText("¥" + Ui.money(inc));
        double bal = inc - exp;
        balVal.setText((bal < 0 ? "-¥" : "¥") + Ui.money(Math.abs(bal)));
        balVal.setTextColor(bal < 0 ? Ui.C_RED : Ui.C_TEXT);

        renderBudget(exp);
        renderGoal();

        if (count == 0) {
            TextView emptyIcon = Ui.text(this, kw.length() > 0 ? "🔍" : "🪙", 34, Ui.C_SUB);
            emptyIcon.setGravity(Gravity.CENTER);
            emptyIcon.setPadding(0, Ui.dp(this, 56), 0, Ui.dp(this, 10));
            listBox.addView(emptyIcon);

            TextView empty = Ui.text(this,
                    kw.length() > 0 ? "没有匹配的记录\n换个关键词试试" : "这个月还没有记录\n点下面的「＋ 记一笔」开始",
                    13, Ui.C_SUB);
            empty.setGravity(Gravity.CENTER);
            empty.setLineSpacing(Ui.dp(this, 4), 1f);
            listBox.addView(empty);
        }
    }

    private void renderBudget(double exp) {
        budgetBox.removeAllViews();
        double budget = Db.get(this).budget();
        if (budget <= 0) {
            budgetBox.setVisibility(View.GONE);
            return;
        }
        budgetBox.setVisibility(View.VISIBLE);
        double pct = exp / budget * 100;
        boolean over = exp > budget;

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(Ui.text(this, "本月预算 ¥" + Ui.money(budget), 12, Ui.C_SUB),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView p = Ui.text(this, String.format(java.util.Locale.CHINA, "%.0f%%", pct),
                12, over ? Ui.C_RED : Ui.C_ACCENT);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(p);
        budgetBox.addView(head);

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(Ui.roundRect(Ui.C_TRACK, this, 4));
        int w1 = (int) Math.max(1, Math.round(Math.min(100, pct) * 10));
        LinearLayout fill = new LinearLayout(this);
        fill.setBackground(Ui.roundRect(over ? Ui.C_RED : Ui.C_ACCENT, this, 4));
        track.addView(fill, new LinearLayout.LayoutParams(0, Ui.dp(this, 7), w1));
        LinearLayout rest = new LinearLayout(this);
        track.addView(rest, new LinearLayout.LayoutParams(0, Ui.dp(this, 7), Math.max(1, 1000 - w1)));
        LinearLayout tw = new LinearLayout(this);
        tw.setPadding(0, Ui.dp(this, 6), 0, 0);
        tw.addView(track, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 7)));
        budgetBox.addView(tw);

        if (over) {
            TextView warn = Ui.text(this,
                    "已超支 ¥" + Ui.money(exp - budget), 12, Ui.C_RED);
            warn.setPadding(0, Ui.dp(this, 6), 0, 0);
            budgetBox.addView(warn);
        }
    }

    private void renderGoal() {
        goalBox.removeAllViews();
        java.util.List<Goal> goals = Db.get(this).goals();
        if (goals.isEmpty()) {
            TextView t = Ui.text(this, "🎯  设定攒钱目标", 13, Ui.C_ACCENT);
            t.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 2));
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, GoalActivity.class));
                }
            });
            goalBox.addView(t);
            return;
        }
        final Goal g = goals.get(0);
        double avg = Goal.avgMonthlySurplus(Db.get(this));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = Ui.text(this, "🎯 " + g.name, 13, Ui.C_TEXT);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(Ui.text(this, String.format(java.util.Locale.CHINA, "%.0f%%", g.percent()),
                12, Ui.C_ACCENT));
        goalBox.addView(head);

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(Ui.roundRect(Ui.C_TRACK, this, 4));
        int w1 = (int) Math.max(1, Math.round(Math.min(100, g.percent()) * 10));
        LinearLayout fill = new LinearLayout(this);
        fill.setBackground(Ui.roundRect(Ui.C_ACCENT, this, 4));
        track.addView(fill, new LinearLayout.LayoutParams(0, Ui.dp(this, 6), w1));
        track.addView(new LinearLayout(this), new LinearLayout.LayoutParams(0, Ui.dp(this, 6), Math.max(1, 1000 - w1)));
        LinearLayout tw = new LinearLayout(this);
        tw.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 4));
        tw.addView(track, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 6)));
        goalBox.addView(tw);

        int m = Goal.monthsNeeded(g.remain(), avg);
        String shortEta = g.remain() <= 0 ? "已达成 🎉"
                : (m < 0 ? "按当前结余攒不下来" : "还要 " + m + " 个月");
        TextView eta = Ui.text(this, Goal.progressText(g) + " · " + shortEta, 11, Ui.C_SUB);
        goalBox.addView(eta);

        goalBox.setBackground(Ui.ripple(this, Ui.glassSoft(this, 14), 0x1A5B7CFF));
        goalBox.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        goalBox.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GoalActivity.class));
            }
        });
    }

    private View row(final Rec rec) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackground(Ui.ripple(this, Ui.glassSoft(this, 18), 0x225B7CFF));
        r.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rl.bottomMargin = Ui.dp(this, 8);
        r.setLayoutParams(rl);

        TextView icon = Ui.text(this, Ui.emoji(rec.cat, emojiMap), 19, Ui.C_TEXT);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.roundRect(Ui.C_ICON_BG, this, 19));
        r.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38)));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        TextView name = Ui.text(this, rec.cat, 15, Ui.C_TEXT);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        mid.addView(name);
        String sub = (rec.note == null || rec.note.length() == 0) ? Ui.timeKey(rec.ts) : rec.note;
        mid.addView(Ui.text(this, sub, 12, Ui.C_SUB));
        r.addView(mid, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        String sign = rec.type == Db.EXPENSE ? "-" : "+";
        TextView amt = Ui.text(this, sign + "¥" + Ui.money(rec.amount), 16,
                rec.type == Db.EXPENSE ? Ui.C_TEXT : Ui.C_GREEN);
        amt.setTypeface(Typeface.DEFAULT_BOLD);
        r.addView(amt);

        r.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, AddActivity.class);
                i.putExtra("id", rec.id);
                startActivity(i);
            }
        });
        r.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                Dialogs.confirm(MainActivity.this, "删除记录",
                        rec.cat + "  " + (rec.type == Db.EXPENSE ? "−" : "+")
                                + Ui.money(rec.amount) + " 元，删掉就找不回来了。",
                        Ui.C_RED, new Runnable() {
                            public void run() {
                                Db.get(MainActivity.this).delete(rec.id);
                                refresh();
                            }
                        });
                return true;
            }
        });
        return r;
    }
}
