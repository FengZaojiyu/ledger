package com.dsh.ledger;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.List;

/** 极简 Markdown：粗体 / 项目符号 / 标题 / 行内代码，够聊天用 */
public class Md {

    public static CharSequence render(String src) {
        if (src == null) return "";
        StringBuilder plain = new StringBuilder();
        List<int[]> bold = new ArrayList<int[]>();
        List<int[]> mono = new ArrayList<int[]>();

        String[] lines = src.replace("\r", "").split("\n");
        boolean inCode = false;
        for (String raw : lines) {
            String line = raw;
            if (line.trim().startsWith("```")) {
                inCode = !inCode;
                continue;
            }
            if (inCode) {
                int s = plain.length();
                plain.append(line);
                mono.add(new int[]{s, plain.length()});
                plain.append('\n');
                continue;
            }
            if (line.startsWith("#")) {
                line = line.replaceAll("^#+\\s*", "");
            }
            if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ")) {
                line = "•  " + line.substring(2);
            }
            // 行内代码：去掉反引号，保留文字
            line = line.replace("`", "");
            int idx = 0;
            while (true) {
                int a = line.indexOf("**", idx);
                if (a < 0) { plain.append(line.substring(idx)); break; }
                int b = line.indexOf("**", a + 2);
                if (b < 0) { plain.append(line.substring(idx)); break; }
                plain.append(line, idx, a);
                int s = plain.length();
                plain.append(line, a + 2, b);
                bold.add(new int[]{s, plain.length()});
                idx = b + 2;
            }
            // 行内代码
            plain.append('\n');
        }

        SpannableStringBuilder sb = new SpannableStringBuilder(plain.toString());
        for (int[] r : bold) {
            if (r[1] > r[0]) {
                sb.setSpan(new StyleSpan(Typeface.BOLD), r[0], r[1],
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new android.text.style.ForegroundColorSpan(
                                Ui.DARK ? 0xFF9DB6FF : 0xFF2442C8), r[0], r[1],
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        for (int[] r : mono) {
            if (r[1] > r[0]) sb.setSpan(new TypefaceSpan("monospace"), r[0], r[1],
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        // 去掉末尾多余空行（直接改原对象，避免 subSequence 丢 span）
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb;
    }
}
