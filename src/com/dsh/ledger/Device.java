package com.dsh.ledger;

import android.os.Build;

/** 识别厂商与「岛」能力，决定用哪种实时活动参数 */
public class Device {

    public enum Vendor { XIAOMI, OPPO, VIVO, HONOR, HUAWEI, SAMSUNG, OTHER }

    public static String prop(String key) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method m = c.getMethod("get", String.class);
            Object v = m.invoke(null, key);
            return v == null ? "" : v.toString();
        } catch (Throwable e) {
            return "";
        }
    }

    public static Vendor vendor() {
        String man = (Build.MANUFACTURER == null ? "" : Build.MANUFACTURER).toLowerCase();
        String brand = (Build.BRAND == null ? "" : Build.BRAND).toLowerCase();
        if (man.contains("xiaomi") || brand.contains("xiaomi") || brand.contains("redmi")
                || brand.contains("poco") || !prop("ro.mi.os.version.name").isEmpty()
                || !prop("ro.miui.ui.version.name").isEmpty()) return Vendor.XIAOMI;
        if (man.contains("oppo") || brand.contains("oppo") || brand.contains("oneplus")
                || brand.contains("realme") || !prop("ro.build.version.oplusrom").isEmpty()) return Vendor.OPPO;
        if (man.contains("vivo") || brand.contains("vivo") || brand.contains("iqoo")
                || !prop("ro.vivo.os.version").isEmpty()) return Vendor.VIVO;
        if (man.contains("honor") || brand.contains("honor") || !prop("ro.build.version.magic").isEmpty()) return Vendor.HONOR;
        if (man.contains("huawei") || brand.contains("huawei") || !prop("ro.build.version.emui").isEmpty()) return Vendor.HUAWEI;
        if (man.contains("samsung")) return Vendor.SAMSUNG;
        return Vendor.OTHER;
    }

    public static String vendorName() {
        switch (vendor()) {
            case XIAOMI: return "Xiaomi";
            case OPPO: return "OPPO / OnePlus / realme";
            case VIVO: return "vivo / iQOO";
            case HONOR: return "Honor";
            case HUAWEI: return "Huawei";
            case SAMSUNG: return "Samsung";
            default: return Build.MANUFACTURER == null ? "Android" : Build.MANUFACTURER;
        }
    }

    public static String osName() {
        String os = prop("ro.mi.os.version.name");
        if (!os.isEmpty()) return "HyperOS " + os;
        String miui = prop("ro.miui.ui.version.name");
        if (!miui.isEmpty()) return "MIUI " + miui;
        String oplus = prop("ro.build.version.oplusrom");
        if (!oplus.isEmpty()) return "ColorOS " + oplus;
        String vivo = prop("ro.vivo.os.version");
        if (!vivo.isEmpty()) return "OriginOS " + vivo;
        String magic = prop("ro.build.version.magic");
        if (!magic.isEmpty()) return "MagicOS " + magic;
        String emui = prop("ro.build.version.emui");
        if (!emui.isEmpty()) return emui;
        String rel = prop("ro.build.version.release");
        return "Android " + (rel.isEmpty() ? String.valueOf(Build.VERSION.SDK_INT) : rel);
    }

    /** 该机型上「岛」的官方叫法 */
    public static String islandName() {
        switch (vendor()) {
            case XIAOMI: return "超级岛";
            case OPPO: return "流体云";
            case VIVO: return "原子岛";
            case HONOR: return "灵动胶囊";
            case HUAWEI: return "实况窗";
            case SAMSUNG: return "Now Bar";
            default: return "状态栏实时活动";
        }
    }

    /** 系统接受哪些实时活动字段（普通应用能发的只有小米这一套是公开约定） */
    public static String islandParamKey() {
        switch (vendor()) {
            case XIAOMI: return "miui.focus.param";
            case OPPO: return "oplus.focus.param";
            case VIVO: return "vivo.focus.param";
            default: return "";
        }
    }

    public static String describe() {
        return vendorName() + " · " + osName();
    }

    public static String supportLine() {
        String key = islandParamKey();
        if (key.isEmpty()) return islandName() + "：该机型走标准通知（无公开接口）";
        return islandName() + "：已按 " + key + " 适配";
    }
}
