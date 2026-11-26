package com.dsh.ledger;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

public class StatsActivity extends Activity {

    private final Calendar month = Calendar.getInstance();
    private LinearLayout box;
    private TextView monthLabel;
    private java.util.HashMap<String, String> emojiMap;

    @Override
    protected void onCreate(Bundle b) {
        Ui.applyTheme(this);
        super.onCreate(b);
        long m = getIntent().getLongExtra("month", 0);
        if (m > 0) month.setTimeInMillis(m);
        setContentView(build());
        render();
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
        TextView title = Ui.text(this, "统计", 17, Ui.C_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1f));
        monthLabel = Ui.text(this, "", 13, Ui.C_SUB);
        bar.addView(monthLabel);
        root.addView(bar);

        ScrollView sv = new ScrollView(this);
        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 16));
        sv.addView(box);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private void render() {
        monthLabel.setText(Ui.monthKey(month));
        box.removeAllViews();
        long[] r = Ui.monthRange(month);
        Db db = Db.get(this);
        emojiMap = db.emojiMap();

        double expTotal = db.sum(r[0], r[1], Db.EXPENSE);
        double incTotal = db.sum(r[0], r[1], Db.INCOME);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setBackground(Ui.glass(this, 22));
        head.setElevation(Ui.dp(this, 7));
        head.setPadding(Ui.dp(this, 20), Ui.dp(this, 18), Ui.dp(this, 20), Ui.dp(this, 18));
        head.addView(Ui.text(this, "本月支出", 12, Ui.C_SUB));
        TextView big = Ui.text(this, "¥" + Ui.money(expTotal), 30, Ui.C_TEXT);
        big.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(big);
        head.addView(Ui.text(this, "收入 ¥" + Ui.money(incTotal)
                + "　结余 ¥" + Ui.money(incTotal - expTotal), 13, Ui.C_SUB));
        LinearLayout hw = new LinearLayout(this);
        hw.setPadding(0, 0, 0, Ui.dp(this, 12));
        hw.addView(head, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(hw);

        // 近 6 个月趋势
        sectionTitle("近 6 个月趋势");
        LinearLayout chartCard = new LinearLayout(this);
        chartCard.setOrientation(LinearLayout.VERTICAL);
        chartCard.setBackground(Ui.glass(this, 22));
        chartCard.setElevation(Ui.dp(this, 7));
        chartCard.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 10));

        LinearLayout legend = new LinearLayout(this);
        legend.setPadding(Ui.dp(this, 8), 0, 0, Ui.dp(this, 6));
        legend.addView(dot(Ui.C_RED, "支出"));
        legend.addView(dot(Ui.C_GREEN, "收入"));
        chartCard.addView(legend);

        List<double[]> data = db.monthly(6);
        ChartView cv = new ChartView(this, data);
        chartCard.addView(cv, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 170)));
        LinearLayout cw = new LinearLayout(this);
        cw.setPadding(0, 0, 0, Ui.dp(this, 10));
        cw.addView(chartCard, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(cw);

        sectionTitle("支出构成");
        section(db, r, Db.EXPENSE, expTotal, "暂无支出记录");
        sectionTitle("收入构成");
        section(db, r, Db.INCOME, incTotal, "暂无收入记录");
    }

    private void sectionTitle(String s) {
        TextView h = Ui.text(this, s, 13, Ui.C_SUB);
        h.setPadding(Ui.dp(this, 4), Ui.dp(this, 6), 0, Ui.dp(this, 8));
        box.addView(h);
    }

    private View dot(int color, String label) {
        LinearLayout l = new LinearLayout(this);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(0, 0, Ui.dp(this, 14), 0);
        View v = new View(this);
        v.setBackground(Ui.roundRect(color, this, 4));
        l.addView(v, new LinearLayout.LayoutParams(Ui.dp(this, 8), Ui.dp(this, 8)));
        TextView t = Ui.text(this, " " + label, 11, Ui.C_SUB);
        l.addView(t);
        return l;
    }

    private void section(Db db, long[] r, int type, double total, String emptyText) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.glass(this, 22));
        card.setElevation(Ui.dp(this, 7));
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));

        Cursor c = db.sumsByCat(r[0], r[1], type);
        int n = 0;
        while (c.moveToNext()) {
            String cat = c.getString(0);
            double sum = c.getDouble(1);
            n++;
            card.addView(barRow(cat, sum, total));
        }
        c.close();
        if (n == 0) {
            TextView empty = Ui.text(this, emptyText, 13, Ui.C_SUB);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 24), 0, Ui.dp(this, 24));
            card.addView(empty);
        }
        LinearLayout w = new LinearLayout(this);
        w.setPadding(0, 0, 0, Ui.dp(this, 6));
        w.addView(card, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(w);
    }

    private View barRow(String cat, double sum, double total) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));

        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView ic = Ui.text(this, Ui.emoji(cat, emojiMap), 15, Ui.C_TEXT);
        ic.setGravity(Gravity.CENTER);
        ic.setBackground(Ui.roundRect(Ui.C_ICON_BG, this, 16));
        line.addView(ic, new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32)));
        TextView nm = Ui.text(this, cat, 14, Ui.C_TEXT);
        nm.setPadding(Ui.dp(this, 10), 0, 0, 0);
        line.addView(nm, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        double pct = total <= 0 ? 0 : sum / total * 100;
        line.addView(Ui.text(this, "¥" + Ui.money(sum) + "  "
                + String.format(java.util.Locale.CHINA, "%.0f%%", pct), 13, Ui.C_SUB));
        col.addView(line);

        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(Ui.roundRect(Ui.C_TRACK, this, 4));
        int w1 = (int) Math.max(1, Math.round(pct * 10));
        LinearLayout fill = new LinearLayout(this);
        fill.setBackground(Ui.roundRect(Ui.C_ACCENT, this, 4));
        track.addView(fill, new LinearLayout.LayoutParams(0, Ui.dp(this, 7), w1));
        track.addView(new LinearLayout(this), new LinearLayout.LayoutParams(0, Ui.dp(this, 7), Math.max(1, 1000 - w1)));
        LinearLayout tw = new LinearLayout(this);
        tw.setPadding(0, Ui.dp(this, 6), 0, 0);
        tw.addView(track, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 7)));
        col.addView(tw);
        return col;
    }

    /** 6 个月柱状图：每组两根柱（支出 / 收入） */
    static class ChartView extends View {
        private final List<double[]> data;
        private final Paint p = new Paint();
        private final float dens;

        ChartView(Context c, List<double[]> data) {
            super(c);
            this.data = data;
            this.dens = c.getResources().getDisplayMetrics().density;
            p.setAntiAlias(true);
        }

        @Override protected void onDraw(Canvas cv) {
            int W = getWidth(), H = getHeight();
            float padL = 8 * dens, padR = 8 * dens, padB = 22 * dens, padT = 8 * dens;
            float plotH = H - padB - padT;
            double max = 1;
            for (double[] d : data) max = Math.max(max, Math.max(d[1], d[2]));
            max *= 1.15f;

            // 基线
            p.setColor(Ui.C_TRACK);
            p.setStrokeWidth(1);
            cv.drawLine(padL, H - padB, W - padR, H - padB, p);

            int n = data.size();
            float groupW = (W - padL - padR) / n;
            float barW = Math.min(groupW * 0.26f, 14 * dens);

            p.setTextSize(9 * dens);
            p.setTextAlign(Paint.Align.CENTER);

            for (int i = 0; i < n; i++) {
                double[] d = data.get(i);
                float cx = padL + groupW * i + groupW / 2f;
                float hExp = (float) (d[1] / max * plotH);
                float hInc = (float) (d[2] / max * plotH);

                p.setColor(Ui.C_RED);
                cv.drawRect(cx - barW - 1 * dens, H - padB - hExp,
                        cx - 1 * dens, H - padB, p);
                p.setColor(Ui.C_GREEN);
                cv.drawRect(cx + 1 * dens, H - padB - hInc,
                        cx + barW + 1 * dens, H - padB, p);

                p.setColor(Ui.C_SUB);
                java.util.Calendar m = java.util.Calendar.getInstance();
                m.setTimeInMillis((long) d[0]);
                String label = (m.get(Calendar.MONTH) + 1) + "月";
                cv.drawText(label, cx, H - 6 * dens, p);
            }
        }
    }
}
