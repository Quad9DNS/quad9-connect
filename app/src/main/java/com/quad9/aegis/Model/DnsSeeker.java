package com.quad9.aegis.Model;

import static java.lang.StrictMath.min;

import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.quad9.aegis.BuildConfig;
import com.quad9.aegis.MainActivity;
import com.quad9.aegis.R;
import com.quad9.aegis.util.LiveEvent;
import de.measite.minidns.Record;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;

public class DnsSeeker extends Application {
  private static final int MSG_TEST_OK = 1;
  private static final int MSG_TEST_FAILED = 2;
  private static final int MSG_TEST_PRIVATE = 3;

  static int HIGH_ID = 11111;

  private static final String TAG = "Quad9 Connect";
  static String versionString = "";

  // Statistics
  static MutableLiveData<Integer> successMut = new MutableLiveData<>(0);
  public static LiveData<Integer> success = successMut;
  static MutableLiveData<Integer> failMut = new MutableLiveData<>(0);
  public static LiveData<Integer> fail = failMut;
  static MutableLiveData<Integer> blockedMut = new MutableLiveData<>(0);
  public static LiveData<Integer> blocked = blockedMut;
  static MutableLiveData<Integer> aliveTimeMut = new MutableLiveData<>(0);
  static List<ResponseRecord> recentResponse = new ArrayList<ResponseRecord>();
  static List<ResponseRecord> blockedResponse = new ArrayList<ResponseRecord>();
  static List<ResponseRecord> failedResponse = new ArrayList<ResponseRecord>();
  public static LiveEvent<Void> responsesUpdated = new LiveEvent<>();
  static Lock lock = new ReentrantLock();
  static String lastBlockedTime = "";
  static String lastBlockedDomain = "";

  // Singleton
  private static DnsSeeker instance = new DnsSeeker();
  static ConnectStatus status;
  static SharedPreferences sharedPref;

  static TestHandler testHandler = new TestHandler();

  private static class TestHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      Intent networkIntent = new Intent(GlobalVariables.NetworkStatus);

      switch (msg.what) {
      case MSG_TEST_OK:
        networkIntent.putExtra("connected", true);
        activateVpnService();
        LocalBroadcastManager.getInstance(getInstance())
            .sendBroadcast(networkIntent);
        break;
      case MSG_TEST_FAILED:
        popToast(R.string.toast_unreachable);
        status.setConnected(false);
        networkIntent.putExtra("connected", false);
        LocalBroadcastManager.getInstance(getInstance())
            .sendBroadcast(networkIntent);
        break;
      case MSG_TEST_PRIVATE:
        popToast(R.string.toast_privatedns);
        status.setConnected(false);
        networkIntent.putExtra("connected", false);
        LocalBroadcastManager.getInstance(getInstance())
            .sendBroadcast(networkIntent);
        break;
      }
      super.handleMessage(msg);
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();

    instance = this;

    successMut.postValue(0);
    failMut.postValue(0);

    //}
    Gson gson = new Gson();
    Type listType = new TypeToken<List<ResponseRecord>>() {}.getType();

    sharedPref =
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    PreferenceManager.setDefaultValues(this, R.xml.preference, false);
    successMut.postValue(
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
            .getInt("success", 0));
    failMut.postValue(
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
            .getInt("fail", 0));
    blockedMut.postValue(
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
            .getInt("blocked_q", 0));
    aliveTimeMut.postValue(
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
            .getInt("alive_time", 0));

    if (recentResponse.size() == 0) {
      String temp = PreferenceManager
                        .getDefaultSharedPreferences(
                            getInstance().getApplicationContext())
                        .getString("recent_response", "");
      if (!temp.equals("")) {
        recentResponse = gson.fromJson(temp, listType);
      }
    }

    String temp =
        PreferenceManager
            .getDefaultSharedPreferences(getInstance().getApplicationContext())
            .getString("blocked_response", "");
    if (!temp.equals("")) {
      blockedResponse = gson.fromJson(temp, listType);
    }

