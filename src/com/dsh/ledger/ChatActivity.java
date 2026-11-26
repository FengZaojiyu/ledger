package com.dsh.ledger;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends Activity {

    private LinearLayout msgBox;
    private ScrollView scroll;
    private EditText input;
    private TextView sendBtn;
    private final List<AiClient.Msg> history = new ArrayList<AiClient.Msg>();
    private boolean busy = false;

    @Override
    protected void onCreate(Bundle b) {
        Ui.applyTheme(this);
        super.onCreate(b);
        setContentView(build());
        greet();
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        Ui.polish(this, root);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 6));
        TextView back = Ui.text(this, "‹ 返回", 15, Ui.C_ACCENT);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        bar.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 80), Ui.dp(this, 32)));
        TextView title = Ui.text(this, "AI 助手", 17, Ui.C_TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        bar.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1f));
        TextView cfg = Ui.text(this, "设置", 13, Ui.C_ACCENT);
        cfg.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        cfg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(ChatActivity.this, SettingsActivity.class));
            }
        });
        bar.addView(cfg, new LinearLayout.LayoutParams(Ui.dp(this, 80), Ui.dp(this, 32)));
        root.addView(bar);

        scroll = new ScrollView(this);
        msgBox = new LinearLayout(this);
        msgBox.setOrientation(LinearLayout.VERTICAL);
        msgBox.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 10));
        scroll.addView(msgBox);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // 快捷问题
        LinearLayout chips = new LinearLayout(this);
        chips.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 6));
        chips.addView(chip("本月分析", "帮我分析一下这个月的支出结构，哪里花得最多，有什么问题？"));
        chips.addView(chip("省钱建议", "根据我的记账数据，给我 3 条具体可执行的省钱建议。"));
        root.addView(chips);

        // 输入区
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 16));
        input = new EditText(this);
        input.setHint("问点什么，比如「这个月还能花多少」");
        input.setTextSize(14);
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setBackground(Ui.glassSoft(this, 16));
        input.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        row.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        sendBtn = Ui.button(this, "发送", Ui.C_ACCENT, Color.WHITE, 14);
        sendBtn.setTypeface(Typeface.DEFAULT_BOLD);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { send(null); }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(Ui.dp(this, 68), Ui.dp(this, 42));
        sp.leftMargin = Ui.dp(this, 8);
        row.addView(sendBtn, sp);
        root.addView(row);
        Ui.imeAware(root);
        return root;
    }

    private View chip(final String label, final String question) {
        TextView t = Ui.button(this, label, Ui.C_SOFT_BTN, Ui.C_ACCENT, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(this, 34));
        lp.rightMargin = Ui.dp(this, 8);
        t.setLayoutParams(lp);
        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { send(question); }
        });
        return t;
    }

    private SharedPreferences prefs() {
        return getSharedPreferences("ledger", MODE_PRIVATE);
    }

    private boolean configured() {
        String base = prefs().getString("ai_base", "");
        String key = prefs().getString("ai_key", "");
        return base != null && base.trim().length() > 0
                && key != null && key.trim().length() > 0;
    }

    private void greet() {
        if (!configured()) {
            bubble("还没有接入 AI。点右上角「设置」→ AI 助手，填 API 地址、密钥和模型名（OpenAI 兼容接口都行，比如 DeepSeek、通义、Kimi、本地 Ollama）。", false, true);
            bubble("配好之后，我可以读到你本月的收支、分类明细和最近记录，帮你分析、给建议、回答具体问题。", false, true);
            bubble("回复会按 Markdown 排版，长这样：\n\n**本月支出 20.87 元**\n- 餐饮 20.87（占 100%）\n- 其余分类无记录\n\n（下面不会再出现星号）", false, true);
        } else {
            bubble("已接入。你可以直接问我账本上的事，比如「这个月哪类花得最多」「按现在的速度月底会超支吗」。", false, true);
            bubble("回复会按 Markdown 排版：**关键数字加粗**、\n- 列表变成圆点\n- 行内 `代码` 用等宽字体\n\n星号不会直接显示出来。", false, true);
        }
    }

    private void send(String preset) {
        if (busy) return;
        final String q = preset != null ? preset : input.getText().toString().trim();
        if (q.length() == 0) return;
        if (!configured()) {
            toast("先去设置里填 API 地址和密钥");
            return;
        }
        input.setText("");
        bubble(q, true, false);
        history.add(new AiClient.Msg("user", q));

        busy = true;
        sendBtn.setAlpha(0.5f);
        final TextView thinking = bubble("思考中…", false, true);

        new Thread(new Runnable() {
            public void run() {
                String reply;
                try {
                    String base = prefs().getString("ai_base", "");
                    String key = prefs().getString("ai_key", "");
                    String model = prefs().getString("ai_model", "deepseek-chat");
                    String effort = prefs().getString("ai_effort", "");
                    List<AiClient.Msg> msgs = new ArrayList<AiClient.Msg>();
                    msgs.add(new AiClient.Msg("system",
                            "你是「小账本」App 里的个人财务助手。下面是用户的真实记账数据，回答要具体、"
                            + "引用真实数字、给出可执行建议，不要编造数据。金额单位是人民币元。\n\n"
                            + DataSnapshot.build(ChatActivity.this)));
                    msgs.addAll(history);
                    reply = AiClient.chat(base, key, model, msgs, effort);
                } catch (Exception e) {
                    reply = "⚠️ " + (e.getMessage() == null ? "请求失败" : e.getMessage());
                }
                final String r = reply;
                runOnUiThread(new Runnable() {
                    public void run() {
                        busy = false;
                        sendBtn.setAlpha(1f);
                        msgBox.removeView(thinking);
                        boolean err = r.startsWith("⚠️");
                        bubble(r, false, true);
                        if (!err) history.add(new AiClient.Msg("assistant", r));
                        scroll.post(new Runnable() {
                            public void run() { scroll.fullScroll(View.FOCUS_DOWN); }
                        });
                    }
                });
            }
        }).start();
    }

    private TextView bubble(String text, boolean user, boolean assistant) {
        LinearLayout rowWrap = new LinearLayout(this);
        rowWrap.setOrientation(LinearLayout.HORIZONTAL);
        rowWrap.setGravity(user ? Gravity.END : Gravity.START);
        rowWrap.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));

        TextView t = Ui.text(this, user ? (CharSequence) text : Md.render(text), 14,
                user ? Color.WHITE : Ui.C_TEXT);
        t.setTextIsSelectable(true);
        t.setLineSpacing(Ui.dp(this, 2), 1f);
        t.setBackground(user
                ? Ui.gradient(Ui.C_BTN, Ui.C_BTN2, this, 16)
                : Ui.glassSoft(this, 16));
        t.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));

        int max = (int) (getResources().getDisplayMetrics().widthPixels * 0.78f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.width = max;
        rowWrap.addView(t, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        t.setMaxWidth(max);
        msgBox.addView(rowWrap, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        scroll.post(new Runnable() {
            public void run() { scroll.fullScroll(View.FOCUS_DOWN); }
        });
        return t;
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
