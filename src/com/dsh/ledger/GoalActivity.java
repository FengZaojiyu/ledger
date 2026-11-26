package com.dsh.ledger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

public class GoalActivity extends Activity {

    private LinearLayout box;
    private double avg = 0;

    @Override
    protected void onCreate(Bundle b) {
        Ui.applyTheme(this);
        super.onCreate(b);
        setContentView(build());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        Ui.polish(this, root);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 8));
        TextView back = Ui.text(this, "‹ 返回", 15, Ui.C_ACCENT);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        bar.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 80), Ui.dp(this, 32)));
        TextView title = Ui.text(this, "攒钱目标", 17, Ui.C_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1f));
        bar.addView(new LinearLayout(this), new LinearLayout.LayoutParams(Ui.dp(this, 80), Ui.dp(this, 32)));
        root.addView(bar);

        ScrollView sv = new ScrollView(this);
        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 12));
        sv.addView(box);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView add = Ui.button(this, "＋  新建目标", Ui.C_ACCENT, Color.WHITE, 16);
        add.setTypeface(Typeface.DEFAULT_BOLD);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new android.content.Intent(GoalActivity.this, GoalEditActivity.class));
            }
        });
        LinearLayout bottom = new LinearLayout(this);
        bottom.setPadding(Ui.dp(this, 16), Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 18));
        bottom.addView(add, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        root.addView(bottom);
        return root;
    }

    private void refresh() {
        box.removeAllViews();
        final Db db = Db.get(this);
        avg = Goal.avgMonthlySurplus(db);
        List<Goal> list = db.goals();

        LinearLayout tip = new LinearLayout(this);
        tip.setOrientation(LinearLayout.VERTICAL);
        tip.setBackground(Ui.glassSoft(this, 14));
        tip.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        tip.addView(Ui.text(this, "按近 3 个月平均结余估算：" + (avg > 0
                ? "¥" + Ui.money(avg) + " / 月"
                : "暂无结余（收入减支出 ≤ 0）"), 12, Ui.C_SUB));
        box.addView(tip);

        if (list.isEmpty()) {
            TextView empty = Ui.text(this, "还没有攒钱目标\n新建一个，比如「换手机 5000」「旅行基金 8000」", 14, Ui.C_SUB);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 50), 0, 0);
            box.addView(empty);
            return;
        }
        for (final Goal g : list) {
            box.addView(card(g));
        }
    }

    private View card(final Goal g) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.ripple(this, Ui.glass(this, 20), 0x1A5B7CFF));
        c.setElevation(Ui.dp(this, 7));
        c.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 12);
        c.setLayoutParams(lp);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = Ui.text(this, g.name, 16, Ui.C_TEXT);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView pct = Ui.text(this, String.format(java.util.Locale.CHINA, "%.0f%%", g.percent()),
                14, Ui.C_ACCENT);
        pct.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(pct);
        c.addView(head);

        c.addView(Ui.text(this, Goal.progressText(g), 12, Ui.C_SUB));

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(Ui.roundRect(Ui.C_TRACK, this, 4));
        int w1 = (int) Math.max(1, Math.round(Math.min(100, g.percent()) * 10));
        LinearLayout fill = new LinearLayout(this);
        fill.setBackground(Ui.roundRect(Ui.C_ACCENT, this, 4));
        track.addView(fill, new LinearLayout.LayoutParams(0, Ui.dp(this, 8), w1));
        track.addView(new LinearLayout(this), new LinearLayout.LayoutParams(0, Ui.dp(this, 8), Math.max(1, 1000 - w1)));
        LinearLayout tw = new LinearLayout(this);
        tw.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 4));
        tw.addView(track, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 8)));
        c.addView(tw);

        c.addView(Ui.text(this, Goal.etaText(g, avg), 12, Ui.C_ACCENT));
        if (g.deadlineTs > 0) {
            TextView dl = Ui.text(this, Goal.deadlineText(g), 11, Ui.C_SUB);
            dl.setPadding(0, Ui.dp(this, 2), 0, 0);
            c.addView(dl);
        }

        LinearLayout ops = new LinearLayout(this);
        ops.setPadding(0, Ui.dp(this, 10), 0, 0);
        TextView put = Ui.button(this, "存入", Ui.C_SOFT_BTN, Ui.C_ACCENT, 13);
        put.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                android.content.Intent i = new android.content.Intent(GoalActivity.this, GoalEditActivity.class);
                i.putExtra("mode", GoalEditActivity.MODE_DEPOSIT);
                i.putExtra("goal", g.id);
                startActivity(i);
            }
        });
        ops.addView(put, new LinearLayout.LayoutParams(0, Ui.dp(this, 36), 1f));
        TextView del = Ui.button(this, "删除", Ui.C_SOFT_BTN, Ui.C_RED, 13);
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { confirmDelete(g); }
        });
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0, Ui.dp(this, 36), 1f);
        dlp.leftMargin = Ui.dp(this, 8);
        ops.addView(del, dlp);
        c.addView(ops);

        return c;
    }

    private void newGoal() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);

        final EditText name = new EditText(this);
        name.setHint("目标名称，比如「换手机」");
        name.setSingleLine(true);
        final EditText target = new EditText(this);
        target.setHint("目标金额（元）");
        target.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        target.setSingleLine(true);
        final EditText saved0 = new EditText(this);
        saved0.setHint("已经攒了多少（可不填）");
        saved0.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        saved0.setSingleLine(true);

        wrap.addView(name);
        wrap.addView(target);
        wrap.addView(saved0);

        new AlertDialog.Builder(this)
                .setTitle("新建攒钱目标")
                .setView(wrap)
                .setNegativeButton("取消", null)
                .setPositiveButton("创建", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        String n = name.getText().toString().trim();
                        if (n.length() == 0) { toast("起个名字"); return; }
                        double t;
                        try { t = Double.parseDouble(target.getText().toString().trim()); }
                        catch (Exception e) { toast("目标金额不对"); return; }
                        if (t <= 0) { toast("目标金额要大于 0"); return; }
                        double s = 0;
                        try { s = Double.parseDouble(saved0.getText().toString().trim()); } catch (Exception ignored) { }
                        Goal g = new Goal();
                        g.name = n;
                        g.target = t;
                        g.saved = Math.max(0, s);
                        g.createdTs = System.currentTimeMillis();
                        Db.get(GoalActivity.this).addGoal(g);
                        refresh();
                    }
                }).show();
    }

    private void deposit(final Goal g) {
        final EditText input = new EditText(this);
        input.setHint("存入金额（元），负数表示取出");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setSingleLine(true);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        wrap.addView(input);
        new AlertDialog.Builder(this)
                .setTitle("「" + g.name + "」存入")
                .setView(wrap)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        String s = input.getText().toString().trim();
                        if (s.length() == 0) return;
                        double v;
                        try { v = Double.parseDouble(s); } catch (Exception e) { toast("金额不对"); return; }
                        Db.get(GoalActivity.this).addSaved(g.id, v);
                        refresh();
                    }
                }).show();
    }

    private void confirmDelete(final Goal g) {
        Dialogs.confirm(this, "删除目标", "「" + g.name + "」已攒的 "
                + Ui.money(g.saved) + " 元记录也会一起删掉。", Ui.C_RED, new Runnable() {
            public void run() {
                Db.get(GoalActivity.this).deleteGoal(g.id);
                refresh();
            }
        });
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
