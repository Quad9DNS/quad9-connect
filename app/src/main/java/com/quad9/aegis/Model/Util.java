package com.quad9.aegis.Model;

import android.util.Log;

import org.pcap4j.packet.IpPacket;

public interface Util {
    String TAG = "Util";

    class ReadyQuery {
        byte[] data;

    }

    class PendingQuery {
        final IpPacket packet;
        private final long time;
        boolean whitelisted = false;

        PendingQuery(IpPacket packet) {
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        boolean isWhitelisted() {
            return whitelisted;
        }

        double lastSeconds() {
            return (double) (System.currentTimeMillis() - this.time) / 1000;
        }
    }


    static void sendFailToApp(ResponseRecord r) {
        DnsSeeker.getInstance().addFail(r);
    }

    char[] hexArray = "0123456789ABCDEF".toCharArray();

    static void rejectPacket(String reason) {
        Log.d(TAG, "Discarded " + " " + reason);
    }


}
