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
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

public class AddActivity extends Activity {

    private int type = Db.EXPENSE;
    private String cat = "";
    private long editId = 0;
    private final Calendar day = Calendar.getInstance();

    private EditText amountInput, noteInput;
    private TextView tabExp, tabInc, dateView;
    private GridLayout grid;

    @Override
    protected void onCreate(Bundle b) {
        Ui.applyTheme(this);
        super.onCreate(b);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        editId = getIntent().getLongExtra("id", 0);
        setContentView(build());
        if (editId > 0) load();
    }

    private void load() {
        Rec r = Db.get(this).find(editId);
        if (r == null) { finish(); return; }
        type = r.type;
        cat = r.cat;
        day.setTimeInMillis(r.ts);
        amountInput.setText(String.format(java.util.Locale.CHINA, "%.2f", r.amount));
        amountInput.setSelection(amountInput.getText().length());
        noteInput.setText(r.note == null ? "" : r.note);
        updateDate();
        syncTabs();
        fillGrid();
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        Ui.polish(this, root);

        // 顶栏
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 8));
        TextView cancel = Ui.text(this, "取消", 15, Ui.C_SUB);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        bar.addView(cancel, new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1f));
        TextView title = Ui.text(this, editId > 0 ? "编辑" : "记一笔", 17, Ui.C_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 2f));
        bar.addView(new LinearLayout(this),
                new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1f));
        root.addView(bar);

        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 16));

        // 支出 / 收入
        LinearLayout tabs = new LinearLayout(this);
        tabs.setBackground(Ui.glassSoft(this, 14));
        tabs.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        tabExp = tab("支出");
        tabInc = tab("收入");
        tabExp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchType(Db.EXPENSE); }
        });
        tabInc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchType(Db.INCOME); }
        });
        tabs.addView(tabExp, new LinearLayout.LayoutParams(0, Ui.dp(this, 38), 1f));
        tabs.addView(tabInc, new LinearLayout.LayoutParams(0, Ui.dp(this, 38), 1f));
        LinearLayout tabsWrap = new LinearLayout(this);
        tabsWrap.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 14));
        tabsWrap.addView(tabs, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(tabsWrap);

        // 金额
        LinearLayout amountRow = new LinearLayout(this);
        amountRow.setOrientation(LinearLayout.HORIZONTAL);
        amountRow.setGravity(Gravity.CENTER_VERTICAL);
        amountRow.setBackground(Ui.glass(this, 20));
        amountRow.setElevation(Ui.dp(this, 7));
        amountRow.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 16));
        TextView yuan = Ui.text(this, "¥", 26, Ui.C_TEXT);
        yuan.setTypeface(Typeface.DEFAULT_BOLD);
        amountRow.addView(yuan);
        amountInput = new EditText(this);
        amountInput.setHint("0.00");
        amountInput.setTextSize(34);
        amountInput.setTextColor(Ui.C_TEXT);
        amountInput.setHintTextColor(Ui.C_HINT);
        amountInput.setBackground(null);
        amountInput.setPadding(Ui.dp(this, 8), 0, 0, 0);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setSingleLine(true);
        amountRow.addView(amountInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout aw = new LinearLayout(this);
        aw.setPadding(0, 0, 0, Ui.dp(this, 14));
        aw.addView(amountRow, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(aw);

        // 分类
        LinearLayout catHead = new LinearLayout(this);
        catHead.setGravity(Gravity.CENTER_VERTICAL);
        catHead.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 8));
        catHead.addView(Ui.text(this, "分类", 13, Ui.C_SUB),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView manage = Ui.text(this, "管理", 13, Ui.C_ACCENT);
        manage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new android.content.Intent(AddActivity.this, SettingsActivity.class));
            }
        });
        catHead.addView(manage);
        box.addView(catHead);

        grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setBackground(Ui.glass(this, 20));
        grid.setElevation(Ui.dp(this, 7));
        grid.setPadding(Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12));
        LinearLayout gw = new LinearLayout(this);
        gw.setPadding(0, 0, 0, Ui.dp(this, 14));
        gw.addView(grid, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(gw);

        // 备注
        LinearLayout noteRow = new LinearLayout(this);
        noteRow.setOrientation(LinearLayout.VERTICAL);
        noteRow.setBackground(Ui.glass(this, 20));
        noteRow.setElevation(Ui.dp(this, 7));
        noteRow.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 10));
        noteInput = new EditText(this);
        noteInput.setHint("备注（可选）");
        noteInput.setTextSize(15);
        noteInput.setBackground(null);
        noteInput.setSingleLine(true);
        noteInput.setHintTextColor(Ui.C_HINT);
        noteRow.addView(noteInput);
        LinearLayout nw = new LinearLayout(this);
        nw.setPadding(0, 0, 0, Ui.dp(this, 14));
        nw.addView(noteRow, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(nw);

        // 日期
        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);
        dateRow.setBackground(Ui.glass(this, 20));
        dateRow.setElevation(Ui.dp(this, 7));
        dateRow.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        dateRow.addView(Ui.text(this, "日期", 15, Ui.C_TEXT),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        dateView = Ui.text(this, "", 15, Ui.C_ACCENT);
        dateRow.addView(dateView);
        dateRow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickDate(); }
        });
        box.addView(dateRow, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView save = Ui.button(this, "保存", Ui.C_ACCENT, Color.WHITE, 16);
        save.setTypeface(Typeface.DEFAULT_BOLD);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { save(); }
        });
        LinearLayout sw = new LinearLayout(this);
        sw.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 8));
        sw.addView(save, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        box.addView(sw);

        if (editId > 0) {
            TextView delBtn = Ui.text(this, "删除这条记录", 14, Ui.C_RED);
            delBtn.setGravity(Gravity.CENTER);
            delBtn.setBackground(Ui.glassSoft(this, 14));
            delBtn.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
            delBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { confirmDelete(); }
            });
            LinearLayout dw = new LinearLayout(this);
            dw.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 6));
            dw.addView(delBtn, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            box.addView(dw);
        }

        sv.addView(box);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        syncTabs();
        fillGrid();
        updateDate();
        amountInput.requestFocus();
        Ui.imeAware(root);
        return root;
    }

    private TextView tab(String label) {
        TextView t = Ui.text(this, label, 15, Ui.C_SUB);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private void syncTabs() {
        tabExp.setBackground(Ui.roundRect(type == Db.EXPENSE ? Ui.C_ACCENT : 0x00000000, this, 9));
        tabExp.setTextColor(type == Db.EXPENSE ? Color.WHITE : Ui.C_SUB);
        tabInc.setBackground(Ui.roundRect(type == Db.INCOME ? Ui.C_ACCENT : 0x00000000, this, 9));
        tabInc.setTextColor(type == Db.INCOME ? Color.WHITE : Ui.C_SUB);
    }

    private void switchType(int t) {
        if (type == t) return;
        type = t;
        cat = "";
        syncTabs();
        fillGrid();
    }

    private void fillGrid() {
        if (grid == null) return;
        grid.removeAllViews();
        List<String[]> list = Db.get(this).categoryRows(type);
        boolean found = false;
        for (String[] row : list) if (row[0].equals(cat)) found = true;
        if (cat.length() == 0 || !found) {
            cat = list.isEmpty() ? "其他" : list.get(0)[0];
        }
        int cols = 4;
        for (int i = 0; i < list.size(); i++) {
            final String name = list.get(i)[0];
            final String em = list.get(i)[1];
            final boolean sel = name.equals(cat);
            TextView chip = Ui.text(this, em + " " + name, 13, sel ? Ui.C_ACCENT : Ui.C_TEXT);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
            chip.setBackground(Ui.roundRect(sel ? Ui.C_CHIP_ON : Ui.C_CHIP, this, 10));
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.height = GridLayout.LayoutParams.WRAP_CONTENT;
            p.columnSpec = GridLayout.spec(i % cols, 1f);
            p.rowSpec = GridLayout.spec(i / cols);
            p.setMargins(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
            chip.setLayoutParams(p);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    cat = name;
                    fillGrid();
                }
            });
            grid.addView(chip);
        }
    }

    private void pickDate() {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int y, int m, int d) {
                day.set(Calendar.YEAR, y);
                day.set(Calendar.MONTH, m);
                day.set(Calendar.DAY_OF_MONTH, d);
                updateDate();
            }
        }, day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDate() {
        if (dateView != null)
            dateView.setText(new java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.CHINA).format(day.getTime()));
    }

    private void save() {
        String s = amountInput.getText().toString().trim();
        if (s.length() == 0) { toast("请输入金额"); return; }
        double v;
        try { v = Double.parseDouble(s); } catch (Exception e) { toast("金额格式不对"); return; }
        if (v <= 0) { toast("金额要大于 0"); return; }
        String note = noteInput.getText().toString().trim();
        if (editId > 0) {
            Db.get(this).update(editId, v, type, cat, note, day.getTimeInMillis());
            toast("已更新");
        } else {
            Db.get(this).insert(v, type, cat, note, day.getTimeInMillis());
            toast("已保存");
        }
        FocusNotify.update(this);
        finish();
    }

    private void confirmDelete() {
        Dialogs.confirm(this, "删除记录", "这条记录会被永久删除。", Ui.C_RED, new Runnable() {
            public void run() {
                Db.get(AddActivity.this).delete(editId);
                finish();
            }
        });
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
