package com.dsh.ledger;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Db extends SQLiteOpenHelper {
    public static final int EXPENSE = 0;
    public static final int INCOME = 1;

    private static final String[] DEF_EXP = {"餐饮", "交通", "购物", "娱乐", "居住", "医疗", "其他"};
    private static final String[] DEF_INC = {"工资", "红包", "兼职", "其他收入"};
    private static final String[] DEF_EMOJI = {"🍜", "🚌", "🛍️", "🎮", "🏠", "💊", "📦",
            "💰", "🧧", "💼", "➕"};

    private static Db sInstance;

    public static synchronized Db get(Context c) {
        if (sInstance == null) sInstance = new Db(c.getApplicationContext());
        return sInstance;
    }

    private final Context ctx;

    private Db(Context c) {
        super(c, "ledger.db", null, 5);
        ctx = c;
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE rec("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "amount REAL NOT NULL,"
                + "type INTEGER NOT NULL,"
                + "cat TEXT NOT NULL,"
                + "note TEXT,"
                + "ts INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_ts ON rec(ts)");
        db.execSQL("CREATE TABLE cat("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "type INTEGER NOT NULL,"
                + "emoji TEXT)");
        db.execSQL("CREATE TABLE goal("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL,"
                + "target REAL NOT NULL,"
                + "saved REAL NOT NULL DEFAULT 0,"
                + "created_ts INTEGER,"
                + "deadline_ts INTEGER,"
                + "note TEXT)");
        db.execSQL("CREATE TABLE trip("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "title TEXT NOT NULL,"
                + "start_ts INTEGER,"
                + "note TEXT)");
        db.execSQL("CREATE TABLE trip_item("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "trip_id INTEGER NOT NULL,"
                + "kind INTEGER NOT NULL,"
                + "title TEXT NOT NULL,"
                + "detail TEXT,"
                + "place TEXT,"
                + "lat REAL,"
                + "lng REAL,"
                + "start_ts INTEGER,"
                + "seq INTEGER)");
        seed(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS cat("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL,"
                    + "type INTEGER NOT NULL,"
                    + "emoji TEXT)");
        }
        if (oldV < 3) {
            try { db.execSQL("ALTER TABLE cat ADD COLUMN emoji TEXT"); } catch (Exception ignored) { }
        }
        if (oldV < 5) {
            db.execSQL("CREATE TABLE IF NOT EXISTS goal("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL,"
                    + "target REAL NOT NULL,"
                    + "saved REAL NOT NULL DEFAULT 0,"
                    + "created_ts INTEGER,"
                    + "deadline_ts INTEGER,"
                    + "note TEXT)");
        }
        if (oldV < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS trip("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "title TEXT NOT NULL,"
                    + "start_ts INTEGER,"
                    + "note TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS trip_item("
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "trip_id INTEGER NOT NULL,"
                    + "kind INTEGER NOT NULL,"
                    + "title TEXT NOT NULL,"
                    + "detail TEXT,"
                    + "place TEXT,"
                    + "lat REAL,"
                    + "lng REAL,"
                    + "start_ts INTEGER,"
                    + "seq INTEGER)");
        }
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM cat", null);
        boolean empty = c.moveToFirst() && c.getInt(0) == 0;
        c.close();
        if (empty) seed(db);
        fillMissingEmoji(db);
    }

    private void seed(SQLiteDatabase db) {
        for (int i = 0; i < DEF_EXP.length; i++)
            db.execSQL("INSERT INTO cat(name,type,emoji) VALUES(?,0,?)",
                    new Object[]{DEF_EXP[i], DEF_EMOJI[i]});
        for (int i = 0; i < DEF_INC.length; i++)
            db.execSQL("INSERT INTO cat(name,type,emoji) VALUES(?,1,?)",
                    new Object[]{DEF_INC[i], DEF_EMOJI[DEF_EXP.length + i]});
    }

    /** 老数据（v2 之前）没有 emoji，按内置表回填 */
    private void fillMissingEmoji(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT _id, name FROM cat WHERE emoji IS NULL OR emoji=''", null);
        List<long[]> ids = new ArrayList<long[]>();
        List<String> names = new ArrayList<String>();
        while (c.moveToNext()) {
            ids.add(new long[]{c.getLong(0)});
            names.add(c.getString(1));
        }
        c.close();
        for (int i = 0; i < ids.size(); i++) {
            String e = defaultEmoji(names.get(i));
            ContentValues v = new ContentValues();
            v.put("emoji", e);
            db.update("cat", v, "_id=?", new String[]{String.valueOf(ids.get(i)[0])});
        }
    }

    public static String defaultEmoji(String name) {
        if (name == null) return "📦";
        for (int i = 0; i < DEF_EXP.length; i++) if (DEF_EXP[i].equals(name)) return DEF_EMOJI[i];
        for (int i = 0; i < DEF_INC.length; i++) if (DEF_INC[i].equals(name)) return DEF_EMOJI[DEF_EXP.length + i];
        return "📦";
    }

    // ---------- 记录 ----------

    public long insert(double amount, int type, String cat, String note, long ts) {
        ContentValues v = new ContentValues();
        v.put("amount", amount);
        v.put("type", type);
        v.put("cat", cat);
        v.put("note", note == null ? "" : note);
        v.put("ts", ts);
        return getWritableDatabase().insert("rec", null, v);
    }

    public void update(long id, double amount, int type, String cat, String note, long ts) {
        ContentValues v = new ContentValues();
        v.put("amount", amount);
        v.put("type", type);
        v.put("cat", cat);
        v.put("note", note == null ? "" : note);
        v.put("ts", ts);
        getWritableDatabase().update("rec", v, "_id=?", new String[]{String.valueOf(id)});
    }

    public void delete(long id) {
        getWritableDatabase().delete("rec", "_id=?", new String[]{String.valueOf(id)});
    }

    public Rec find(long id) {
        Cursor c = getReadableDatabase().query("rec", null, "_id=?",
                new String[]{String.valueOf(id)}, null, null, null);
        Rec r = null;
        if (c.moveToFirst()) r = Rec.from(c);
        c.close();
        return r;
    }

    public Cursor between(long from, long to) {
        return between(from, to, null);
    }

    public Cursor between(long from, long to, String kw) {
        String sel = "ts>=? AND ts<?";
        List<String> args = new ArrayList<String>();
        args.add(String.valueOf(from));
        args.add(String.valueOf(to));
        if (kw != null && kw.length() > 0) {
            sel += " AND (note LIKE ? OR cat LIKE ?)";
            args.add("%" + kw + "%");
            args.add("%" + kw + "%");
        }
        return getReadableDatabase().query("rec", null, sel,
                args.toArray(new String[0]), null, null, "ts DESC, _id DESC");
    }

    public List<Rec> list(long from, long to) {
        Cursor c = between(from, to);
        List<Rec> out = new ArrayList<Rec>();
        while (c.moveToNext()) out.add(Rec.from(c));
        c.close();
        return out;
    }

    public double sum(long from, long to, int type) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT SUM(amount) FROM rec WHERE ts>=? AND ts<? AND type=?",
                new String[]{String.valueOf(from), String.valueOf(to), String.valueOf(type)});
        double s = 0;
        if (c.moveToFirst()) s = c.getDouble(0);
        c.close();
        return s;
    }

    public Cursor sumsByCat(long from, long to, int type) {
        return getReadableDatabase().rawQuery(
                "SELECT cat, SUM(amount) AS s FROM rec WHERE ts>=? AND ts<? AND type=? GROUP BY cat ORDER BY s DESC",
                new String[]{String.valueOf(from), String.valueOf(to), String.valueOf(type)});
    }

    public List<double[]> monthly(int months) {
        List<double[]> out = new ArrayList<double[]>();
        for (int i = months - 1; i >= 0; i--) {
            java.util.Calendar m = java.util.Calendar.getInstance();
            m.add(java.util.Calendar.MONTH, -i);
            long[] r = Ui.monthRange(m);
            out.add(new double[]{r[0], sum(r[0], r[1], EXPENSE), sum(r[0], r[1], INCOME)});
        }
        return out;
    }

    // ---------- 分类 ----------

    public List<String> categories(int type) {
        List<String> out = new ArrayList<String>();
        Cursor c = getReadableDatabase().query("cat", new String[]{"name"},
                "type=?", new String[]{String.valueOf(type)}, null, null, "_id ASC");
        while (c.moveToNext()) out.add(c.getString(0));
        c.close();
        return out;
    }

    /** [{name, emoji}, ...] */
    public List<String[]> categoryRows(int type) {
        List<String[]> out = new ArrayList<String[]>();
        Cursor c = getReadableDatabase().query("cat", new String[]{"name", "emoji"},
                "type=?", new String[]{String.valueOf(type)}, null, null, "_id ASC");
        while (c.moveToNext()) {
            String e = c.getString(1);
            out.add(new String[]{c.getString(0), e == null || e.length() == 0 ? "📦" : e});
        }
        c.close();
        return out;
    }

    public HashMap<String, String> emojiMap() {
        HashMap<String, String> m = new HashMap<String, String>();
        Cursor c = getReadableDatabase().query("cat", new String[]{"name", "emoji"},
                null, null, null, null, null);
        while (c.moveToNext()) {
            String e = c.getString(1);
            m.put(c.getString(0), e == null || e.length() == 0 ? "📦" : e);
        }
        c.close();
        return m;
    }

    public long addCategory(String name, int type, String emoji) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("type", type);
        v.put("emoji", emoji == null || emoji.length() == 0 ? "📦" : emoji);
        return getWritableDatabase().insert("cat", null, v);
    }

    public void setEmoji(String name, int type, String emoji) {
        ContentValues v = new ContentValues();
        v.put("emoji", emoji);
        getWritableDatabase().update("cat", v, "name=? AND type=?",
                new String[]{name, String.valueOf(type)});
    }

    public void renameCategory(String oldName, int type, String newName) {
        ContentValues v = new ContentValues();
        v.put("name", newName);
        getWritableDatabase().update("cat", v, "name=? AND type=?",
                new String[]{oldName, String.valueOf(type)});
        ContentValues r = new ContentValues();
        r.put("cat", newName);
        getWritableDatabase().update("rec", r, "cat=? AND type=?",
                new String[]{oldName, String.valueOf(type)});
    }

    public void deleteCategory(long id, String name, int type) {
        getWritableDatabase().delete("cat", "name=? AND type=?",
                new String[]{name, String.valueOf(type)});
        String other = type == EXPENSE ? "其他" : "其他收入";
        ContentValues v = new ContentValues();
        v.put("cat", other);
        getWritableDatabase().update("rec", v, "cat=? AND type=?",
                new String[]{name, String.valueOf(type)});
    }

    // ---------- 攒钱目标 ----------

    public java.util.List<Goal> goals() {
        java.util.List<Goal> out = new java.util.ArrayList<Goal>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id, name, target, saved, created_ts, deadline_ts, note FROM goal"
                        + " ORDER BY created_ts DESC, _id DESC", null);
        while (c.moveToNext()) {
            Goal g = new Goal();
            g.id = c.getLong(0);
            g.name = c.getString(1);
            g.target = c.getDouble(2);
            g.saved = c.getDouble(3);
            g.createdTs = c.getLong(4);
            g.deadlineTs = c.getLong(5);
            g.note = c.getString(6);
            out.add(g);
        }
        c.close();
        return out;
    }

    public long addGoal(Goal g) {
        ContentValues v = new ContentValues();
        v.put("name", g.name);
        v.put("target", g.target);
        v.put("saved", g.saved);
        v.put("created_ts", g.createdTs);
        v.put("deadline_ts", g.deadlineTs);
        v.put("note", g.note == null ? "" : g.note);
        return getWritableDatabase().insert("goal", null, v);
    }

    public void addSaved(long id, double delta) {
        getWritableDatabase().execSQL(
                "UPDATE goal SET saved = MAX(0, saved + ?) WHERE _id=?", new Object[]{delta, id});
    }

    public void deleteGoal(long id) {
        getWritableDatabase().delete("goal", "_id=?", new String[]{String.valueOf(id)});
    }

    // ---------- 预算 ----------

    private SharedPreferences prefs() {
        return ctx.getSharedPreferences("ledger", Context.MODE_PRIVATE);
    }

    public double budget() {
        return Double.longBitsToDouble(prefs().getLong("budget_bits", Double.doubleToLongBits(0)));
    }

    public void setBudget(double v) {
        prefs().edit().putLong("budget_bits", Double.doubleToLongBits(v)).apply();
    }
}
