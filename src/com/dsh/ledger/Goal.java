package com.dsh.ledger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 攒钱目标模型 + 基于账本结余的估算 */
public class Goal {
    public long id;
    public String name;
    public double target;
    public double saved;
    public long createdTs;
    public long deadlineTs;
    public String note;

    public double remain() {
        return Math.max(0, target - saved);
    }

    public double percent() {
        return target <= 0 ? 0 : Math.min(100, saved / target * 100);
    }

    /** 近 3 个月平均每月结余（收入 − 支出） */
    public static double avgMonthlySurplus(Db db) {
        List<double[]> months = db.monthly(3);
        if (months.isEmpty()) return 0;
        double sum = 0;
        for (double[] m : months) sum += (m[2] - m[1]);
        return sum / months.size();
    }

    /** 按当前结余速度需要多少个月；返 -1 表示按现在攒不下来 */
    public static int monthsNeeded(double remain, double avg) {
        if (remain <= 0) return 0;
        if (avg <= 0.01) return -1;
        return (int) Math.ceil(remain / avg);
    }

    public static String etaText(Goal g, double avg) {
        double remain = g.remain();
        if (remain <= 0) return "已达成 🎉";
        int months = monthsNeeded(remain, avg);
        if (months < 0) return "按当前结余无法达成";
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, months);
        String date = new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(c.getTime());
        return "还要约 " + months + " 个月 · " + date + " 前后";
    }

    public static String progressText(Goal g) {
        return String.format(Locale.CHINA, "%.0f / %.0f 元（%.0f%%）",
                g.saved, g.target, g.percent());
    }

    public static String deadlineText(Goal g) {
        if (g.deadlineTs <= 0) return "";
        return "目标期限 " + new SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
                .format(new Date(g.deadlineTs));
    }
}
