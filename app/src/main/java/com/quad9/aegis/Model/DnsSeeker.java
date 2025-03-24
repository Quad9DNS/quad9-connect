package com.quad9.aegis.Model;

import static java.lang.StrictMath.min;

import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.quad9.aegis.BuildConfig;
import com.quad9.aegis.MainActivity;
import com.quad9.aegis.R;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.measite.minidns.Record;


public class DnsSeeker extends Application {
    private static final int MSG_TEST_OK = 1;
    private static final int MSG_TEST_FAILED = 2;
    private static final int MSG_TEST_PRIVATE = 3;

    static int JOB_ID = 13579;
    static int HIGH_ID = 11111;

    private static final String TAG = "Quad9 Connect";
    static String versionString = "";

    // Statistics
    static int success;
    static int fail;
    static int blocked;
    static int aliveTime;
    static List<ResponseRecord> recentResponse = new ArrayList<ResponseRecord>();
    static List<ResponseRecord> blockedResponse = new ArrayList<ResponseRecord>();
    static List<ResponseRecord> failedResponse = new ArrayList<ResponseRecord>();
    static Lock lock = new ReentrantLock();
    static String lastBlockedTime = "";
    static String lastBlockedDomain = "";

    // Singleton
    private static DnsSeeker instance = new DnsSeeker();
    static ConnectStatus status;
    static SharedPreferences sharedPref;

    static TestHandler testHandler = new TestHandler(getInstance());

    private static class TestHandler extends Handler {

        WeakReference<DnsSeeker> mActivity;

        TestHandler(DnsSeeker activity) {
            mActivity = new WeakReference<DnsSeeker>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Intent networkIntent = new Intent(GlobalVariables.NetworkStatus);

            switch (msg.what) {
                case MSG_TEST_OK:
                    networkIntent.putExtra("connected", true);
                    activateVpnService();
                    LocalBroadcastManager.getInstance(getInstance()).sendBroadcast(networkIntent);
                    break;
                case MSG_TEST_FAILED:
                    popToast(R.string.toast_unreachable);
                    status.setConnected(false);
                    networkIntent.putExtra("connected", false);
                    LocalBroadcastManager.getInstance(getInstance()).sendBroadcast(networkIntent);
                    break;
                case MSG_TEST_PRIVATE:
                    popToast(R.string.toast_privatedns);
                    status.setConnected(false);
                    networkIntent.putExtra("connected", false);
                    LocalBroadcastManager.getInstance(getInstance()).sendBroadcast(networkIntent);
                    break;
            }
            super.handleMessage(msg);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        success = 0;
        fail = 0;


        //}
        Gson gson = new Gson();
        Type listType = new TypeToken<List<ResponseRecord>>() {
        }.getType();

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        PreferenceManager.setDefaultValues(this, R.xml.preference, false);
        success = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt("success", 0);
        fail = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt("fail", 0);
        blocked = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt("blocked_q", 0);
        aliveTime = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt("alive_time", 0);

        if (recentResponse.size() == 0) {
            String temp = PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getString("recent_response", "");
            if (!temp.equals("")) {
                recentResponse = gson.fromJson(temp, listType);
            }
        }

        String temp = PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getString("blocked_response", "");
        if (!temp.equals("")) {
            blockedResponse = gson.fromJson(temp, listType);
        }

        temp = PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getString("failed_response", "");
        if (!temp.equals("")) {
            failedResponse = gson.fromJson(temp, listType);
        }

        status = new ConnectStatus();

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                resetReceiver, new IntentFilter("ResetStats"));

