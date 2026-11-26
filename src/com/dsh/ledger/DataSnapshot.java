package com.dsh.ledger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 把账本数据整理成给模型看的一段文字 */
public class DataSnapshot {

    public static String build(android.content.Context ctx) {
        Db db = Db.get(ctx);
        Calendar cal = Calendar.getInstance();
        long[] r = Ui.monthRange(cal);
        double exp = db.sum(r[0], r[1], Db.EXPENSE);
        double inc = db.sum(r[0], r[1], Db.INCOME);
        double budget = db.budget();
        String month = Ui.monthKey(cal);

        StringBuilder sb = new StringBuilder();
        sb.append("【账本数据 · ").append(month).append("】\n");
        sb.append("本月支出：").append(fmt(exp)).append(" 元\n");
        sb.append("本月收入：").append(fmt(inc)).append(" 元\n");
        sb.append("本月结余：").append(fmt(inc - exp)).append(" 元\n");
        if (budget > 0) {
            sb.append("月度预算：").append(fmt(budget)).append(" 元，已用 ")
              .append(String.format(Locale.CHINA, "%.0f%%", exp / budget * 100)).append("\n");
        } else {
            sb.append("月度预算：未设置\n");
        }

        sb.append("\n分类明细（支出）：\n");
        android.database.Cursor c = db.sumsByCat(r[0], r[1], Db.EXPENSE);
        boolean any = false;
        while (c.moveToNext()) {
            any = true;
            double v = c.getDouble(1);
            sb.append("- ").append(c.getString(0)).append("：").append(fmt(v))
              .append(" 元（").append(exp > 0 ? String.format(Locale.CHINA, "%.0f%%", v / exp * 100) : "0%")
              .append("）\n");
        }
        c.close();
        if (!any) sb.append("- （本月还没有支出记录）\n");

        sb.append("\n分类明细（收入）：\n");
        c = db.sumsByCat(r[0], r[1], Db.INCOME);
        any = false;
        while (c.moveToNext()) {
            any = true;
            sb.append("- ").append(c.getString(0)).append("：").append(fmt(c.getDouble(1))).append(" 元\n");
        }
        c.close();
        if (!any) sb.append("- （本月还没有收入记录）\n");

        List<Rec> list = db.list(r[0], r[1]);
        sb.append("\n最近记录（最多 30 条，按时间倒序）：\n");
        SimpleDateFormat df = new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA);
        int n = 0;
        for (Rec rec : list) {
            if (n++ >= 30) break;
            sb.append("- ").append(df.format(new Date(rec.ts))).append(" ")
              .append(rec.type == Db.EXPENSE ? "支出" : "收入").append(" ")
              .append(rec.cat).append(" ")
              .append(fmt(rec.amount)).append(" 元");
            if (rec.note != null && rec.note.length() > 0) sb.append("，备注：").append(rec.note);
            sb.append("\n");
        }
        if (list.isEmpty()) sb.append("- （本月还没有记录）\n");

        // 攒钱目标
        java.util.List<Goal> goals = db.goals();
        sb.append("\n攒钱目标：\n");
        if (goals.isEmpty()) {
            sb.append("- （还没设目标）\n");
        } else {
            double avg = Goal.avgMonthlySurplus(db);
            for (Goal g : goals) {
                sb.append("- ").append(g.name).append("：目标 ").append(fmt(g.target))
                  .append(" 元，已攒 ").append(fmt(g.saved)).append(" 元（")
                  .append(String.format(Locale.CHINA, "%.0f%%", g.percent())).append("），");
                int m = Goal.monthsNeeded(g.remain(), avg);
                if (m < 0) sb.append("按近 3 个月平均结余（" + fmt(avg) + " 元/月）无法达成");
                else if (m == 0) sb.append("已达成");
                else sb.append("按近 3 个月平均结余（").append(fmt(avg)).append(" 元/月）还需约 ")
                        .append(m).append(" 个月");
                sb.append("\n");
            }
        }

        // 近 3 个月对比
        List<double[]> months = db.monthly(3);
        sb.append("\n近 3 个月：\n");
        Calendar m = Calendar.getInstance();
        for (int i = months.size() - 1; i >= 0; i--) {
            Calendar t = Calendar.getInstance();
            t.add(Calendar.MONTH, -(months.size() - 1 - i));
            sb.append("- ").append(Ui.monthKey(t)).append("：支出 ")
              .append(fmt(months.get(i)[1])).append("，收入 ")
              .append(fmt(months.get(i)[2])).append("\n");
        }
        return sb.toString();
    }

    private static String fmt(double v) {
        return String.format(Locale.CHINA, "%.2f", v);
    }
}
