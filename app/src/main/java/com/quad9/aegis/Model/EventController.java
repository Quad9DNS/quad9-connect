package com.quad9.aegis.Model;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class EventController {
    static String Channel_ID = "CHANNEL_ID";
    static String Channel_HIGH = "CHANNEL_HIGH";
    EventController m = new EventController();
    static NotificationManager NotiManager;
    static NotificationManager highNotiManager;
    static Notification.Builder noti;
    static Notification.Builder highNoti;


    private EventController() {
    }

    static public NotificationManager getNotiManager() {
        if (NotiManager == null) {
            return buildManger();
        } else {
            return NotiManager;
        }
    }

    static public NotificationManager getHighNotiManager() {
        if (highNotiManager == null) {
            highNotiManager = (NotificationManager) DnsSeeker.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(Channel_HIGH,
                        "BLock Events",
                        NotificationManager.IMPORTANCE_HIGH);
                highNotiManager.createNotificationChannel(channel);
            }
        }
        return highNotiManager;
    }

    static public Notification.Builder getHighNotiBuilder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(DnsSeeker.getInstance(), Channel_HIGH);
        } else {
            return new Notification.Builder(DnsSeeker.getInstance());
        }
    }

    static private NotificationManager buildManger() {
        NotiManager = (NotificationManager) DnsSeeker.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Channel_ID,
                    "Network Event",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotiManager.createNotificationChannel(channel);
            noti = new Notification.Builder(DnsSeeker.getInstance(), Channel_ID);
        } else {
            noti = new Notification.Builder(DnsSeeker.getInstance());
        }
        return NotiManager;
    }

}
