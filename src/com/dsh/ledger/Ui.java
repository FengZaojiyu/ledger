package com.dsh.ledger;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Ui {
    // 这些颜色会随主题切换，所以是可变字段
    public static int C_BG, C_CARD, C_GLASS, C_GLASS2, C_GLASS_SOFT, C_GLASS_SOFT2, C_STROKE;
    public static int C_TEXT, C_SUB, C_SUB2, C_ACCENT, C_ACCENT2, C_BTN, C_BTN2, C_RED, C_GREEN;
    public static int C_TRACK, C_ICON_BG, C_CHIP, C_CHIP_ON, C_TABS_BG, C_SOFT_BTN;
    public static int C_DIALOG, C_DIV, C_HINT;
    public static boolean DARK = false;

    static { applyPalette(false); }

    public static void applyPalette(boolean dark) {
        DARK = dark;
        if (dark) {
            C_BG = 0xFF0F1218;
            C_CARD = 0xFF1A1E27;
            C_GLASS = 0x17FFFFFF;
            C_GLASS2 = 0x0FFFFFFF;
            C_GLASS_SOFT = 0x10FFFFFF;
            C_GLASS_SOFT2 = 0x0AFFFFFF;
            C_STROKE = 0x26FFFFFF;
            C_TEXT = 0xFFEDEFF5;
            C_SUB = 0xFF99A0B0;
            C_SUB2 = 0xFFB6BCC9;
            C_ACCENT = 0xFF7C9BFF;
            C_ACCENT2 = 0xFFA8BEFF;
            C_BTN = 0xFF3F63F0;
            C_BTN2 = 0xFF6E8DFF;
            C_RED = 0xFFFF7075;
            C_GREEN = 0xFF3DD68C;
            C_TRACK = 0x24FFFFFF;
            C_ICON_BG = 0x1AFFFFFF;
            C_CHIP = 0x1AFFFFFF;
            C_CHIP_ON = 0x335B7CFF;
            C_TABS_BG = 0x14FFFFFF;
            C_SOFT_BTN = 0x1AFFFFFF;
            C_DIALOG = 0xFF1C2029;
            C_DIV = 0x1FFFFFFF;
            C_HINT = 0xFF6C7382;
        } else {
            C_BG = 0xFFEEF2FF;
            C_CARD = 0xFFFFFFFF;
            C_GLASS = 0xBFFFFFFF;
            C_GLASS2 = 0x9CFFFFFF;
            C_GLASS_SOFT = 0x9AFFFFFF;
            C_GLASS_SOFT2 = 0x80FFFFFF;
            C_STROKE = 0x70FFFFFF;
            C_TEXT = 0xFF16181D;
            C_SUB = 0xFF7A8090;
            C_SUB2 = 0xFF6B7280;
            C_ACCENT = 0xFF4C6FFF;
            C_ACCENT2 = 0xFF8AA4FF;
            C_BTN = 0xFF4C6FFF;
            C_BTN2 = 0xFF8AA4FF;
            C_RED = 0xFFE5484D;
            C_GREEN = 0xFF12A150;
            C_TRACK = 0xFFEDEFF3;
            C_ICON_BG = 0xFFF3F6FF;
            C_CHIP = 0xFFF2F3F6;
            C_CHIP_ON = 0xFFE3EDFF;
            C_TABS_BG = 0xFFE7E9EE;
            C_SOFT_BTN = 0x66FFFFFF;
            C_DIALOG = 0xFFFFFFFF;
            C_DIV = 0x14000000;
            C_HINT = 0xFFB9BEC7;
        }
    }

    /** light / dark / system */
    public static String themeMode(Context c) {
        return c.getSharedPreferences("ledger", Context.MODE_PRIVATE)
                .getString("theme", "system");
    }

    public static void setThemeMode(Context c, String mode) {
        c.getSharedPreferences("ledger", Context.MODE_PRIVATE)
                .edit().putString("theme", mode).apply();
    }

    public static boolean isSystemDark(Context c) {
        return (c.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public static void applyTheme(android.app.Activity a) {
        String m = themeMode(a);
        boolean dark;
        if ("dark".equals(m)) dark = true;
        else if ("light".equals(m)) dark = false;
        else dark = isSystemDark(a);
        applyPalette(dark);
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** 柔光背景：三色渐变 + 三处径向光斑 */
    public static Drawable aura(Context c) {
        GradientDrawable base = DARK
                ? new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFF101521, 0xFF171225, 0xFF0E1620, 0xFF12151C})
                : new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                        new int[]{0xFFEAF0FF, 0xFFF7F1FF, 0xFFEBF7FF, 0xFFF2F5FB});
        GradientDrawable p1 = DARK ? radial(0x3DFF7AB6, 0x00FF7AB6) : radial(0x66FFB6D9, 0x00FFB6D9);
        GradientDrawable p2 = DARK ? radial(0x405B8CFF, 0x005B8CFF) : radial(0x599BC4FF, 0x009BC4FF);
        GradientDrawable p3 = DARK ? radial(0x3DA78BFA, 0x00A78BFA) : radial(0x4DD6B8FF, 0x00D6B8FF);
        GradientDrawable scrim = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x33000000, 0x12000000, 0x00000000});
        LayerDrawable ld = new LayerDrawable(new Drawable[]{base, p1, p2, p3, scrim});
        int w = c.getResources().getDisplayMetrics().widthPixels;
        int h = c.getResources().getDisplayMetrics().heightPixels;
        ld.setLayerInset(1, -w / 3, -h / 10, w / 3, h / 2);
        ld.setLayerInset(2, w / 3, h / 2, -w / 4, -h / 12);
        ld.setLayerInset(3, w / 5, -h / 12, -w / 6, h / 3);
        int sb = 0;
        int resId = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) sb = c.getResources().getDimensionPixelSize(resId);
        ld.setLayerInset(4, 0, 0, 0, Math.max(0, h - sb - dp(c, 26)));
        return ld;
    }

    private static GradientDrawable radial(int inner, int outer) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientRadius(600f);
        g.setGradientCenter(0.5f, 0.5f);
        g.setColors(new int[]{inner, outer});
        return g;
    }

    /** 玻璃卡片：半透明白 + 顶部更亮 + 细描边 */
    public static Drawable glass(Context c, float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{C_GLASS, C_GLASS2});
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(Math.max(1, dp(c, 1)), C_STROKE);
        return g;
    }

    /** 更轻的玻璃层，用于列表行、搜索框 */
    public static Drawable glassSoft(Context c, float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{C_GLASS_SOFT, C_GLASS_SOFT2});
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(Math.max(1, dp(c, 1)), 0x5CFFFFFF);
        return g;
    }

    /** 页面统一处理：状态栏透明化 + 柔光背景 */
    public static void polish(Activity a, View root) {
        try {
            Window w = a.getWindow();
            if (Build.VERSION.SDK_INT >= 21) {
                w.setStatusBarColor(Color.TRANSPARENT);
                // 深色主题下导航条也必须是深色，否则底部会留一条白边
                w.setNavigationBarColor(DARK ? 0xFF0F1218 : 0xFFEDF1FA);
                int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                w.getDecorView().setSystemUiVisibility(flags);
                if (Build.VERSION.SDK_INT >= 30) {
                    android.view.WindowInsetsController ic = w.getInsetsController();
                    if (ic != null) {
                        int mask = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                        // 深色主题：图标保持白色（不设 light 标志）
                        int appearance = DARK ? 0 : mask;
                        ic.setSystemBarsAppearance(appearance, mask);
                    }
                } else if (Build.VERSION.SDK_INT >= 23) {
                    w.getDecorView().setSystemUiVisibility(
                            DARK ? flags : (flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
                }
            }
        } catch (Exception ignored) { }
        int top = 0;
        int resId = a.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) top = a.getResources().getDimensionPixelSize(resId);
        if (top <= 0) top = dp(a, 24);
        root.setPadding(root.getPaddingLeft(), root.getPaddingTop() + top,
                root.getPaddingRight(), root.getPaddingBottom());
        root.setBackground(aura(a));
    }

    /** 点击涟漪，套在玻璃底上 */
    public static Drawable ripple(Context c, Drawable content, int rippleColor) {
        return new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(rippleColor), content, null);
    }

    /** 顶栏小胶囊按钮 */
    public static TextView pill(Context c, String s) {
        TextView t = text(c, s, 13, C_ACCENT);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 13), dp(c, 7), dp(c, 13), dp(c, 7));
        t.setBackground(ripple(c, glassSoft(c, 14), 0x335B7CFF));
        return t;
    }

    /**
     * 键盘弹出时把根布局顶上来。
     * Android 15+ 强制 edge-to-edge 后 windowSoftInputMode=adjustResize 不再生效，
     * 需要自己读 IME insets。
     */
    public static void imeAware(final View root) {
        final int[] base = new int[]{-1, -1, -1, -1};
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            public android.view.WindowInsets onApplyWindowInsets(View v, android.view.WindowInsets insets) {
                if (base[0] < 0) {
                    base[0] = v.getPaddingLeft();
                    base[1] = v.getPaddingTop();
                    base[2] = v.getPaddingRight();
                    base[3] = v.getPaddingBottom();
                }
                int ime = 0;
                if (Build.VERSION.SDK_INT >= 30) {
                    ime = insets.getInsets(android.view.WindowInsets.Type.ime()).bottom;
                } else {
                    ime = insets.getSystemWindowInsetBottom();
                }
                v.setPadding(base[0], base[1], base[2], base[3] + Math.max(0, ime));
                return insets;
            }
        });
    }

    public static GradientDrawable roundRect(int color, Context c, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable gradient(int from, int to, Context c, float radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{from, to});
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static TextView text(Context c, CharSequence s, float sizeSp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTextColor(color);
        return t;
    }

    public static TextView text(Context c, String s, float sizeSp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTextColor(color);
        return t;
    }

    public static TextView button(Context c, String s, int bg, int fg, float sizeSp) {
        TextView t = text(c, s, sizeSp, fg);
        t.setGravity(Gravity.CENTER);
        if (bg == C_ACCENT) {
            t.setBackground(gradient(C_BTN, C_BTN2, c, 14));
        } else {
            t.setBackground(roundRect(bg, c, 12));
        }
        t.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        return t;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static String money(double v) {
        DecimalFormat f = new DecimalFormat("#,##0.00");
        return f.format(v);
    }

    public static String monthKey(Calendar c) {
        return new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(c.getTime());
    }

    public static String dayKey(long ts) {
        return new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date(ts));
    }

    public static String timeKey(long ts) {
        return new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(ts));
    }

    public static long[] monthRange(Calendar c) {
        Calendar a = (Calendar) c.clone();
        a.set(Calendar.DAY_OF_MONTH, 1);
        a.set(Calendar.HOUR_OF_DAY, 0);
        a.set(Calendar.MINUTE, 0);
        a.set(Calendar.SECOND, 0);
        a.set(Calendar.MILLISECOND, 0);
        long from = a.getTimeInMillis();
        a.add(Calendar.MONTH, 1);
        return new long[]{from, a.getTimeInMillis()};
    }

    /** 优先用分类表里手动指定的图标 */
    public static String emoji(String cat, java.util.Map<String, String> map) {
        if (map != null && cat != null) {
            String e = map.get(cat);
            if (e != null && e.length() > 0) return e;
        }
        return emoji(cat);
    }

    public static String emoji(String cat) {
        if (cat == null) return "📦";
        if (cat.equals("餐饮")) return "🍜";
        if (cat.equals("交通")) return "🚌";
        if (cat.equals("购物")) return "🛍️";
        if (cat.equals("娱乐")) return "🎮";
        if (cat.equals("居住")) return "🏠";
        if (cat.equals("医疗")) return "💊";
        if (cat.equals("工资")) return "💰";
        if (cat.equals("红包")) return "🧧";
        if (cat.equals("兼职")) return "💼";
        if (cat.equals("其他收入")) return "➕";
        return "📦";
    }
}
