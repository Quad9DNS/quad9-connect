package com.quad9.aegis.Model;

import android.provider.BaseColumns;

public class TrustedNetworkContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private TrustedNetworkContract() {
    }

    /* Inner class that defines the table contents */
    public static class TrustedNetworkEntry implements BaseColumns {
        public static final String TABLE_NAME = "trusted_networks";
        public static final String COLUMN_BSSID = "bssid";
        public static final String COLUMN_SSID = "ssid";
    }

}
