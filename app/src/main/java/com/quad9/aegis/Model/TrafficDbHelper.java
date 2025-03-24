package com.quad9.aegis.Model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TrafficDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FeedReader.db";
    private static TrafficDbHelper mInstance = null;
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TrafficContract.TrafficEntry.TABLE_NAME + " (" +
                    TrafficContract.TrafficEntry._ID + " INTEGER PRIMARY KEY," +
                    TrafficContract.TrafficEntry.COLUMN_NAME_TITLE + " INT," +
                    TrafficContract.TrafficEntry.COLUMN_NAME_SUBTITLE + " DATETIME DEFAULT CURRENT_TIMESTAMP)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TrafficContract.TrafficEntry.TABLE_NAME;

    public TrafficDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static TrafficDbHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TrafficDbHelper(context);
        }
        return mInstance;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}