    temp =
        PreferenceManager
            .getDefaultSharedPreferences(getInstance().getApplicationContext())
            .getString("failed_response", "");
    if (!temp.equals("")) {
      failedResponse = gson.fromJson(temp, listType);
    }

    status = new ConnectStatus();

    try {
      PackageInfo packageInfo =
          getInstance()
              .getApplicationContext()
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
    // if (!BuildConfig.DEBUG) { // Makes it easier to see backtraces when
    // testing
    CoreConfigurationBuilder builder = new CoreConfigurationBuilder();
    builder.withBuildConfigClass(BuildConfig.class)
        .withReportFormat(StringFormat.JSON);
    builder.withPluginConfigurations(
        new DialogConfigurationBuilder()
            .withText(this.getResources().getString(R.string.crash_dialog_text))
            .withTitle(
                this.getResources().getString(R.string.crash_dialog_title))
            .withResTheme(R.style.AppThemeDialog)
            // allows other customization
            .build(),
        new MailSenderConfigurationBuilder()
            .withMailTo("android-support@quad9.net")
            .withReportAsFile(true)
            .withReportFileName("crash_report.txt")
            .withEnabled(true)
            .build());

    ACRA.init(this, builder);
  }

  public SharedPreferences getSharedConfig() {
    return PreferenceManager.getDefaultSharedPreferences(
        getApplicationContext());
  }

  public static ConnectStatus getStatus() { return status; }

  // Setters are for mock and test.
  public static void setStatus(ConnectStatus s) { status = s; }

  public static void setInstance(DnsSeeker s) { instance = s; }

  private void saveStatistics() {
    updateAliveTime();
    getStatus().updateTraffic();
    SharedPreferences.Editor editor = sharedPref.edit();
    Gson gson = new Gson();
    editor.putInt("success", successMut.getValue());
    editor.putInt("fail", failMut.getValue());
    editor.putInt("total_q", successMut.getValue() + failMut.getValue());
    editor.putInt("blocked_q", blockedMut.getValue());
    editor.putInt("alive_time", aliveTimeMut.getValue());
    editor.putString("recent_response", gson.toJson(recentResponse));
    editor.putString("blocked_response", gson.toJson(blockedResponse));
    editor.putString("failed_response", gson.toJson(failedResponse));
    editor.apply();
  }

  public static boolean activateService() {
    lock.lock();
    try {
      status.configBySetting(
          PreferenceManager
              .getDefaultSharedPreferences(
                  getInstance().getApplicationContext())
              .getBoolean("checkbox_malicious", true),
          PreferenceManager
              .getDefaultSharedPreferences(
                  getInstance().getApplicationContext())
              .getBoolean("checkbox_ECS", false),
          PreferenceManager
              .getDefaultSharedPreferences(
                  getInstance().getApplicationContext())
              .getBoolean("checkbox_tls", true),
          PreferenceManager
              .getDefaultSharedPreferences(
                  getInstance().getApplicationContext())
              .getBoolean("checkbox_notification", true),
          PreferenceManager
              .getDefaultSharedPreferences(
                  getInstance().getApplicationContext())
              .getBoolean("checkbox_enhanced", true),
          PreferenceManager
              .getDefaultSharedPreferences(
                  getInstance().getApplicationContext())
              .getStringSet("whitelistDomain", new HashSet<String>()),
          PreferenceManager
              .getDefaultSharedPreferences(
                  getInstance().getApplicationContext())
              .getStringSet("wildcardDomain", new HashSet<String>()));

      Thread thread = new Thread(testConnection);
      thread.start();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      lock.unlock();
    }

    return true;
  }

  private static final Runnable testConnection = new Runnable() {
    @Override
    public void run() {
      if (DnsSeeker.getInstance().getConnectionMonitor().isPrivateDnsActive()) {
        testHandler.sendEmptyMessage(MSG_TEST_PRIVATE);
        return;
      }
      if (TestQuad9.dig_over_tls(getInstance(),
                                 TestQuad9.getInstance().getServerCallback)) {
        testHandler.sendEmptyMessage(MSG_TEST_OK);
      } else {
        testHandler.sendEmptyMessage(MSG_TEST_FAILED);
      }
    }
  };

