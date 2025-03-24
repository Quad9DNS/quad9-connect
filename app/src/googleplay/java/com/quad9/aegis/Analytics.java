package com.quad9.aegis;

import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public enum Analytics {
    INSTANCE;

    /**
     * @return true if analytics is supported in current build.
     */
    public boolean isSupported() {
        return true;
    }

    /**
     * If analytics are supported, crashes the app to test out integration
     */
    public void testCrash() {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCustomKey("CrashType", "click crash button");
        int a = 100/0;
    }

    /**
     * Sets a custom key in the underlying analytics service, if supported
     */
    public void setCustomCrashlyticsKey(String key, String value) {
        FirebaseCrashlytics.getInstance().setCustomKey(key, value);
    }

    /**
     * Enables analytics, if supported
     */
    public void setAnalyticsCollectionEnabled(Context context, boolean enabled) {
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled);
    }

    /**
     * Initializes the analytics service, if supported
     */
    public void initialize(Context context) {
        FirebaseAnalytics.getInstance(context);
    }

    /**
     * Sends a log message to underlying analytics service, if supported
     */
    public void log(String message) {
        FirebaseCrashlytics.getInstance().log(message);
    }
}
