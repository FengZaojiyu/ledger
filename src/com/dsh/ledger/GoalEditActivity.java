package com.dsh.ledger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/** 新建目标 / 存入，都用页面内表单，和「记一笔」同一套视觉 */
public class GoalEditActivity extends Activity {

    public static final int MODE_NEW = 0;
    public static final int MODE_DEPOSIT = 1;

    private int mode = MODE_NEW;
    private Goal goal;

    private EditText nameIn, targetIn, savedIn, amountIn;
    private TextView preview;

    @Override
    protected void onCreate(Bundle b) {
        Ui.applyTheme(this);
        super.onCreate(b);
        mode = getIntent().getIntExtra("mode", MODE_NEW);
        long id = getIntent().getLongExtra("goal", 0);
        if (id > 0) {
            for (Goal g : Db.get(this).goals()) if (g.id == id) goal = g;
        }
        if (mode == MODE_DEPOSIT && goal == null) { finish(); return; }
        setContentView(build());
        if (mode == MODE_NEW) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            nameIn.requestFocus();
        }
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        Ui.polish(this, root);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 8));
        TextView cancel = Ui.text(this, "取消", 15, Ui.C_SUB);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        bar.addView(cancel, new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1f));
        TextView title = Ui.text(this,
                mode == MODE_NEW ? "新建目标" : "存入 · " + goal.name, 17, Ui.C_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 2f));
        bar.addView(new LinearLayout(this), new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1f));
        root.addView(bar);

        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 16));

        if (mode == MODE_NEW) {
            // 名称
            LinearLayout nameCard = card();
            nameCard.addView(label("目标名称"));
            nameIn = bigInput("换手机 / 旅行基金", false);
            nameCard.addView(nameIn);
            box.addView(nameCard);

            LinearLayout tCard = card();
            tCard.addView(label("目标金额"));
            targetIn = moneyRow(tCard);
            box.addView(tCard);

            LinearLayout sCard = card();
            sCard.addView(label("已经攒了多少（可不填）"));
            savedIn = moneyRow(sCard);
            box.addView(sCard);

            LinearLayout pCard = card();
            preview = Ui.text(this, "", 13, Ui.C_ACCENT);
            preview.setLineSpacing(Ui.dp(this, 3), 1f);
            pCard.addView(preview);
            box.addView(pCard);

            TextWatcher w = new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b2, int c) { }
                public void onTextChanged(CharSequence s, int a, int b2, int c) { }
                public void afterTextChanged(Editable s) { updatePreview(); }
            };
            targetIn.addTextChangedListener(w);
            savedIn.addTextChangedListener(w);
        } else {
            LinearLayout info = card();
            TextView n = Ui.text(this, "🎯 " + goal.name, 16, Ui.C_TEXT);
            n.setTypeface(Typeface.DEFAULT_BOLD);
            info.addView(n);
            info.addView(Ui.text(this, "已攒 " + Goal.progressText(goal), 12, Ui.C_SUB));
            info.addView(Ui.text(this, Goal.etaText(goal, Goal.avgMonthlySurplus(Db.get(this))),
                    12, Ui.C_ACCENT));
            box.addView(info);

            LinearLayout dCard = card();
            dCard.addView(label("存入金额（负数表示取出）"));
            amountIn = moneyRow(dCard);
            box.addView(dCard);

            LinearLayout chips = new LinearLayout(this);
            chips.setPadding(0, Ui.dp(this, 2), 0, 0);
            String[] quick = {"+100", "+500", "+1000", "-100"};
            for (final String q : quick) {
                TextView chip = Ui.button(this, q, Ui.C_SOFT_BTN, Ui.C_ACCENT, 13);
                chip.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        amountIn.setText(q.replace("+", ""));
                        amountIn.setSelection(amountIn.getText().length());
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 36), 1f);
                lp.rightMargin = Ui.dp(this, 6);
                chips.addView(chip, lp);
            }
            box.addView(chips);
        }

        TextView save = Ui.button(this, "保存", Ui.C_ACCENT, Color.WHITE, 16);
        save.setTypeface(Typeface.DEFAULT_BOLD);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { save(); }
        });
        LinearLayout bottom = new LinearLayout(this);
        bottom.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 8));
        bottom.addView(save, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        box.addView(bottom);

        sv.addView(box);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        if (mode == MODE_NEW) updatePreview();
        Ui.imeAware(root);
        return root;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.glass(this, 20));
        c.setElevation(Ui.dp(this, 7));
        c.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 10);
        c.setLayoutParams(lp);
        return c;
    }

    private TextView label(String s) {
        TextView t = Ui.text(this, s, 12, Ui.C_SUB);
        t.setPadding(0, 0, 0, Ui.dp(this, 6));
        return t;
    }

    private EditText bigInput(String hint, boolean numeric) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(18);
        e.setBackground(null);
        e.setPadding(0, 0, 0, 0);
        e.setSingleLine(true);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    private EditText moneyRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(Ui.text(this, "¥", 20, Ui.C_TEXT));
        EditText e = bigInput("0", true);
        e.setTextSize(26);
        e.setPadding(Ui.dp(this, 8), 0, 0, 0);
        row.addView(e, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        parent.addView(row);
        return e;
    }

    private void updatePreview() {
        if (preview == null) return;
        double t = parse(targetIn.getText().toString());
        double s = parse(savedIn.getText().toString());
        if (t <= 0) {
            preview.setText("填上目标金额，这里会实时算出还要攒多久。");
            return;
        }
        double avg = Goal.avgMonthlySurplus(Db.get(this));
        double remain = Math.max(0, t - Math.max(0, s));
        int m = Goal.monthsNeeded(remain, avg);
        String eta;
        if (m == 0) eta = "已经达成 🎉";
        else if (m < 0) eta = "按当前结余（" + Ui.money(avg) + " 元/月）攒不下来，需要提高月结余";
        else eta = "还要约 " + m + " 个月，也就是 " + etaDate(m) + " 前后";
        preview.setText("还差 " + Ui.money(remain) + " 元\n" + eta);
    }

    private String etaDate(int months) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.MONTH, months);
        return new java.text.SimpleDateFormat("yyyy年M月", Locale.CHINA).format(c.getTime());
    }

    private double parse(String s) {
        if (s == null || s.trim().length() == 0) return 0;
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
    }

    private void save() {
        if (mode == MODE_NEW) {
            String n = nameIn.getText().toString().trim();
            if (n.length() == 0) { toast("起个名字"); return; }
            double t = parse(targetIn.getText().toString());
            if (t <= 0) { toast("目标金额要大于 0"); return; }
            Goal g = new Goal();
            g.name = n;
            g.target = t;
            g.saved = Math.max(0, parse(savedIn.getText().toString()));
            g.createdTs = System.currentTimeMillis();
            Db.get(this).addGoal(g);
            toast("已创建");
        } else {
            double v = parse(amountIn.getText().toString());
            if (v == 0) { toast("输入金额"); return; }
            Db.get(this).addSaved(goal.id, v);
            toast(v > 0 ? "已存入 ¥" + Ui.money(v) : "已取出 ¥" + Ui.money(-v));
        }
        finish();
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
