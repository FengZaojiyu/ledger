package com.dsh.ledger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;

public class SettingsActivity extends Activity {

    private static final String[] PALETTE = {
            "🍜", "🍚", "🍔", "🍰", "☕", "🍺",
            "🚌", "🚇", "🚕", "✈️", "🛍️", "👕",
            "👟", "💄", "🎮", "🎬", "🎵", "📚",
            "🏠", "🛏️", "💡", "🧴", "🚿", "💊",
            "🏥", "🏋️", "⚽", "🎁", "🧧", "💰",
            "💼", "💻", "📱", "🎓", "🐱", "🐶",
            "👶", "🧸", "🎨", "🔧", "📦", "➕"
    };

    private EditText budgetInput, newCatInput;
    private EditText aiBase, aiKey, aiModel;
    private String aiEffort = "";
    private TextView[] effortChips;
    private static final String[] EFFORT_LABEL = {"默认", "低", "中", "高"};
    private static final String[] EFFORT_VALUE = {"", "low", "medium", "high"};
    private LinearLayout catBox;
    private int catType = Db.EXPENSE;
    private TextView tabExp, tabInc, newCatEmoji;
    private TextView islandSwitch;

    @Override
    protected void onCreate(Bundle b) {
        Ui.applyTheme(this);
        super.onCreate(b);
        setContentView(build());
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
        TextView title = Ui.text(this, "设置", 17, Ui.C_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1f));
        bar.addView(new LinearLayout(this), new LinearLayout.LayoutParams(Ui.dp(this, 80), Ui.dp(this, 32)));
        root.addView(bar);

        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 20));

        box.addView(section("外观"));
        LinearLayout themeCard = card();
        LinearLayout themeRow = new LinearLayout(this);
        final String[] themeKeys = {"system", "light", "dark"};
        final String[] themeNames = {"跟随系统", "浅色", "深色"};
        String cur = Ui.themeMode(this);
        for (int i = 0; i < themeKeys.length; i++) {
            final String key = themeKeys[i];
            boolean on = key.equals(cur);
            TextView chip = Ui.button(this, themeNames[i],
                    on ? Ui.C_ACCENT : Ui.C_SOFT_BTN, on ? Color.WHITE : Ui.C_SUB, 13);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Ui.setThemeMode(SettingsActivity.this, key);
                    recreate();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 38), 1f);
            if (i > 0) lp.leftMargin = Ui.dp(this, 6);
            themeRow.addView(chip, lp);
        }
        themeCard.addView(themeRow);
        box.addView(wrap(themeCard));

        box.addView(section("月度预算"));
        LinearLayout budgetCard = card();
        LinearLayout budRow = new LinearLayout(this);
        budRow.setGravity(Gravity.CENTER_VERTICAL);
        budRow.addView(Ui.text(this, "¥", 20, Ui.C_TEXT));
        budgetInput = new EditText(this);
        budgetInput.setHint("0 表示不设预算");
        budgetInput.setTextSize(20);
        budgetInput.setBackground(null);
        budgetInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        budgetInput.setSingleLine(true);
        budRow.addView(budgetInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView saveBudget = Ui.button(this, "保存", Ui.C_ACCENT, Color.WHITE, 14);
        saveBudget.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { saveBudget(); }
        });
        budRow.addView(saveBudget, new LinearLayout.LayoutParams(Ui.dp(this, 76), Ui.dp(this, 40)));
        budgetCard.addView(budRow);
        box.addView(wrap(budgetCard));

        box.addView(section("分类管理"));
        LinearLayout catCard = card();
        LinearLayout tabs = new LinearLayout(this);
        tabs.setBackground(Ui.glassSoft(this, 12));
        tabs.setPadding(Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3));
        tabExp = Ui.text(this, "支出分类", 14, Color.WHITE);
        tabExp.setGravity(Gravity.CENTER);
        tabInc = Ui.text(this, "收入分类", 14, Ui.C_SUB);
        tabInc.setGravity(Gravity.CENTER);
        tabExp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { catType = Db.EXPENSE; refresh(); }
        });
        tabInc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { catType = Db.INCOME; refresh(); }
        });
        tabs.addView(tabExp, new LinearLayout.LayoutParams(0, Ui.dp(this, 36), 1f));
        tabs.addView(tabInc, new LinearLayout.LayoutParams(0, Ui.dp(this, 36), 1f));
        catCard.addView(tabs);

        catBox = new LinearLayout(this);
        catBox.setOrientation(LinearLayout.VERTICAL);
        catBox.setPadding(0, Ui.dp(this, 6), 0, 0);
        catCard.addView(catBox);

        LinearLayout addRow = new LinearLayout(this);
        addRow.setGravity(Gravity.CENTER_VERTICAL);
        addRow.setPadding(0, Ui.dp(this, 10), 0, 0);

        newCatEmoji = Ui.text(this, "📦", 20, Ui.C_TEXT);
        newCatEmoji.setGravity(Gravity.CENTER);
        newCatEmoji.setBackground(Ui.glassSoft(this, 10));
        newCatEmoji.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickEmoji("选择图标", newCatEmoji.getText().toString(), null); }
        });
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42));
        ep.rightMargin = Ui.dp(this, 8);
        addRow.addView(newCatEmoji, ep);

        newCatInput = new EditText(this);
        newCatInput.setHint("新分类名称");
        newCatInput.setTextSize(14);
        newCatInput.setSingleLine(true);
        newCatInput.setBackground(Ui.glassSoft(this, 10));
        newCatInput.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        addRow.addView(newCatInput, new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1f));

        TextView addBtn = Ui.button(this, "添加", Ui.C_ACCENT, Color.WHITE, 14);
        addBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addCategory(); }
        });
        LinearLayout.LayoutParams abp = new LinearLayout.LayoutParams(Ui.dp(this, 68), Ui.dp(this, 42));
        abp.leftMargin = Ui.dp(this, 8);
        addRow.addView(addBtn, abp);
        catCard.addView(addRow);
        box.addView(wrap(catCard));

        box.addView(section("超级岛 / 通知"));
        LinearLayout islandCard = card();
        LinearLayout islandRow = new LinearLayout(this);
        islandRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout islandText = new LinearLayout(this);
        islandText.setOrientation(LinearLayout.VERTICAL);
        islandText.addView(Ui.text(this, "在超级岛上显示预算进度", 14, Ui.C_TEXT));
        islandText.addView(Ui.text(this, "HyperOS 超级岛 / 状态栏常驻，实时更新本月已花与预算占比", 11, Ui.C_SUB));
        islandRow.addView(islandText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        islandSwitch = Ui.text(this, "", 13, Ui.C_ACCENT);
        islandSwitch.setGravity(Gravity.CENTER);
        islandSwitch.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        islandRow.addView(islandSwitch);
        islandRow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { toggleIsland(); }
        });
        islandCard.addView(islandRow);

        LinearLayout devRow = new LinearLayout(this);
        devRow.setOrientation(LinearLayout.VERTICAL);
        devRow.setPadding(0, Ui.dp(this, 10), 0, 0);
        devRow.addView(Ui.text(this, "设备识别", 12, Ui.C_SUB));
        devRow.addView(Ui.text(this, Device.describe(), 13, Ui.C_TEXT));
        TextView sup = Ui.text(this, Device.supportLine(), 11, Ui.C_ACCENT);
        sup.setPadding(0, Ui.dp(this, 3), 0, 0);
        devRow.addView(sup);
        islandCard.addView(devRow);

        box.addView(wrap(islandCard));

        box.addView(section("AI 助手"));
        LinearLayout aiCard = card();
        aiCard.addView(Ui.text(this, "OpenAI 兼容接口（DeepSeek / 通义 / Kimi / 本地 Ollama 都可以）", 12, Ui.C_SUB));
        aiBase = aiField(aiCard, "接口地址（Base URL）", "https://api.deepseek.com/v1", false);
        aiKey = aiField(aiCard, "API Key", "sk-...", true);

        TextView ml = Ui.text(this, "模型名", 12, Ui.C_SUB);
        ml.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        aiCard.addView(ml);
        LinearLayout modelRow = new LinearLayout(this);
        modelRow.setGravity(Gravity.CENTER_VERTICAL);
        aiModel = new EditText(this);
        aiModel.setHint("deepseek-chat");
        aiModel.setTextSize(13);
        aiModel.setSingleLine(true);
        aiModel.setBackground(Ui.glassSoft(this, 10));
        aiModel.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        modelRow.addView(aiModel, new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1f));
        TextView fetch = Ui.button(this, "拉取", Ui.C_SOFT_BTN, Ui.C_ACCENT, 13);
        fetch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { fetchModels(); }
        });
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 42));
        fp.leftMargin = Ui.dp(this, 8);
        modelRow.addView(fetch, fp);
        aiCard.addView(modelRow);

        TextView el = Ui.text(this, "思考强度", 12, Ui.C_SUB);
        el.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 6));
        aiCard.addView(el);
        LinearLayout effRow = new LinearLayout(this);
        effortChips = new TextView[EFFORT_LABEL.length];
        for (int i = 0; i < EFFORT_LABEL.length; i++) {
            final int idx = i;
            final TextView chip = Ui.button(this, EFFORT_LABEL[i], Ui.C_SOFT_BTN, Ui.C_SUB, 12);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    aiEffort = EFFORT_VALUE[idx];
                    syncEffort();
                }
            });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, Ui.dp(this, 34), 1f);
            cp.rightMargin = Ui.dp(this, 6);
            effRow.addView(chip, cp);
            effortChips[i] = chip;
        }
        aiCard.addView(effRow);
        aiCard.addView(Ui.text(this, "「低/中/高」会以 reasoning_effort 传给接口；模型不支持时选「默认」", 11, Ui.C_SUB));

        LinearLayout aiRow = new LinearLayout(this);
        aiRow.setGravity(Gravity.CENTER_VERTICAL);
        aiRow.setPadding(0, Ui.dp(this, 10), 0, 0);
        TextView saveAi = Ui.button(this, "保存配置", Ui.C_ACCENT, Color.WHITE, 14);
        saveAi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { saveAi(); }
        });
        aiRow.addView(saveAi, new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1f));
        TextView openAi = Ui.button(this, "打开 AI 对话", Ui.C_SOFT_BTN, Ui.C_ACCENT, 14);
        openAi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new android.content.Intent(SettingsActivity.this, ChatActivity.class));
            }
        });
        LinearLayout.LayoutParams oap = new LinearLayout.LayoutParams(0, Ui.dp(this, 42), 1f);
        oap.leftMargin = Ui.dp(this, 8);
        aiRow.addView(openAi, oap);
        aiCard.addView(aiRow);
        box.addView(wrap(aiCard));

        box.addView(section("导出数据"));
        LinearLayout expCard = card();
        expCard.addView(Ui.text(this, "保存到 手机存储/Download/小账本/", 12, Ui.C_SUB));
        expCard.addView(exportRow("本月", false));
        expCard.addView(exportRow("全部记录", true));
        box.addView(wrap(expCard));

        sv.addView(box);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.imeAware(root);
        return root;
    }

    private TextView section(String s) {
        TextView t = Ui.text(this, s, 13, Ui.C_SUB);
        t.setPadding(Ui.dp(this, 4), Ui.dp(this, 10), 0, Ui.dp(this, 8));
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.glass(this, 20));
        c.setElevation(Ui.dp(this, 7));
        c.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        return c;
    }

    private LinearLayout wrap(View v) {
        LinearLayout w = new LinearLayout(this);
        w.addView(v, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return w;
    }

    private View exportRow(String label, final boolean all) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 8), 0, 0);
        row.addView(Ui.text(this, label, 14, Ui.C_TEXT),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final String[] fmts = {"CSV", "Excel", "Word", "PDF"};
        for (int i = 0; i < fmts.length; i++) {
            final String f = fmts[i];
            TextView b = Ui.button(this, f, Ui.C_SOFT_BTN, Ui.C_ACCENT, 12);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(this, 34));
            lp.leftMargin = Ui.dp(this, 6);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { export(f, all); }
            });
            row.addView(b, lp);
        }
        return row;
    }

    private void refresh() {
        refreshIsland();
        if (aiBase != null) {
            android.content.SharedPreferences p = getSharedPreferences("ledger", MODE_PRIVATE);
            aiBase.setText(p.getString("ai_base", ""));
            aiKey.setText(p.getString("ai_key", ""));
            aiModel.setText(p.getString("ai_model", "deepseek-chat"));
            aiEffort = p.getString("ai_effort", "");
            syncEffort();
        }
        budgetInput.setText(Db.get(this).budget() > 0
                ? String.format(java.util.Locale.CHINA, "%.0f", Db.get(this).budget()) : "");
        tabExp.setBackground(Ui.roundRect(catType == Db.EXPENSE ? Ui.C_ACCENT : 0, this, 8));
        tabExp.setTextColor(catType == Db.EXPENSE ? Color.WHITE : Ui.C_SUB);
        tabInc.setBackground(Ui.roundRect(catType == Db.INCOME ? Ui.C_ACCENT : 0, this, 8));
        tabInc.setTextColor(catType == Db.INCOME ? Color.WHITE : Ui.C_SUB);

        catBox.removeAllViews();
        List<String[]> rows = Db.get(this).categoryRows(catType);
        for (final String[] row : rows) {
            final String n = row[0];
            final String em = row[1];
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));

            TextView icon = Ui.text(this, em, 19, Ui.C_TEXT);
            icon.setGravity(Gravity.CENTER);
            icon.setBackground(Ui.glassSoft(this, 9));
            icon.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { pickEmoji("「" + n + "」的图标", em, n); }
            });
            line.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 38), Ui.dp(this, 38)));

            TextView name = Ui.text(this, n, 14, Ui.C_TEXT);
            name.setPadding(Ui.dp(this, 12), 0, 0, 0);
            name.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { renameCategory(n); }
            });
            line.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView del = Ui.text(this, "删除", 12, Ui.C_RED);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { confirmDel(n); }
            });
            line.addView(del);
            catBox.addView(line);
        }
    }

    /** 打开图标选择器；catName 为 null 表示给「新增分类」选图标 */
    private void pickEmoji(String title, String current, final String catName) {
        GridLayout g = new GridLayout(this);
        g.setColumnCount(6);
        g.setPadding(Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6));
        final AlertDialog[] holder = new AlertDialog[1];
        for (final String e : PALETTE) {
            TextView t = Ui.text(this, e, 23, Ui.C_TEXT);
            t.setGravity(Gravity.CENTER);
            t.setBackground(Ui.roundRect(e.equals(current) ? Ui.C_CHIP_ON : 0x00000000, this, 10));
            t.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.height = GridLayout.LayoutParams.WRAP_CONTENT;
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            t.setLayoutParams(p);
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (catName == null) {
                        newCatEmoji.setText(e);
                    } else {
                        Db.get(SettingsActivity.this).setEmoji(catName, catType, e);
                        refresh();
                    }
                    if (holder[0] != null) holder[0].dismiss();
                }
            });
            g.addView(t);
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(g);
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(sv)
                .setNegativeButton("取消", null)
                .create();
        holder[0] = d;
        d.show();
    }

    private EditText aiField(LinearLayout parent, String label, String hint, boolean secret) {
        TextView l = Ui.text(this, label, 12, Ui.C_SUB);
        l.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        parent.addView(l);
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(13);
        e.setSingleLine(true);
        e.setBackground(Ui.glassSoft(this, 10));
        e.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        parent.addView(e, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 42)));
        return e;
    }

    private void refreshIsland() {
        if (islandSwitch == null) return;
        boolean on = FocusNotify.enabled(this);
        islandSwitch.setText(on ? "已开启" : "已关闭");
        islandSwitch.setBackground(Ui.roundRect(on ? Ui.C_CHIP_ON : Ui.C_CHIP, this, 12));
        islandSwitch.setTextColor(on ? Ui.C_ACCENT : Ui.C_SUB);
    }

    private void toggleIsland() {
        boolean on = !FocusNotify.enabled(this);
        if (on && !FocusNotify.canPost(this)) {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 12);
            }
            toast("需要通知权限");
            return;
        }
        FocusNotify.setEnabled(this, on);
        refreshIsland();
        toast(on ? "已开启，看状态栏/超级岛" : "已关闭");
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == 12 && FocusNotify.canPost(this)) {
            FocusNotify.setEnabled(this, true);
            refreshIsland();
            FocusNotify.update(this);
        }
    }

    private void syncEffort() {
        if (effortChips == null) return;
        for (int i = 0; i < effortChips.length; i++) {
            boolean on = EFFORT_VALUE[i].equals(aiEffort);
            effortChips[i].setTextColor(on ? Color.WHITE : Ui.C_SUB);
            effortChips[i].setBackground(on
                    ? Ui.roundRect(Ui.C_ACCENT, this, 10)
                    : Ui.glassSoft(this, 10));
        }
    }

    private void fetchModels() {
        final String base = aiBase.getText().toString().trim();
        final String key = aiKey.getText().toString().trim();
        if (base.length() == 0 || key.length() == 0) {
            toast("先填接口地址和密钥");
            return;
        }
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("正在拉取模型列表…");
        pd.setCancelable(false);
        pd.show();
        new Thread(new Runnable() {
            public void run() {
                try {
                    final java.util.List<String> ids = AiClient.models(base, key);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            pd.dismiss();
                            if (ids.isEmpty()) { toast("接口没返回任何模型"); return; }
                            Dialogs.list(SettingsActivity.this,
                                    "选择模型", "共 " + ids.size() + " 个，来自 "
                                            + aiBase.getText().toString().trim(),
                                    ids.toArray(new String[0]), new Dialogs.OnItem() {
                                        public void onItem(int index, String text) {
                                            aiModel.setText(text);
                                        }
                                    });
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            pd.dismiss();
                            toast("拉取失败：" + (e.getMessage() == null ? "网络错误" : e.getMessage()));
                        }
                    });
                }
            }
        }).start();
    }

    private void saveAi() {
        String base = aiBase.getText().toString().trim();
        String key = aiKey.getText().toString().trim();
        String model = aiModel.getText().toString().trim();
        if (model.length() == 0) model = "deepseek-chat";
        if (base.length() == 0 && key.length() == 0) {
            getSharedPreferences("ledger", MODE_PRIVATE).edit()
                    .remove("ai_base").remove("ai_key").apply();
            toast("已清除 AI 配置");
            return;
        }
        if (base.length() == 0 || key.length() == 0) {
            toast("接口地址和密钥都要填");
            return;
        }
        getSharedPreferences("ledger", MODE_PRIVATE).edit()
                .putString("ai_base", base)
                .putString("ai_key", key)
                .putString("ai_model", model)
                .putString("ai_effort", aiEffort)
                .apply();
        toast("已保存，去「打开 AI 对话」试试");
    }

    private void saveBudget() {
        String s = budgetInput.getText().toString().trim();
        double v = 0;
        if (s.length() > 0) {
            try { v = Double.parseDouble(s); } catch (Exception e) { toast("数字格式不对"); return; }
        }
        Db.get(this).setBudget(v);
        toast(v > 0 ? "预算已设为 ¥" + Ui.money(v) : "已取消预算");
    }

    private void addCategory() {
        String name = newCatInput.getText().toString().trim();
        if (name.length() == 0) { toast("请输入名称"); return; }
        if (Db.get(this).categories(catType).contains(name)) { toast("已存在"); return; }
        Db.get(this).addCategory(name, catType, newCatEmoji.getText().toString());
        newCatInput.setText("");
        refresh();
        toast("已添加");
    }

    private void renameCategory(final String oldName) {
        Dialogs.input(this, "重命名分类", "新名称", oldName, false, new Dialogs.OnText() {
            public void onText(String s) {
                if (s.length() == 0 || s.equals(oldName)) return;
                if (Db.get(SettingsActivity.this).categories(catType).contains(s)) {
                    toast("已有同名分类");
                    return;
                }
                Db.get(SettingsActivity.this).renameCategory(oldName, catType, s);
                refresh();
                toast("已重命名，历史记录同步更新");
            }
        });
    }

    private void confirmDel(final String name) {
        final List<String> names = Db.get(this).categories(catType);
        if (names.size() <= 1) { toast("至少保留一个分类"); return; }
        Dialogs.confirm(this, "删除分类", "「" + name + "」下的记录会归入「"
                + (catType == Db.EXPENSE ? "其他" : "其他收入") + "」。", Ui.C_RED, new Runnable() {
            public void run() {
                Db.get(SettingsActivity.this).deleteCategory(-1, name, catType);
                refresh();
            }
        });
    }

    private void export(final String fmt, final boolean all) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("正在生成 " + fmt + " …");
        pd.setCancelable(false);
        pd.show();
        new Thread(new Runnable() {
            public void run() {
                String msg;
                try {
                    Calendar now = Calendar.getInstance();
                    long[] r = all ? new long[]{0, System.currentTimeMillis() + 86400000L} : Ui.monthRange(now);
                    List<Rec> list = Db.get(SettingsActivity.this).list(r[0], r[1]);
                    String ts = Export.stamp();
                    String scope = all ? "全部" : new java.text.SimpleDateFormat("yyyyMM", java.util.Locale.CHINA).format(now.getTime());
                    String title = "小账本 " + (all ? "全部记录" : scope) + " 导出";
                    String name;
                    String mime;
                    byte[] data;
                    if ("CSV".equals(fmt)) {
                        name = "小账本-" + scope + "-" + ts + ".csv";
                        mime = Export.MIME_CSV;
                        data = Export.csv(list);
                    } else if ("Excel".equals(fmt)) {
                        name = "小账本-" + scope + "-" + ts + ".xlsx";
                        mime = Export.MIME_XLSX;
                        data = Export.xlsx(list, title);
                    } else if ("Word".equals(fmt)) {
                        name = "小账本-" + scope + "-" + ts + ".docx";
                        mime = Export.MIME_DOCX;
                        data = Export.docx(list, title);
                    } else {
                        name = "小账本-" + scope + "-" + ts + ".pdf";
                        mime = Export.MIME_PDF;
                        data = Export.pdf(list, title);
                    }
                    msg = "已保存：" + Export.save(SettingsActivity.this, name, mime, data)
                            + "（" + list.size() + " 条）";
                } catch (Exception e) {
                    msg = "导出失败：" + e.getMessage();
                }
                final String fm = msg;
                runOnUiThread(new Runnable() {
                    public void run() {
                        pd.dismiss();
                        toast(fm);
                    }
                });
            }
        }).start();
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_LONG).show();
    }
}
