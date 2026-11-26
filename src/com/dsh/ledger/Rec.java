package com.dsh.ledger;

import android.database.Cursor;

public class Rec {
    public long id;
    public double amount;
    public int type;
    public String cat;
    public String note;
    public long ts;

    public static Rec from(Cursor c) {
        Rec r = new Rec();
        r.id = c.getLong(c.getColumnIndexOrThrow("_id"));
        r.amount = c.getDouble(c.getColumnIndexOrThrow("amount"));
        r.type = c.getInt(c.getColumnIndexOrThrow("type"));
        r.cat = c.getString(c.getColumnIndexOrThrow("cat"));
        r.note = c.getString(c.getColumnIndexOrThrow("note"));
        r.ts = c.getLong(c.getColumnIndexOrThrow("ts"));
        return r;
    }
}
