package com.dsh.ledger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/** 小米超级岛（焦点通知）：把本月预算/今日支出挂在岛和状态栏上 */
public class FocusNotify {

    private static final int ID = 2001;
    private static final String CHANNEL = "ledger-live";

    public static boolean enabled(Context c) {
        return c.getSharedPreferences("ledger", Context.MODE_PRIVATE)
                .getBoolean("focus_notify", true);
    }

    public static void setEnabled(Context c, boolean on) {
        c.getSharedPreferences("ledger", Context.MODE_PRIVATE)
                .edit().putBoolean("focus_notify", on).apply();
        if (on) update(c); else cancel(c);
    }

    public static boolean canPost(Context c) {
        if (Build.VERSION.SDK_INT < 23) return true;
        return c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void cancel(Context c) {
        try {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(ID);
        } catch (Exception ignored) { }
    }

    public static void update(Context c) {
        LedgerWidget.refresh(c);
        if (!enabled(c) || !canPost(c)) return;
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
            double today = db.sum(dayStart, dayStart + 86400000L, Db.EXPENSE);

            double exp = db.sum(month[0], month[1], Db.EXPENSE);
            double inc = db.sum(month[0], month[1], Db.INCOME);
            double budget = db.budget();

            String big, small, content;
            int pct = 0;
            if (budget > 0) {
                pct = (int) Math.min(100, Math.round(exp / budget * 100));
                big = "预算 " + pct + "%";
                small = pct + "%";
                content = "本月已花 ¥" + Ui.money(exp) + " / 预算 ¥" + Ui.money(budget)
                        + "　结余 ¥" + Ui.money(inc - exp);
            } else {
                big = "今日 ¥" + Ui.money(today);
                small = "¥" + Ui.money(today);
                content = "本月支出 ¥" + Ui.money(exp) + "　结余 ¥" + Ui.money(inc - exp);
            }

            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "实时活动",
                        NotificationManager.IMPORTANCE_DEFAULT);
                ch.setDescription("超级岛 / 状态栏上的预算进度");
                ch.setShowBadge(false);
                ch.enableVibration(false);
                ch.setSound(null, null);
                nm.createNotificationChannel(ch);
            }

            Intent open = new Intent(c, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int piFlags = Build.VERSION.SDK_INT >= 23
                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getActivity(c, 0, open, piFlags);

            int icon = c.getResources().getIdentifier("ic_ai", "drawable", c.getPackageName());
            if (icon == 0) icon = android.R.drawable.ic_dialog_info;

            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= 26) {
                b = new Notification.Builder(c, CHANNEL);
            } else {
                b = new Notification.Builder(c);
            }
            b.setSmallIcon(icon)
                    .setContentTitle(big)
                    .setContentText(content)
                    .setStyle(new Notification.BigTextStyle().bigText(content))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .setCategory(Notification.CATEGORY_PROGRESS)
                    .setColor(0xFF5B7CFF)
                    .setContentIntent(pi);
            if (budget > 0) b.setProgress(100, pct, false);

            // 让系统按「实时活动 / 提升型常驻通知」处理（API 31+ 的隐藏方法）
            try {
                java.lang.reflect.Method m = Notification.Builder.class
                        .getMethod("setRequestPromotedOngoing", boolean.class);
                m.invoke(b, true);
            } catch (Throwable ignored) { }

            Notification n = b.build();
            n.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_ONLY_ALERT_ONCE;

            int promoted = promotedFlag();
            if (promoted != 0) n.flags |= promoted;

            n.extras.putBoolean("android.requestPromotedOngoing", true);

            // 按厂商塞对应的实时活动参数；没有公开接口的机型只发标准通知
            String islandKey = Device.islandParamKey();
            if (islandKey.length() > 0) {
                n.extras.putString(islandKey, focusJson(big, content, small));
                if (Device.vendor() == Device.Vendor.XIAOMI) {
                    Bundle pics = new Bundle();
                    Bitmap bm = drawableBitmap(c, icon);
                    if (bm != null) pics.putParcelable("miui.focus.pic_icon", bm);
                    n.extras.putBundle("miui.focus.pics", pics);
                }
            }

            nm.notify(ID, n);
        } catch (Exception ignored) { }
    }

    /** Notification 的 PROMOTED_ONGOING 是隐藏常量，反射拿不到时用已知位值兜底 */
    private static int promotedFlag() {
        try {
            for (java.lang.reflect.Field f : Notification.class.getFields()) {
                if (f.getName().contains("PROMOTED")) {
                    int v = f.getInt(null);
                    if (v != 0) return v;
                }
            }
        } catch (Throwable ignored) { }
        return 0;
    }

    private static String focusJson(String title, String content, String islandTitle) {
        try {
            JSONObject pic = new JSONObject().put("type", 1).put("pic", "miui.focus.pic_icon");

            JSONObject textInfo = new JSONObject().put("title", islandTitle);
            JSONObject left = new JSONObject()
                    .put("type", 1)
                    .put("picInfo", pic)
                    .put("textInfo", textInfo);
            JSONObject bigArea = new JSONObject().put("imageTextInfoLeft", left);
            JSONObject smallArea = new JSONObject().put("picInfo", pic);
            JSONObject island = new JSONObject()
                    .put("islandProperty", 1)
                    .put("bigIslandArea", bigArea)
                    .put("smallIslandArea", smallArea);

            JSONObject base = new JSONObject()
                    .put("type", 2)
                    .put("title", title)
                    .put("content", content);

            JSONObject v2 = new JSONObject()
                    .put("protocol", 1)
                    .put("business", "ledger_live")
                    .put("updatable", true)
                    .put("enableFloat", false)
                    .put("ticker", title + " · " + content)
                    .put("aodTitle", "小账本")
                    .put("param_island", island)
                    .put("baseInfo", base);

            return new JSONObject().put("param_v2", v2).toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private static Bitmap drawableBitmap(Context c, int id) {
        try {
            Drawable d = c.getResources().getDrawable(id);
            if (d == null) return null;
            int s = (int) (48 * c.getResources().getDisplayMetrics().density);
            Bitmap bm = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bm);
            d.setBounds(0, 0, s, s);
            d.draw(cv);
            return bm;
        } catch (Exception e) {
            return null;
        }
    }
}
