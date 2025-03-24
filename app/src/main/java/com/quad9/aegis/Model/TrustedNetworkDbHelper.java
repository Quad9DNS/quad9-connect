package com.quad9.aegis.Model;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class TrustedNetworkDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "TrustedNetworks.db";
    private static TrustedNetworkDbHelper mInstance = null;
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TrustedNetworkContract.TrustedNetworkEntry.TABLE_NAME + " (" +
                    TrustedNetworkContract.TrustedNetworkEntry.COLUMN_BSSID + " TEXT PRIMARY KEY," +
                    TrustedNetworkContract.TrustedNetworkEntry.COLUMN_SSID + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TrustedNetworkContract.TrustedNetworkEntry.TABLE_NAME;

    public TrustedNetworkDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static TrustedNetworkDbHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TrustedNetworkDbHelper(context);
        }
        return mInstance;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new SQLiteException("Can't upgrade database from version " +
                oldVersion + " to " + newVersion);
    }

    public List<TrustedNetwork> getTrustedNetworks() {
        List<TrustedNetwork> networks = new ArrayList<>();
        try (
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT * FROM " + TrustedNetworkContract.TrustedNetworkEntry.TABLE_NAME, null)
        ) {
            while (cursor.moveToNext()) {
                int bssidIndex = cursor.getColumnIndex(TrustedNetworkContract.TrustedNetworkEntry.COLUMN_BSSID);
                int ssidIndex = cursor.getColumnIndex(TrustedNetworkContract.TrustedNetworkEntry.COLUMN_SSID);
                if (bssidIndex < 0 || ssidIndex < 0) {
                    continue;
                }
                TrustedNetwork network = new TrustedNetwork(
                        cursor.getString(bssidIndex),
                        cursor.getString(ssidIndex)
                );
                networks.add(network);
            }
        }
        return networks;
    }

    public void addTrustedNetwork(TrustedNetwork trustedNetwork) {
        try (
                SQLiteDatabase db = getWritableDatabase()
        ) {
            db.execSQL("INSERT INTO " + TrustedNetworkContract.TrustedNetworkEntry.TABLE_NAME + "('bssid', 'ssid') VALUES (?, ?)", new Object[]{trustedNetwork.getBssid(), trustedNetwork.getSsid()});
        }
    }

    public boolean isTrustedNetwork(TrustedNetwork trustedNetwork) {
        if (trustedNetwork.getBssid() == null) {
            return false;
        }

        try (
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT * FROM " + TrustedNetworkContract.TrustedNetworkEntry.TABLE_NAME + " WHERE bssid = ?", new String[]{trustedNetwork.getBssid()})
        ) {
            return cursor.getCount() > 0;
        }
    }

    public void removeTrustedNetwork(TrustedNetwork trustedNetwork) {
        try (
                SQLiteDatabase db = getWritableDatabase()
        ) {
            db.execSQL("DELETE FROM " + TrustedNetworkContract.TrustedNetworkEntry.TABLE_NAME + " WHERE bssid = ?", new Object[]{trustedNetwork.getBssid()});
        }
    }
}