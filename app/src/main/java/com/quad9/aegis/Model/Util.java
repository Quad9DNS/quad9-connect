package com.quad9.aegis.Model;

import android.util.Log;

import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.UdpPacket;

import java.net.DatagramSocket;

public interface Util {
    String TAG = "Util";

    class ReadyQuery {
        byte[] data;

        ReadyQuery(byte[] rawData, short l) {

            byte[] shortTemp = new byte[2];
            shortTemp[0] = (byte) ((l >> 8) & 0xff);
            shortTemp[1] = (byte) (l & 0xff);

            this.data = new byte[2 + rawData.length];
            System.arraycopy(shortTemp, 0, this.data, 0, 2);
            System.arraycopy(rawData, 0, this.data, 2, rawData.length);
        }
    }

    class PendingQuery {
        final DatagramSocket socket;
        final IpPacket packet;
        private long time;
        boolean whitelisted = false;
        boolean isDns;

        PendingQuery(DatagramSocket socket, IpPacket packet) {
            this.socket = socket;
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        PendingQuery(IpPacket packet) {
            this.socket = null;
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        void setWhitelisted() {
            whitelisted = true;
        }

        boolean isWhitelisted() {
            return whitelisted;
        }

        double lastSeconds() {
            return (double) (System.currentTimeMillis() - this.time) / 1000;
        }
    }


    default void sendResponseToApp(ResponseRecord r) {
        DnsSeeker.getInstance().addResponse(r);
        System.out.print("inging");
    }

    default void sendBlockedToApp(ResponseRecord r) {
        DnsSeeker.getInstance().addBlocked(r);
    }

    static void sendFailToApp(ResponseRecord r) {
        DnsSeeker.getInstance().addFail(r);
    }

    static boolean checkQuery(byte[] packetData) {
        return true;
    }

    char[] hexArray = "0123456789ABCDEF".toCharArray();

    static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    static void rejectPacket(String reason) {
        Log.d(TAG, "Discarded " + " " + reason);
    }


    static IpPacket getDnsPacket(byte[] packetData) {
        IpPacket parsedPacket;

        try {
            parsedPacket = (IpPacket) IpSelector.newPacket(packetData, 0, packetData.length);
        } catch (Exception e) {
            rejectPacket("handleDnsRequest: Discarding invalid IP packet");
            return null;
        }
        if (!parsedPacket.getHeader().getProtocol().valueAsString().equals("17")) {
            rejectPacket(parsedPacket.getHeader().getProtocol() + " from device");
            return null;
        }
        try {
            UdpPacket parsedUdp = (UdpPacket) (parsedPacket.getPayload());
            if (parsedUdp.getHeader().getDstPort().valueAsInt() != 53) {
                rejectPacket("port = " + parsedUdp.getHeader().getDstPort());
                return null;
            }
        } catch (Exception e) {
            rejectPacket("handleDnsRequest: Discarding invalid IP packet");
        }
        return parsedPacket;
    }

    static String getIdentifier(byte[] data) {
        int key;
        key = (data[0] & 0xFF) * 256 + (data[1] & 0xFF);
        return Integer.toString(key);
    }
}
