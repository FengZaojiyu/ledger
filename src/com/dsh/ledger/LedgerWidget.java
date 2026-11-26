package com.dsh.ledger;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Locale;

/** 桌面小组件：小尺寸显示今日支出，大尺寸显示本月支出 + 预算进度 + 今日 */
public class LedgerWidget extends AppWidgetProvider {

    private static final int SMALL_MAX_WIDTH_DP = 150;

    // 没有 R 类，按名字取资源 id（解析一次缓存）
    private static int sBigLayout, sSmallLayout;
    private static int sBigRoot, sExpense, sBudget, sToday;
    private static int sSmallRoot, sSmallToday;

    private static void ids(Context c) {
        if (sBigLayout != 0) return;
        sBigLayout = c.getResources().getIdentifier("widget_ledger", "layout", c.getPackageName());
        sSmallLayout = c.getResources().getIdentifier("widget_small", "layout", c.getPackageName());
        sBigRoot = c.getResources().getIdentifier("widget_root", "id", c.getPackageName());
        sExpense = c.getResources().getIdentifier("w_expense", "id", c.getPackageName());
        sBudget = c.getResources().getIdentifier("w_budget", "id", c.getPackageName());
        sToday = c.getResources().getIdentifier("w_today", "id", c.getPackageName());
        sSmallRoot = c.getResources().getIdentifier("small_root", "id", c.getPackageName());
        sSmallToday = c.getResources().getIdentifier("s_today", "id", c.getPackageName());
    }

    @Override
    public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, build(c, id));
    }

    /** 用户拉伸组件后切换布局 */
    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager mgr, int id, Bundle newOptions) {
        mgr.updateAppWidget(id, build(c, id));
    }

    public static void refresh(Context c) {
        try {
            ids(c);
            if (sBigLayout == 0) return;
            AppWidgetManager mgr = AppWidgetManager.getInstance(c);
            ComponentName cn = new ComponentName(c, LedgerWidget.class);
            int[] ids = mgr.getAppWidgetIds(cn);
            if (ids == null || ids.length == 0) return;
            for (int id : ids) mgr.updateAppWidget(id, build(c, id));
        } catch (Exception ignored) { }
    }

    private static boolean isSmall(Context c, int id) {
        try {
            Bundle o = AppWidgetManager.getInstance(c).getAppWidgetOptions(id);
            int w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            return w > 0 && w <= SMALL_MAX_WIDTH_DP;
        } catch (Exception e) {
            return false;
        }
    }

    private static RemoteViews build(Context c, int widgetId) {
        ids(c);
        boolean small = isSmall(c, widgetId);

        double today = 0, exp = 0, budget = 0;
        try {
            Db db = Db.get(c);
            Calendar cal = Calendar.getInstance();
            long[] month = Ui.monthRange(cal);

            Calendar d0 = (Calendar) cal.clone();
            d0.set(Calendar.HOUR_OF_DAY, 0);
            d0.set(Calendar.MINUTE, 0);
            d0.set(Calendar.SECOND, 0);
            d0.set(Calendar.MILLISECOND, 0);
            long dayStart = d0.getTimeInMillis();

            today = db.sum(dayStart, dayStart + 86400000L, Db.EXPENSE);
            exp = db.sum(month[0], month[1], Db.EXPENSE);
            budget = db.budget();
        } catch (Exception ignored) { }

        RemoteViews rv;
        if (small) {
            rv = new RemoteViews(c.getPackageName(), sSmallLayout);
            rv.setTextViewText(sSmallToday, "¥" + shortNum(today));
            rv.setOnClickPendingIntent(sSmallRoot, openIntent(c));
        } else {
            rv = new RemoteViews(c.getPackageName(), sBigLayout);
            rv.setTextViewText(sExpense, "¥" + fmt(exp));
            if (budget > 0) {
                int pct = (int) Math.min(100, Math.round(exp / budget * 100));
                rv.setTextViewText(sBudget, "本月 · 预算已用 " + pct + "%");
            } else {
                rv.setTextViewText(sBudget, "本月支出");
            }
            rv.setTextViewText(sToday, "今日 ¥" + fmt(today));
            rv.setOnClickPendingIntent(sBigRoot, openIntent(c));
        }
        return rv;
    }

    private static PendingIntent openIntent(Context c) {
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = android.os.Build.VERSION.SDK_INT >= 23
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
        return PendingIntent.getActivity(c, 0, open, flags);
    }

    private static String fmt(double v) {
        return String.format(Locale.CHINA, "%,.2f", v);
    }

    /** 1x1 空间小，整数就够 */
    private static String shortNum(double v) {
        if (v >= 1000) return String.format(Locale.CHINA, "%,.0f", v);
        if (v == Math.floor(v)) return String.format(Locale.CHINA, "%.0f", v);
        return String.format(Locale.CHINA, "%.1f", v);
    }
}
