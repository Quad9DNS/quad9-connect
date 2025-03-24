package com.quad9.aegis.Model;

import android.util.Log;

import org.pcap4j.packet.IpPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UdpSender {
    private VpnSeekerService service;
    private static final String TAG = "UdpSender";

    UdpSender(VpnSeekerService s) {
        this.service = s;
    }

    public DatagramSocket send(DatagramPacket outPacket, IpPacket parsedPacket) {
        DatagramSocket dnsSocket;
        try {
            // Packets to be sent to the real DNS server will need to be protected from the VPN
            Log.d(TAG, "sending packet...");

            dnsSocket = new DatagramSocket();
            service.protect(dnsSocket);
            // dnsSocket.connect(new InetSocketAddress("2620:fe::fe",53));

            dnsSocket.send(outPacket);
        } catch (IOException e) {
            Log.d(TAG, "error sending packet" + e);
            return null;
        }
        return dnsSocket;
    }
}
