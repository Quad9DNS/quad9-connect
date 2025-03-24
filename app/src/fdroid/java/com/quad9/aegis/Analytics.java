package com.quad9.aegis;

import android.content.Context;

public enum Analytics {
    INSTANCE;

    /**
     * @return true if analytics is supported in current build.
     */
    public boolean isSupported() {
        return false;
    }

    /**
     * If analytics are supported, crashes the app to test out integration
     */
    public void testCrash() {
        // no-op
    }

    /**
     * Sets a custom key in the underlying analytics service, if supported
     */
    public void setCustomCrashlyticsKey(String key, String value) {
        // no-op
    }

    /**
     * Enables analytics, if supported
     */
    public void setAnalyticsCollectionEnabled(Context context, boolean enabled) {
        // no-op
    }

    /**
     * Initializes the analytics service, if supported
     */
    public void initialize(Context context) {
        // no-op
    }

    /**
     * Sends a log message to underlying analytics service, if supported
     */
    public void log(String message) {
        // no-op
    }
}
