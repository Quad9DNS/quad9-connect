package com.quad9.aegis.Model;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.system.OsConstants;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.quad9.aegis.Analytics;
import com.quad9.aegis.MainActivity;
import com.quad9.aegis.R;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class VpnSeekerService extends VpnService implements Runnable {
    private static final String TAG = "VpnSeekerService";

    private static ParcelFileDescriptor descriptor;
    private static Thread mThread = null;

    DnsResolver mDnsResolver;
    InetAddress localServer;
    Builder builder = new Builder();
    private static final String IPV6_SUBNET = "fd66:f83a:c650::";


    public class LocalBinder extends Binder {
        public VpnSeekerService getService() {
            return VpnSeekerService.this;
        }
    }

    private final LocalBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // ConnectionMonitor.getInstance().setContext(DnsSeeker.getInstance().getApplicationContext());
    }

    private BroadcastReceiver restartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            Log.d(TAG, "reStart");
            // reconnectVpn();
            startThread();
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction() == null) {
                return START_REDELIVER_INTENT;
            }
            if (intent.getAction().equals("stopping")) {
                Analytics.INSTANCE.setCustomCrashlyticsKey("Connect status", "stopped");
                Log.d(TAG, "stop");
                if (!DnsSeeker.getStatus().shouldAutoConnect()) {
                    ConnectionMonitor.getInstance().disable();
                }
                stopThread();
                //stopSelf();
                NotificationCompat.Builder notificationBuilder;
                if (Build.VERSION.SDK_INT >= 26) {
                    String CHANNEL_ID = "quad9_alive";
                    NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                            "Quad9 Foreground",
                            NotificationManager.IMPORTANCE_DEFAULT);

                    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
                    notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
                } else {
                    notificationBuilder = new NotificationCompat.Builder(this);
                }
                Intent notificationIntent = new Intent(DnsSeeker.getInstance(), MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent notiPendingIntent = PendingIntent.getActivity(DnsSeeker.getInstance().getApplicationContext(), 0,
                        notificationIntent, PendingIntent.FLAG_IMMUTABLE);
                Notification notification = notificationBuilder.setContentTitle(DnsSeeker.getInstance().getResources().getString(R.string.noti_title_disabled))
                        .setContentText(DnsSeeker.getInstance().getResources().getString(R.string.noti_content_disabled))
                        .setContentIntent(notiPendingIntent)
                        .setSmallIcon(R.drawable.ic_favicon_disabled)
                        .build();
                NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(1, notification);
                LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                        restartReceiver);
                //return START_NOT_STICKY;
            } else if (intent.getAction().equals("start")) {
                Analytics.INSTANCE.setCustomCrashlyticsKey("Connect status", "start");
                //monitor = new ConnectionMonitor();
                DnsSeeker.getStatus().setActivated(true);
                DnsSeeker.getStatus().setConnected(true);
                ConnectionMonitor.getInstance().enable(DnsSeeker.getInstance().getApplicationContext());
                /*
                NotificationCompat.Builder notificationBuilder;
                if (Build.VERSION.SDK_INT >= 26) {
                    String CHANNEL_ID = "quad9_alive";
                    NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                            "Quad9 Foreground",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setSound(null, null);

                    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
                    notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
                }else{
                    notificationBuilder =  new NotificationCompat.Builder(this);
                }
                Intent notificationIntent = new Intent(DnsSeeker.getInstance(), MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent notiPendingIntent = PendingIntent.getActivity(DnsSeeker.getInstance().getApplicationContext(), 0,
                        notificationIntent, 0);
                Notification notification = notificationBuilder.setContentTitle(DnsSeeker.getInstance().getResources().getString(R.string.noti_title_connect))
                        .setContentIntent(notiPendingIntent)
                        .setSmallIcon(R.drawable.ic_favicon)
                        .build();
                */

                Notification notification = getNotification();
                if (Build.VERSION.SDK_INT >= 26) {
                    startForeground(1, notification);
                } else {
                    NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                    manager.notify(1, notification);
                }

                Log.d(TAG, "start");
                localServer = (InetAddress) intent.getSerializableExtra("localServer");
                LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                        restartReceiver, new IntentFilter("restartService"));
                startThread();
            }
        }

        return super.onStartCommand(intent, flags, startId); //flags -> Service.START_REDELIVER_INTENT
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        DnsSeeker.getStatus().setConnected(false);
        stopThread();
        super.onDestroy();
    }

    public static Notification getNotification() {
        NotificationCompat.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "quad9_alive";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Quad9 Foreground",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(null, null);

            ((NotificationManager) DnsSeeker.getInstance().getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                notificationBuilder = new NotificationCompat.Builder(DnsSeeker.getInstance(), CHANNEL_ID)
                        .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
            } else {
                notificationBuilder = new NotificationCompat.Builder(DnsSeeker.getInstance(), CHANNEL_ID);
            }
        } else {
            notificationBuilder = new NotificationCompat.Builder(DnsSeeker.getInstance());
        }
        Intent notificationIntent = new Intent(DnsSeeker.getInstance(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent notiPendingIntent = PendingIntent.getActivity(DnsSeeker.getInstance().getApplicationContext(), 0,
                notificationIntent, PendingIntent.FLAG_MUTABLE);
        Notification notification = notificationBuilder.setContentTitle(DnsSeeker.getInstance().getResources().getString(R.string.noti_title_connect))
                .setContentIntent(notiPendingIntent)
                .setSmallIcon(R.drawable.ic_favicon)
                .setOngoing(true)
                .build();
        return notification;
    }

    private void startThread() {
        Log.d(TAG, "startThread");
        if (mThread == null) {
            mThread = new Thread(this, "vpn");
            mThread.start();
        } else {
            mThread.interrupt();
            mThread = new Thread(this, "vpn");
            mThread.start();
        }
    }

    private void stopThread() {
        //Log.d(TAG, "stopThread");
        try {

            if (descriptor != null) {
                descriptor.close();
                descriptor = null;
            }
            if (mDnsResolver != null) {
                mDnsResolver.stop();
            }

            if (mThread != null) {
                Log.d("Thread", "stopRealThread");
                mThread.interrupt();
                mThread = null;
            }
            Log.d("Thread", "stopRealThread");
            DnsSeeker.getStatus().setConnected(false);
            if (Build.VERSION.SDK_INT >= 26) {
                stopForeground(true);
            } else {
                NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.cancelAll();
            }
        } catch (Exception e) {
            Log.d(TAG, "" + e);
        }
    }

    @Override
    public void run() {
        buildVpnService();
        try {
            descriptor = this.builder.establish();
        } catch (NullPointerException ex) {
            Analytics.INSTANCE.log("NPE in VpnSeekerService establish");
            Analytics.INSTANCE.log("Message: " + ex.getMessage());
            Analytics.INSTANCE.log("Builder state: " + this.builder);
            throw ex;
        }

        try {
            mDnsResolver = new DnsResolver(descriptor, this);
            mDnsResolver.start();
            mDnsResolver.process();
        } catch (Exception e) {
            Log.d(TAG, "" + e);
        }

    }

    // Should compare reconnectVpn vs run
    public void reconnectVpn() {
        try {
            if (descriptor != null) {
                descriptor.close();
                descriptor = null;
            }
            if (mDnsResolver != null) {
                mDnsResolver.stop();
            }

            descriptor = this.builder.establish();
            mDnsResolver = new DnsResolver(descriptor, this);
            mDnsResolver.start();
            mDnsResolver.process();
        } catch (Exception e) {
            Log.d(TAG, "" + e);
        }
    }


    public VpnService.Builder buildVpnService() {
        try {
            Set<InetAddress> dnsSet = new HashSet<InetAddress>();
            //DnsSeeker.getStatus().clearDNSSet();

            ConnectivityManager connectivityManager = (ConnectivityManager) DnsSeeker.getInstance().getSystemService(CONNECTIVITY_SERVICE);
            for (Network network : connectivityManager.getAllNetworks()) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                if (networkInfo != null) {
                    if (networkInfo.isConnected()) {
                        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                        Log.d("DnsInfo", "iface = " + linkProperties.getInterfaceName());
                        Log.d("DnsInfo", "dns = " + linkProperties.getDnsServers());
                        try {
                            DnsSeeker.getStatus().addDNSSet(linkProperties.getDnsServers());
                        } catch (NullPointerException e) {
                            Log.d("DnsInfo", "fail");
                        }
                    }
                }
            }
            Log.d("DnsInfo", "dns = " + dnsSet);
            Analytics.INSTANCE.log("VpnSeekerService: Initializing builder");
            this.builder = new VpnService.Builder()
                    .setSession("vpn")
                    .setConfigureIntent(PendingIntent.getActivity(this, 0,
                            new Intent(this, MainActivity.class),
                            PendingIntent.FLAG_IMMUTABLE))
                    .addDnsServer("10.0.0.2")
                    .addRoute("10.0.0.2", 32)
                    .addAddress(IPV6_SUBNET + "9", 120)
                    .addRoute(IPV6_SUBNET + "99", 128)
                    .addDnsServer(IPV6_SUBNET + "99")
                    .allowBypass()
                    .setBlocking(true);
            for (InetAddress addr : DnsSeeker.getStatus().getDNSSet()) {
                Log.d("DnsInfo", "dns = " + addr.getHostAddress());
                if (addr instanceof Inet4Address) {
                    this.builder.addRoute(addr.getHostAddress(), 32);
                } else if (addr instanceof Inet6Address) {
                    this.builder.addRoute(addr.getHostAddress(), 128);
                }
            }
            this.builder.allowFamily(OsConstants.AF_INET);
            this.builder.allowFamily(OsConstants.AF_INET6);
            String addressCandidate[] = {"10.9.9.9", "10.94.8.7", "192.168.6.6", "10.87.9.2", "10.111.8.8"};
            for (String s : addressCandidate) {
                try {
                    this.builder.addAddress(s, 24);
                } catch (Exception e) {
                    continue;
                }
                break;
            }

        } catch (Exception e) {
            Log.d(TAG, "" + e);
        }
        addWhiteList();
        Log.d(TAG, "DNS Set: " + DnsSeeker.getStatus().getDNSSet().toString());
        return this.builder;
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra(
                    "key");
            if (message.equals("stopping")) {
                Log.d("broadcastService", message);
                stopThread();
            } else if (message.equals("start")) {
                Log.d("broadcastService", message);
                startThread();
            }
        }
    };

    public void addWhiteList() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Set<String> selections = sharedPrefs.getStringSet("white_list", new HashSet<>(Arrays.asList("com.android.captiveportallogin", "com.android.vending")));
        if (selections.size() > 0) {
            String[] selected = selections.toArray(new String[]{});
            for (String i : selected) {
                try {
                    this.builder.addDisallowedApplication(i);
                } catch (Exception e) {
                    continue;
                    //just pass
                }
                Log.d("selectedWhiteList", i);
            }
        }
    }
}