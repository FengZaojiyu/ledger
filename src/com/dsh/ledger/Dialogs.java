package com.dsh.ledger;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 统一风格的圆角玻璃对话框，替代系统 AlertDialog */
public class Dialogs {

    public interface OnText {
        void onText(String s);
    }

    public interface OnItem {
        void onItem(int index, String text);
    }

    public static void confirm(final Context c, String title, String msg,
                               int okColor, final Runnable onOk) {
        final Dialog d = new Dialog(c, android.R.style.Theme_Translucent_NoTitleBar);
        LinearLayout root = card(c);

        TextView t = Ui.text(c, title, 16, Ui.C_TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(t);

        if (msg != null && msg.length() > 0) {
            TextView m = Ui.text(c, msg, 13.5f, Ui.C_SUB);
            m.setLineSpacing(Ui.dp(c, 3), 1f);
            m.setPadding(0, Ui.dp(c, 8), 0, 0);
            root.addView(m);
        }

        root.addView(buttonRow(c, d, "取消", "确定", okColor, new Runnable() {
            public void run() { if (onOk != null) onOk.run(); }
        }));
        show(d, c, root);
    }

    public static void input(final Context c, String title, String hint,
                             String initial, final boolean numeric,
                             final OnText onOk) {
        final Dialog d = new Dialog(c, android.R.style.Theme_Translucent_NoTitleBar);
        LinearLayout root = card(c);

        TextView t = Ui.text(c, title, 16, Ui.C_TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(t);

        final EditText e = new EditText(c);
        e.setHint(hint);
        e.setText(initial == null ? "" : initial);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setBackground(Ui.glassSoft(c, 12));
        e.setPadding(Ui.dp(c, 14), Ui.dp(c, 10), Ui.dp(c, 14), Ui.dp(c, 10));
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(c, 46));
        elp.topMargin = Ui.dp(c, 12);
        root.addView(e, elp);

        root.addView(buttonRow(c, d, "取消", "确定", Ui.C_ACCENT, new Runnable() {
            public void run() { if (onOk != null) onOk.onText(e.getText().toString().trim()); }
        }));
        show(d, c, root);
        e.requestFocus();
    }

    /** 长列表选择（模型列表之类），外框与其它对话框一致 */
    public static void list(final Context c, String title, String subtitle,
                            final String[] items, final OnItem onItem) {
        final Dialog d = new Dialog(c, android.R.style.Theme_Translucent_NoTitleBar);
        LinearLayout root = card(c);

        TextView t = Ui.text(c, title, 16, Ui.C_TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(t);
        if (subtitle != null && subtitle.length() > 0) {
            TextView s = Ui.text(c, subtitle, 12, Ui.C_SUB);
            s.setPadding(0, Ui.dp(c, 4), 0, 0);
            root.addView(s);
        }

        android.widget.ListView lv = new android.widget.ListView(c);
        lv.setDivider(new ColorDrawable(Ui.C_DIV));
        lv.setDividerHeight(1);
        lv.setBackgroundColor(Color.TRANSPARENT);
        lv.setVerticalScrollBarEnabled(true);
        lv.setAdapter(new android.widget.BaseAdapter() {
            public int getCount() { return items.length; }

            public Object getItem(int i) { return items[i]; }

            public long getItemId(int i) { return i; }

            public View getView(int i, View reuse, ViewGroup parent) {
                TextView v;
                if (reuse instanceof TextView) {
                    v = (TextView) reuse;
                } else {
                    v = new TextView(c);
                    v.setTextSize(14);
                    v.setTextColor(Ui.C_TEXT);
                    v.setPadding(Ui.dp(c, 6), Ui.dp(c, 13), Ui.dp(c, 6), Ui.dp(c, 13));
                }
                v.setText(items[i]);
                return v;
            }
        });
        lv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            public void onItemClick(android.widget.AdapterView<?> p, View v, int pos, long id) {
                d.dismiss();
                if (onItem != null) onItem.onItem(pos, items[pos]);
            }
        });

        int screenH = c.getResources().getDisplayMetrics().heightPixels;
        int h = Math.min(items.length * Ui.dp(c, 50), (int) (screenH * 0.52f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, h);
        lp.topMargin = Ui.dp(c, 10);
        root.addView(lv, lp);

        LinearLayout btns = new LinearLayout(c);
        btns.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        btns.setPadding(0, Ui.dp(c, 10), 0, 0);
        TextView cancel = Ui.text(c, "取消", 14, Ui.C_SUB);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(Ui.dp(c, 12), Ui.dp(c, 10), Ui.dp(c, 12), Ui.dp(c, 10));
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        btns.addView(cancel);
        root.addView(btns);

        show(d, c, root);
    }

    private static LinearLayout card(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        // 比普通玻璃更实一点：背后内容被 dim 压暗后仍然清楚，但不至于糊成一片
        android.graphics.drawable.GradientDrawable g =
                new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{Ui.C_DIALOG, Ui.C_DIALOG});
        g.setCornerRadius(Ui.dp(c, 24));
        g.setStroke(Math.max(1, Ui.dp(c, 1)), 0x40FFFFFF);
        v.setBackground(g);
        int p = Ui.dp(c, 20);
        v.setPadding(p, p, p, Ui.dp(c, 12));
        return v;
    }

    private static View buttonRow(final Context c, final Dialog d, String cancelText,
                                  String okText, int okColor, final Runnable onOk) {
        LinearLayout btns = new LinearLayout(c);
        btns.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        btns.setPadding(0, Ui.dp(c, 14), 0, 0);

        TextView cancel = Ui.text(c, cancelText, 14, Ui.C_SUB);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(Ui.dp(c, 12), Ui.dp(c, 10), Ui.dp(c, 12), Ui.dp(c, 10));
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        btns.addView(cancel);

        TextView ok = Ui.text(c, okText, 14, Color.WHITE);
        ok.setGravity(Gravity.CENTER);
        ok.setTypeface(Typeface.DEFAULT_BOLD);
        ok.setBackground(Ui.roundRect(okColor, c, 12));
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                d.dismiss();
                if (onOk != null) onOk.run();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(c, 86), Ui.dp(c, 40));
        lp.leftMargin = Ui.dp(c, 8);
        btns.addView(ok, lp);
        return btns;
    }

    private static void show(Dialog d, Context c, View root) {
        d.setContentView(root);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.38f;
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            w.setAttributes(lp);
        }
        d.show();
        if (w != null) {
            int width = (int) (c.getResources().getDisplayMetrics().widthPixels * 0.88f);
            w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
