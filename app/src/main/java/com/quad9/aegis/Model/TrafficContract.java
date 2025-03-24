package com.quad9.aegis.Model;

import android.provider.BaseColumns;

public class TrafficContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private TrafficContract() {
    }

    /* Inner class that defines the table contents */
    public static class TrafficEntry implements BaseColumns {
        public static final String TABLE_NAME = "traffic";
        public static final String COLUMN_NAME_TITLE = "type";
        public static final String COLUMN_NAME_SUBTITLE = "timestamp";
    }

}