        try {
            PackageInfo packageInfo = getInstance().getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(getInstance().getPackageName(), 0);
            versionString = packageInfo.versionName.replace(".", "-");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //if (!BuildConfig.DEBUG) { // Makes it easier to see backtraces when testing
        CoreConfigurationBuilder builder = new CoreConfigurationBuilder();
        builder.withBuildConfigClass(BuildConfig.class).withReportFormat(StringFormat.JSON);
        builder.withPluginConfigurations(
                new DialogConfigurationBuilder()
                        .withText(this.getResources().getString(R.string.crash_dialog_text))
                        .withTitle(this.getResources().getString(R.string.crash_dialog_title))
                        .withResTheme(R.style.AppThemeDialog)
                        //allows other customization
                        .build(),
                new MailSenderConfigurationBuilder()
                        .withMailTo("android-support@quad9.net")
                        .withReportAsFile(true)
                        .withReportFileName("crash_report.txt")
                        .withEnabled(true)
                        .build()
        );

        ACRA.init(this, builder);
    }

    public SharedPreferences getSharedConfig() {
        return PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    public static ConnectStatus getStatus() {
        return status;
    }

    // Setters are for mock and test.
    public static void setStatus(ConnectStatus s) {
        status = s;
    }

    public static void setInstance(DnsSeeker s) {
        instance = s;
    }

    public static void setStats(int success, int fail, int blocked) {
        getInstance().success = success;
        getInstance().fail = fail;
        getInstance().blocked = blocked;
    }

    private void saveStatistics() {
        updateAliveTime();
        getStatus().updateTraffic();
        SharedPreferences.Editor editor = sharedPref.edit();
        Gson gson = new Gson();
        editor.putInt("success", success);
        editor.putInt("fail", fail);
        editor.putInt("total_q", success + fail);
        editor.putInt("blocked_q", blocked);
        editor.putInt("alive_time", aliveTime);
        editor.putString("recent_response", gson.toJson(recentResponse));
        editor.putString("blocked_response", gson.toJson(blockedResponse));
        editor.putString("failed_response", gson.toJson(failedResponse));
        editor.apply();
    }


    public static boolean activateService() {
        lock.lock();
        try {
            status.configBySetting(
                    PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getBoolean("checkbox_malicious", true),
                    PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getBoolean("checkbox_ECS", false),
                    PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getBoolean("checkbox_tls", true),
                    PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getBoolean("checkbox_notification", true),
                    PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getBoolean("checkbox_enhanced", true),
                    PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getStringSet("whitelistDomain", new HashSet<String>()),
                    PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getStringSet("wildcardDomain", new HashSet<String>())
            );
            Intent networkIntent = new Intent(GlobalVariables.NetworkStatus);


            // Should we test through different connection?

            if (status.isUsingTLS()) {
                Thread thread = new Thread(testConnection);
                thread.start();
            } else {
                Thread thread = new Thread(testConnection);
                thread.start();
                //TestQuad9.dig_over_udp(getInstance());
            }


            // This Part is deprecated because network connection should not block main thread.

            if (false) {
                if (!status.isUsingTLS()) {
                    if (TestQuad9.dig_over_udp(getInstance())) {
                        activateVpnService();
                        networkIntent.putExtra("connected", true);
                    } else if (TestQuad9.dig_over_tls(getInstance(), null)) {
                        // Cannot ping by UDP but ping successfully by TLS implies that DNS has been hijacked.
                        status.setConnected(true);
                        networkIntent.putExtra("connected", true);
                        activateVpnService();
                        popToast(R.string.toast_hijacked);

                    } else {
                        popToast(R.string.toast_unreachable);
                        status.setConnected(false);
                        getStatus().setShouldAutoConnect(false);
                        networkIntent.putExtra("connected", false);
                    }
                    LocalBroadcastManager.getInstance(getInstance()).sendBroadcast(networkIntent);

                } else {
                        /*if (TestQuad9.dig_over_tls(getInstance())) {
                            networkIntent.putExtra("connected", true);
                            activateVpnService();

                        } else {
                            popToast(R.string.toast_unreachable);
                            status.setConnected(false);
                            networkIntent.putExtra("connected", false);
                        }*/
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        return true;

    }


    private static Runnable testConnection = new Runnable() {
        @Override
        public void run() {
            if (DnsSeeker.getInstance().getConnectionMonitor().isPrivateDnsActive()) {
                testHandler.sendEmptyMessage(MSG_TEST_PRIVATE);
                return;
            }
            if (TestQuad9.dig_over_tls(getInstance(), TestQuad9.getInstance().getServerCallback)) {
                testHandler.sendEmptyMessage(MSG_TEST_OK);
            } else {
                testHandler.sendEmptyMessage(MSG_TEST_FAILED);
            }
        }
    };

    public static void activateVpnService() {
        Intent it = VpnService.prepare(instance.getApplicationContext());
        status.configBySetting(
                PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getBoolean("checkbox_malicious", true),
                PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getBoolean("checkbox_ecs", false),
                PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getBoolean("checkbox_tls", true),
                PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getBoolean("checkbox_notification", true),
                PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getBoolean("checkbox_enhanced", true),
                PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getStringSet("whitelistDomain", new HashSet<String>()),
                PreferenceManager.getDefaultSharedPreferences(getInstance().getApplicationContext()).getStringSet("wildcardDomain", new HashSet<String>())
        );

        Intent intent = new Intent(getInstance(), VpnSeekerService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            getInstance().startForegroundService(intent.setAction("start"));
        } else {
            getInstance().startService(intent.setAction("start"));
        }
        popToast(R.string.toast_connected);
        status.setConnected(true);

        // Send here because "restart" only call this function. (no activateService, also means no test.)
        // The is sent twice if triggered with button.
        Intent networkIntent = new Intent(GlobalVariables.NetworkStatus);
        networkIntent.putExtra("connected", true);
        LocalBroadcastManager.getInstance(getInstance()).sendBroadcast(networkIntent);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("active", true);
        editor.commit();
    }

    public static boolean deActivateService() {
        lock.lock();
        try {
            if (status.isActive() && status.isConnected()) {
                Intent intent = new Intent(instance, VpnSeekerService.class);
                intent.setAction("stopping");
                getInstance().startService(intent.setAction("stopping"));
                EventController.getNotiManager().cancelAll();
            }
            status.setActivated(false);

            Intent intent = new Intent(GlobalVariables.NetworkStatus);
            intent.putExtra("connected", false);
            LocalBroadcastManager.getInstance(getInstance()).sendBroadcast(intent);
            getInstance().saveStatistics();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("active", false);
            editor.commit();
        } catch (Exception e) {
        } finally {
            lock.unlock();
        }
        return true;
    }

    public static void dismissHighNotification() {
        EventController.getHighNotiManager().cancelAll();
        //EventController.getNotiManager().cancelAll();
    }

    //Seems impossible to handle forced closed scenario.

    public static void onForceClose() {

        //EventController.getNotiManager().cancelAll();
        //Log.d("notification","cancelall");
        //Intent intent = new Intent(GlobalVariables.NetworkStatus);
        //intent.putExtra("connected", false);
        //LocalBroadcastManager.getInstance(getInstance()).sendBroadcast(intent);
        //status.setActivated(false);
    }


    public static void scheduleRestart(int delay, int interval) {
        scheduleJob(instance.getApplicationContext(), delay, interval);
    }

    public static void scheduleJob(Context context, int delay, int interval) {
        Log.d("restart", "ScheduleDeActivate");
        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt("interval", interval);
        ComponentName serviceComponent = new ComponentName(context, RestartJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(delay * 1000); // wait at least
        builder.setOverrideDeadline((delay + 2) * 1000); // maximum delay
        builder.setExtras(bundle);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(context.JOB_SCHEDULER_SERVICE);

        jobScheduler.schedule(builder.build());
    }

    public static DnsSeeker getInstance() {
        return instance;
    }

    public static boolean testForCaptivePortal() {

        // Give time for captive portal to open.
        URL mURL = null;
        try {
            mURL = new URL("http", "connectivitycheck.gstatic.com", "/generate_204");
        } catch (Exception e) {

        }
        HttpURLConnection urlConnection = null;
        int httpResponseCode = 0;
        int SOCKET_TIMEOUT_MS = 1500;
        try {
            urlConnection = (HttpURLConnection) mURL.openConnection();
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setConnectTimeout(SOCKET_TIMEOUT_MS);
            urlConnection.setReadTimeout(SOCKET_TIMEOUT_MS);
            urlConnection.setUseCaches(false);
            urlConnection.getInputStream();
            httpResponseCode = urlConnection.getResponseCode();
        } catch (IOException e) {
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        if (httpResponseCode == 204) {
            return true;
        } else {
            return false;
        }
    }

    private static void sendUpdateToActivity() {
        Intent intent = new Intent("ResponseResult");
        LocalBroadcastManager.getInstance(getInstance().getApplicationContext()).sendBroadcast(intent);
    }

    // The method is called when a wifi connected but cannot get access to quad9.
    public static void checkWifiAvailable() {

        Intent notificationIntent = new Intent(instance, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        Intent intentAction = new Intent(instance, DeviceIntentReceiver.class);

        intentAction.setAction("pause");

        // If this is triggered twice there will be a duplicate action
        // It will trigger a restart of the app within 60 second.
        PendingIntent pIntentlogin = PendingIntent.getBroadcast(instance, 1, intentAction, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Action action = new Notification.Action.Builder(R.drawable.ic_touch, getInstance().getResources().getString(R.string.noti_option_network_problem), pIntentlogin).build();
        if (status.isUsingNotification()) {
            try {
                EventController.getHighNotiManager().cancel(HIGH_ID);
                EventController.getHighNotiManager().notify(HIGH_ID, EventController.getHighNotiBuilder().setContentTitle(getInstance().getResources().getString(R.string.noti_title_network_problem))
                        .setContentText(getInstance().getResources().getString(R.string.noti_content_network_problem))
                        .setSmallIcon(R.drawable.ic_favicon)
                        .addAction(action)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true)
                        .build()
                );
            } catch (Exception e) {

            }
        }
    }

    // This should not a included in the app.

    public static void updateAliveTime() {
        if (aliveTime == 0) {
            aliveTime = (int) System.currentTimeMillis() / 3600000;
        }
        TestQuad9.queryTls(String.format("android-%s.appcounter.quad9.net", versionString), Record.TYPE.A);
    }

    public static void popToast(final int Rid) {
        if (status.isUsingNotification()) {
            String s = getInstance().getResources().getString(Rid);
            Log.i(TAG, "Toast: " + s);

            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(DnsSeeker.getInstance(), s, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static void popToast(String s) {
        if (status.isUsingNotification()) {
            Log.i(TAG, "Toast: " + s);
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(DnsSeeker.getInstance(), s, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private BroadcastReceiver resetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            resetList();
        }

    };

    /**** FOR STATISTIC *****/
    public void addResponse(ResponseRecord r) {
        Log.i(TAG, "addResponse");

        if (recentResponse.size() > 200) {
            recentResponse.remove(recentResponse.size() - 1);
        }
        if (r != null) {
            recentResponse.add(0, r);
            success++;
            status.updateSpeed(r.time);
            sendUpdateToActivity();
            if (success % 200 == 0) {
                saveStatistics();
            }
        }
    }

    public void addBlocked(ResponseRecord r) {
        status.setRecentBlocking();

        ResponseRecord record = ResponseParser.parseResponseDetail(r);

        try {
            if (!record.timeStamp.equals(lastBlockedTime) || !lastBlockedDomain.equals(record.name)) {
                lastBlockedTime = record.timeStamp;
                lastBlockedDomain = record.name;
                if (blockedResponse.size() > 100) {
                    blockedResponse.remove(blockedResponse.size() - 1);
                }
                if (r != null) {
                    blocked++;
                    blockedResponse.add(0, r);
                }
                if (status.isUsingNotification()) {
                    Intent notificationIntent = new Intent(instance, MainActivity.class);
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    PendingIntent notiPendingIntent = PendingIntent.getActivity(instance, 0,
                            notificationIntent, PendingIntent.FLAG_IMMUTABLE);
                    EventController.getHighNotiManager().notify(HIGH_ID, EventController.getHighNotiBuilder().setContentTitle(getResources().getString(R.string.toast_malicious))
                            .setContentIntent(notiPendingIntent)
                            .setContentText(getInstance().getResources().getString(R.string.noti_content_block) + "\"" + record.name + "\"")
                            .setSmallIcon(R.drawable.ic_favicon)
                            .setPriority(Notification.PRIORITY_HIGH)
                            .build()
                    );
                }
                saveStatistics();
            }

        } catch (Exception e) {
            Log.e(TAG, "addBlock failed" + e);
        }
        // Notification for block
    }

    public void addFail(ResponseRecord r) {
        if (recentResponse.size() > 200) {
            recentResponse.remove(recentResponse.size() - 1);
        }

        if (failedResponse.size() > 200) {
            failedResponse.remove(failedResponse.size() - 1);
        }
        if (r != null) {
            recentResponse.add(0, r);
            failedResponse.add(0, r);
            fail++;
        }

    }


    public List<ResponseRecord> getResponse() {
        for (int i = 0; i < recentResponse.size(); i++) {
            if (recentResponse.get(i).rawData != null) {
                recentResponse.set(i, ResponseParser.parseResponseDetail(recentResponse.get(i)));
            }
        }
        // Since the responseResponse is iterated from top down check if there's any unparsed
        // Bad implementation, still a DnsSeeker.TAG here.
        for (int i = 0; i < min(5, recentResponse.size()); i++) {
            if (recentResponse.get(i).rawData != null) {
                recentResponse.set(i, ResponseParser.parseResponseDetail(recentResponse.get(i)));
            }
        }
        // Pass by value prevent from unparsed recent Response
        return new ArrayList<>(recentResponse);
    }

    public List<ResponseRecord> getBlocked() {
        for (int i = 0; i < blockedResponse.size(); i++) {
            if (blockedResponse.get(i).rawData != null) {
                blockedResponse.set(i, ResponseParser.parseResponseDetail(blockedResponse.get(i)));
            }
        }
        return new ArrayList<>(blockedResponse);
    }

    public List<ResponseRecord> getFailedResponse() {
        for (int i = 0; i < failedResponse.size(); i++) {
            if (failedResponse.get(i) != null && failedResponse.get(i).rawData != null) {
                failedResponse.set(i, ResponseParser.parseResponseDetail(failedResponse.get(i)));
            }
        }
        // Since the responseResponse is iterated from top down check if there's any unparsed
        // Bad implementation, still a DnsSeeker.TAG here.
        for (int i = 0; i < min(5, failedResponse.size()); i++) {
            if (failedResponse.get(i) != null && failedResponse.get(i).rawData != null) {
                failedResponse.set(i, ResponseParser.parseResponseDetail(failedResponse.get(i)));
            }
        }
        // Pass by value prevent from unparsed recent Response
        return new ArrayList<>(failedResponse);
    }

    public void resetList() {
        fail = 0;
        success = 0;
        blocked = 0;
        blockedResponse.clear();
        recentResponse.clear();
        failedResponse.clear();
        status.resetSpeed();
    }

    public int getTotalCount() {
        return success;
    }

    public int getFailCount() {
        return fail;
    }

    public int getSuccessCount() {
        return success;
    }

    public int getBlockedCount() {
        return blocked;
    }

    public ConnectionMonitor getConnectionMonitor() {
        return ConnectionMonitor.getInstance();
    }

    @Override
    public void onTerminate() {
        saveStatistics();
        super.onTerminate();
    }
}