  public static void activateVpnService() {
    status.configBySetting(
        PreferenceManager
            .getDefaultSharedPreferences(getInstance().getApplicationContext())
            .getBoolean("checkbox_malicious", true),
        PreferenceManager
            .getDefaultSharedPreferences(getInstance().getApplicationContext())
            .getBoolean("checkbox_ecs", false),
        PreferenceManager
            .getDefaultSharedPreferences(getInstance().getApplicationContext())
            .getBoolean("checkbox_tls", true),
        PreferenceManager
            .getDefaultSharedPreferences(getInstance().getApplicationContext())
            .getBoolean("checkbox_notification", true),
        PreferenceManager
            .getDefaultSharedPreferences(getInstance().getApplicationContext())
            .getBoolean("checkbox_enhanced", true),
        PreferenceManager
            .getDefaultSharedPreferences(getInstance().getApplicationContext())
            .getStringSet("whitelistDomain", new HashSet<String>()),
        PreferenceManager
            .getDefaultSharedPreferences(getInstance().getApplicationContext())
            .getStringSet("wildcardDomain", new HashSet<String>()));

    Intent intent = new Intent(getInstance(), VpnSeekerService.class);
    if (Build.VERSION.SDK_INT >= 26) {
      getInstance().startForegroundService(intent.setAction("start"));
    } else {
      getInstance().startService(intent.setAction("start"));
    }
    popToast(R.string.toast_connected);
    status.setConnected(true);

    // Send here because "restart" only call this function. (no activateService,
    // also means no test.) The is sent twice if triggered with button.
    Intent networkIntent = new Intent(GlobalVariables.NetworkStatus);
    networkIntent.putExtra("connected", true);
    LocalBroadcastManager.getInstance(getInstance())
        .sendBroadcast(networkIntent);
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

  // Seems impossible to handle forced closed scenario.

  public static void scheduleRestart(int delay, int interval) {
    scheduleJob(instance.getApplicationContext(), delay, interval);
  }

  public static void scheduleJob(Context context, int delay, int interval) {
    Log.d("restart", "ScheduleDeActivate");
    PersistableBundle bundle = new PersistableBundle();
    bundle.putInt("interval", interval);
    ComponentName serviceComponent =
        new ComponentName(context, RestartJobService.class);
    JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
    builder.setMinimumLatency(delay * 1000L);         // wait at least
    builder.setOverrideDeadline((delay + 2) * 1000L); // maximum delay
    builder.setExtras(bundle);
    JobScheduler jobScheduler =
        (JobScheduler)context.getSystemService(JOB_SCHEDULER_SERVICE);

    jobScheduler.schedule(builder.build());
  }

  public static DnsSeeker getInstance() { return instance; }

  private static void sendUpdateToActivity() { responsesUpdated.set(); }

  // This should not a included in the app.

  public static void updateAliveTime() {
    if (aliveTimeMut.getValue() == 0) {
      aliveTimeMut.postValue((int)System.currentTimeMillis() / 3600000);
    }
    TestQuad9.queryTls(
        String.format("android-%s.appcounter.quad9.net", versionString),
        Record.TYPE.A);
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

  /**** FOR STATISTIC *****/
  public void addResponse(ResponseRecord r) {
    Log.i(TAG, "addResponse");

    if (recentResponse.size() > 200) {
      recentResponse.remove(recentResponse.size() - 1);
    }
    if (r != null) {
      recentResponse.add(0, r);
      successMut.postValue(successMut.getValue() + 1);
      status.updateSpeed(r.time);
      sendUpdateToActivity();
      if (successMut.getValue() % 200 == 0) {
        saveStatistics();
      }
    }
  }

  public void addBlocked(ResponseRecord r) {
    status.setRecentBlocking();

    ResponseRecord record = ResponseParser.parseResponseDetail(r);

    try {
      if (!record.timeStamp.equals(lastBlockedTime) ||
          !lastBlockedDomain.equals(record.name)) {
        lastBlockedTime = record.timeStamp;
        lastBlockedDomain = record.name;
        if (blockedResponse.size() > 100) {
          blockedResponse.remove(blockedResponse.size() - 1);
        }
        if (r != null) {
          blockedMut.setValue(blockedMut.getValue() + 1);
          blockedResponse.add(0, r);
        }
        if (status.isUsingNotification()) {
          Intent notificationIntent = new Intent(instance, MainActivity.class);
          notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                      Intent.FLAG_ACTIVITY_SINGLE_TOP);
          PendingIntent notiPendingIntent = PendingIntent.getActivity(
              instance, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
          EventController.getHighNotiManager().notify(
              HIGH_ID,
              EventController.getHighNotiBuilder()
                  .setContentTitle(
                      getResources().getString(R.string.toast_malicious))
                  .setContentIntent(notiPendingIntent)
                  .setContentText(getInstance().getResources().getString(
                                      R.string.noti_content_block) +
                                  "\"" + record.name + "\"")
                  .setSmallIcon(R.drawable.ic_favicon)
                  .setPriority(Notification.PRIORITY_HIGH)
                  .build());
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
      failMut.setValue(failMut.getValue() + 1);
    }
  }

  public List<ResponseRecord> getResponse() {
    for (int i = 0; i < recentResponse.size(); i++) {
      if (recentResponse.get(i).rawData != null) {
        recentResponse.set(
            i, ResponseParser.parseResponseDetail(recentResponse.get(i)));
      }
    }
    // Since the responseResponse is iterated from top down check if there's any
    // unparsed Bad implementation, still a DnsSeeker.TAG here.
    for (int i = 0; i < min(5, recentResponse.size()); i++) {
      if (recentResponse.get(i).rawData != null) {
        recentResponse.set(
            i, ResponseParser.parseResponseDetail(recentResponse.get(i)));
      }
    }
    // Pass by value prevent from unparsed recent Response
    return new ArrayList<>(recentResponse);
  }

  public static List<ResponseRecord> getBlocked() {
    for (int i = 0; i < blockedResponse.size(); i++) {
      if (blockedResponse.get(i).rawData != null) {
        blockedResponse.set(
            i, ResponseParser.parseResponseDetail(blockedResponse.get(i)));
      }
    }
    return new ArrayList<>(blockedResponse);
  }

  public static List<ResponseRecord> getFailedResponse() {
    for (int i = 0; i < failedResponse.size(); i++) {
      if (failedResponse.get(i) != null &&
          failedResponse.get(i).rawData != null) {
        failedResponse.set(
            i, ResponseParser.parseResponseDetail(failedResponse.get(i)));
      }
    }
    // Since the responseResponse is iterated from top down check if there's any
    // unparsed Bad implementation, still a DnsSeeker.TAG here.
    for (int i = 0; i < min(5, failedResponse.size()); i++) {
      if (failedResponse.get(i) != null &&
          failedResponse.get(i).rawData != null) {
        failedResponse.set(
            i, ResponseParser.parseResponseDetail(failedResponse.get(i)));
      }
    }
    // Pass by value prevent from unparsed recent Response
    return new ArrayList<>(failedResponse);
  }

  public static void resetList() {
    failMut.postValue(0);
    successMut.postValue(0);
    blockedMut.postValue(0);
    blockedResponse.clear();
    recentResponse.clear();
    failedResponse.clear();
    status.resetSpeed();
  }

  public static int getTotalCount() { return successMut.getValue(); }

  public static int getFailCount() { return failMut.getValue(); }

  public static int getSuccessCount() { return successMut.getValue(); }

  public static int getBlockedCount() { return blockedMut.getValue(); }

  public ConnectionMonitor getConnectionMonitor() {
    return ConnectionMonitor.getInstance();
  }

  @Override
  public void onTerminate() {
    saveStatistics();
    super.onTerminate();
  }
}
