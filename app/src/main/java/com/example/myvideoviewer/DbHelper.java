package com.example.myvideoviewer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class DbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "myservice.db";
    private SQLiteDatabase writableDb;
    private SQLiteDatabase readableDb;

    public static class FavoriteTable implements BaseColumns {
        public static final String TABLE_NAME = "favorite";
        public static final String COLUMN_URL = "url";
    }
    private static final String SQL_CREATE_FAVORITE = "" +
        "CREATE TABLE " + FavoriteTable.TABLE_NAME + " (" +
        FavoriteTable._ID + " INTEGER PRIMARY KEY, " +
        FavoriteTable.COLUMN_URL + " TEXT)";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_FAVORITE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public void insertFavorite(String url) {
        if (writableDb == null) {
            writableDb = getWritableDatabase();
        }
        ContentValues values = new ContentValues();
        values.put(FavoriteTable.COLUMN_URL, url);
        writableDb.insert(FavoriteTable.TABLE_NAME, null, values);
    }

    public Cursor readFavorite() {
        if (readableDb == null) {
            readableDb = getReadableDatabase();
        }
        String[] projection = {
            BaseColumns._ID,
            FavoriteTable.COLUMN_URL
        };
        return readableDb.query(
                FavoriteTable.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );
    }

    public void deleteFavorite(String url) {
        if (writableDb == null) {
            writableDb = getWritableDatabase();
        }
        String selection = FavoriteTable.COLUMN_URL + " = ?";
        String[] selectionArgs = {url};
        writableDb.delete(FavoriteTable.TABLE_NAME, selection, selectionArgs);
    }
}
