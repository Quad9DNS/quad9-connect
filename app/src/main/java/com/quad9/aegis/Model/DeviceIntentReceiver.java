package com.quad9.aegis.Model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.quad9.aegis.R;


public class DeviceIntentReceiver extends BroadcastReceiver {
    private static final String TAG = "DeviceIntentReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") ||
                intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON") ||
                intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) {
            Log.d(TAG, "event get");
            if (PreferenceManager.getDefaultSharedPreferences(DnsSeeker.getInstance().getApplicationContext()).getBoolean("checkbox_auto_start", false)
                    && PreferenceManager.getDefaultSharedPreferences(DnsSeeker.getInstance().getApplicationContext()).getBoolean("active", false)
            ) {
                DnsSeeker.activateVpnService();
            }
        } else if ("pause".equals(intent.getAction())) {
            // Borrow the broadcast receiver for restart in 1 min.
            Log.d(TAG, "receive Pause");
            DnsSeeker.scheduleRestart(1, 60);
            DnsSeeker.popToast(R.string.toast_restart);

        }

    }
}
