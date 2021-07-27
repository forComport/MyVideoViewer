package com.example.myvideoviewer.jav247;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class Jav247DbHelper extends SQLiteOpenHelper {
    private static final String TAG = "Jav247DbHelperTAG";
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "jav247.db";
    private SQLiteDatabase writableDb;
    private SQLiteDatabase readableDb;

    public static class Jav247Table implements BaseColumns {
        public static final String TABLE_NAME = "jav247";
        public static final String TITLE = "title";
        public static final String THUMBNAIL = "thumbnail";
        public static final String CREATED = "created";
        public static final String PAGE_URL = "page_url";
        public static final String STATE = "state";
    }
    public static final String SQL_CREATE_JAV247 = "" +
            "CREATE TABLE " + Jav247Table.TABLE_NAME + " (" +
            Jav247Table._ID + " INTEGER PRIMARY KEY, " +
            Jav247Table.TITLE + " TEXT, " +
            Jav247Table.THUMBNAIL + " TEXT, " +
            Jav247Table.CREATED + " TEXT, " +
            Jav247Table.PAGE_URL + " TEXT, " +
            Jav247Table.STATE + " TEXT)";

    public Jav247DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_JAV247);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void insert(String title, String thumbnail, String created, String page_url) {
        if (writableDb == null) {
            writableDb = getWritableDatabase();
        }
        ContentValues values = new ContentValues();
        values.put(Jav247Table.TITLE, title);
        values.put(Jav247Table.THUMBNAIL, thumbnail);
        values.put(Jav247Table.CREATED, created);
        values.put(Jav247Table.PAGE_URL, page_url);
        values.put(Jav247Table.STATE, "신규");
        writableDb.insert(Jav247Table.TABLE_NAME, null, values);
    }

    public Cursor read(boolean reverse){
        if (readableDb == null) {
            readableDb = getReadableDatabase();
        }
        String[] projection = {
                Jav247Table._ID,
                Jav247Table.TITLE,
                Jav247Table.THUMBNAIL,
                Jav247Table.CREATED,
                Jav247Table.PAGE_URL,
                Jav247Table.STATE
        };
        return readableDb.query(
                Jav247Table.TABLE_NAME,
                projection,
                "state!=?",
                new String[]{"숨김"},
                null,
                null,
                reverse ? Jav247Table._ID + " DESC" : null
        );
    }

    public void update(int id, String state) {
        if (writableDb == null) {
            writableDb = getWritableDatabase();
        }
        ContentValues values = new ContentValues();
        values.put("state", state);
        writableDb.update(Jav247Table.TABLE_NAME, values, Jav247Table._ID+ "=?", new String[]{id+""});
    }

    public void update(String title, String state) {
        if (writableDb == null) {
            writableDb = getWritableDatabase();
        }
        ContentValues values = new ContentValues();
        values.put("state", state);
        writableDb.update(Jav247Table.TABLE_NAME, values, Jav247Table.TITLE+ "=?", new String[]{title});
    }

}
