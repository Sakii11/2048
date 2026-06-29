package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "game2048.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_SETTINGS = "game_settings";
    private static final String COL_ID = "id";
    private static final String COL_BEST_SCORE = "best_score";
    private static final String COL_APP_THEME = "app_theme";

    private static final int SETTINGS_ROW_ID = 1;

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_SETTINGS + " (" +
                COL_ID + " INTEGER PRIMARY KEY, " +
                COL_BEST_SCORE + " INTEGER NOT NULL DEFAULT 0, " +
                COL_APP_THEME + " TEXT NOT NULL DEFAULT 'CLASSIC')";
        db.execSQL(createTable);

        ContentValues values = new ContentValues();
        values.put(COL_ID, SETTINGS_ROW_ID);
        values.put(COL_BEST_SCORE, 0);
        values.put(COL_APP_THEME, "CLASSIC");
        db.insert(TABLE_SETTINGS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    public int getBestScore() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{COL_BEST_SCORE},
                COL_ID + " = ?", new String[]{String.valueOf(SETTINGS_ROW_ID)},
                null, null, null);
        int score = 0;
        if (cursor.moveToFirst()) {
            score = cursor.getInt(cursor.getColumnIndexOrThrow(COL_BEST_SCORE));
        }
        cursor.close();
        return score;
    }

    public void setBestScore(int score) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_BEST_SCORE, score);
        db.update(TABLE_SETTINGS, values, COL_ID + " = ?",
                new String[]{String.valueOf(SETTINGS_ROW_ID)});
    }

    public String getAppTheme() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{COL_APP_THEME},
                COL_ID + " = ?", new String[]{String.valueOf(SETTINGS_ROW_ID)},
                null, null, null);
        String theme = "CLASSIC";
        if (cursor.moveToFirst()) {
            theme = cursor.getString(cursor.getColumnIndexOrThrow(COL_APP_THEME));
        }
        cursor.close();
        return theme;
    }

    public void setAppTheme(String theme) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_APP_THEME, theme);
        db.update(TABLE_SETTINGS, values, COL_ID + " = ?",
                new String[]{String.valueOf(SETTINGS_ROW_ID)});
    }
}
