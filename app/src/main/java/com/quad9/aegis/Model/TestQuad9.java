package com.quad9.aegis.Model;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.net.ssl.SSLSocket;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.Question;
import de.measite.minidns.Record;

public class TestQuad9 {

    private static TestQuad9 instance = new TestQuad9();
    final static String QUAD9SERVER = "on.quad9.net";
    final static List<String> REQUIREDCIPHER = Arrays.asList(new String[]{"TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256"});
    String command;
    InetAddress destAddr;
    DatagramSocket udpSocket;
    SSLSocket dnsSocket;
    MyHandshakeListener myHandshakelistener;
    static Context context = null;
    static int CONNECT_TIMEOUT = 5000;
    private static final String TAG = "TestQuad9";

    private TestQuad9() {
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
    }
/*
    byte[] nslookup = hexStringToByteArray(
            "45000042be4c0000401195ca09cb0ab80909" +
                    "0909" +
                    "f3050035002ea8af670a0120000100000000000102696406736572766572" +
                    "00001000030000291000000000000000");
*/

    public boolean traceroute() {
        /*String format = "ping -c 1 -t %d ";
        command = String.format(format, ttl);
        Runtime.getRuntime().exec(command + url);*/
        return true;
    }

    static boolean checkAns(String response) {
        if (response.contains(QUAD9SERVER)) {
            Log.d("result", "connected");
            return true;
        } else {
            return false;
        }
    }

    public static boolean dig_over_udp(Context pContext) {
        context = pContext;
        int time = (int) System.currentTimeMillis();
        if (!forward(createOutData())) {
            return false;
        } else {
            try {
                byte[] datagramData = new byte[1024];

                DatagramPacket replyPacket = new DatagramPacket(datagramData, datagramData.length);
                getInstance().udpSocket.receive(replyPacket);
                String out = "";
                time = (int) System.currentTimeMillis() - (int) time;

                Log.d("testResult", replyPacket.getSocketAddress().toString());
                DNSMessage ans = new DNSMessage(replyPacket.getData());
                String response = ans.answerSection.get(0).name.toString();
                Log.d("result", ":" + ans.answerSection.get(0).name
                );
                if (checkAns(response)) {
                    // sendMessageToActivity(time,getDnsServer(response)+" " + "9.9.9.9",false);
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                Log.d("result", "failed");
                return false;
            }
        }
    }

    static DatagramPacket createOutData() {
        DNSMessage testDNSMessage = DNSMessage.builder().addQuestion(new Question("id.server.on.quad9.net", Record.TYPE.TXT)).build();
        try {
            getInstance().destAddr = InetAddress.getByName("9.9.9.9");
        } catch (Exception e) {
        }
        DatagramPacket outPacket = null;
        try {
            outPacket = new DatagramPacket(testDNSMessage.toArray(), 0, testDNSMessage.toArray().length, getInstance().destAddr,
                    53);
        } catch (Exception e) {

        }
        return outPacket;
    }

    static boolean forward(DatagramPacket outPacket) {

        try {
            getInstance().udpSocket = new DatagramSocket();
            getInstance().udpSocket.setSoTimeout(3000);
            getInstance().udpSocket.send(outPacket);
        } catch (IOException e) {
            Log.d("fragment_home", "failed");
            return false;
        }
        return true;


    }

    protected static void doInBackground() {
        // getInstance().dnsSocket = .connectSSL(true,null);
    }

    public static boolean dig_over_tls(Context pContext, Callback callback) {

        context = pContext;

        if (DnsSeeker.getStatus().isDebugMode()) {
            return true;
        }

        if (DnsSeeker.getStatus().isCustomServer()) {
            callback.complete(DnsSeeker.getStatus().getServerName());
            DnsSeeker.getStatus().setCurrentDst(DnsSeeker.getStatus().getServerName());
            return true;
        }
        // doInBackground();
        SSLConnector sslConnector = new SSLConnector();
        getInstance().dnsSocket = sslConnector.connectSSL(true, null);
        int time = (int) System.currentTimeMillis();
        try {
            DataOutputStream dos = new DataOutputStream(getInstance().dnsSocket.getOutputStream());
            byte[] packet = createOutData().getData();

            byte[] shortTemp = new byte[2];
            shortTemp[0] = (byte) ((packet.length >> 8) & 0xff);
            shortTemp[1] = (byte) (packet.length & 0xff);

            byte[] data = new byte[2 + packet.length];
            System.arraycopy(shortTemp, 0, data, 0, 2);
            System.arraycopy(packet, 0, data, 2, packet.length);
            //.writeShort(packet.length);
            //dos.write(packet);
            dos.write(data);
            dos.flush();
        } catch (Exception e) {
            Log.d("fragment_home", "error writing" + e);
            return false;
        }
        try {
            DataInputStream stream = new DataInputStream(getInstance().dnsSocket.getInputStream());
            int length = stream.readUnsignedShort();
            byte[] data;

            Log.d(TAG, "Reading length: " + String.valueOf(length));
            data = new byte[length];

            stream.read(data);
            time = (int) System.currentTimeMillis() - (int) time;
            DNSMessage ans = new DNSMessage(data);
            String response = ans.answerSection.get(0).getPayload().toString();

            Log.d("result", ":" + ans.answerSection.get(0).getPayload().toString());
            if (checkAns(response)) {
                boolean safeans = REQUIREDCIPHER.contains(sslConnector.getHandshakeListener().getCipherSuite());
                try {
                    callback.complete(getDnsServer(response));
                } catch (Exception e) {
                }
                // sendMessageToActivity(time, getDnsServer(response) + " "+SSLConnector.getHandshakeListener().getCipherSuite() + " " +
                //         SSLConnector.getInstance().getServerName()
                //        , safeans /* if client requires tls1.3 then (safeans)*/);
                DnsSeeker.getStatus().setCurrentDst(getDnsServer(response));
                //return true;
                getInstance().dnsSocket.close();
                return safeans;
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.d(TAG, "error reading" + e);
            return false;
        }
    }

    /*
        public void queryUdp(DnsMessage message, InetAddress address, int port){

        }*/
    public static boolean queryTls(String domain, Record.TYPE type) {
        doInBackground();

        DNSMessage message = DNSMessage.builder()
                .addQuestion(new Question(domain, type))
                .setId((new Random()).nextInt())
                .setRecursionDesired(true)
                .setOpcode(DNSMessage.OPCODE.QUERY)
                .setResponseCode(DNSMessage.RESPONSE_CODE.NO_ERROR)
                .setQrFlag(false).build();

        int time = (int) System.currentTimeMillis();
        try {
            DataOutputStream dos = new DataOutputStream(getInstance().dnsSocket.getOutputStream());
            byte[] packet = createOutData().getData();
            //dos.writeShort(packet.length);
            //dos.write(packet);
            message.writeTo(dos);
            dos.flush();

        } catch (Exception e) {

        }
        try {
            DataInputStream stream = new DataInputStream(getInstance().dnsSocket.getInputStream());
            int length = stream.readUnsignedShort();
            byte[] data;

            Log.d(TAG, "Reading length: " + String.valueOf(length));
            data = new byte[length];

            stream.read(data);
            //DNSMessage res = new DNSMessage(data);
            //Log.d("testResult", res.toString());
            String response = new String(data);
            Log.d("result", ":" + response);
            getInstance().dnsSocket.close();
            if (checkAns(response)) {
                time = (int) System.currentTimeMillis() - (int) time;
                // sendMessageToActivity(time,"",true);
            } else {

            }
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static TestQuad9 getInstance() {
        return instance;
    }

    private static String getDnsServer(String response) {
        //String[] tokens = response.split(QUAD9SERVER);
        //String[] temp =tokens[0].split("res");

        return response;
    }

    private static void sendMessageToActivity(int t, String server, boolean tls) {
        Intent intent = new Intent("TestResult");
        intent.putExtra("time", t);
        intent.putExtra("server", server);
        intent.putExtra("tls", tls);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public interface Callback {
        void complete(String r);
    }

    public TestQuad9.Callback getServerCallback = new TestQuad9.Callback() {
        public void complete(String s) {
            String currentNetwork = DnsSeeker.getInstance().getConnectionMonitor().getCurrentNetwork();
            Log.d(ConnectionMonitor.TAG, currentNetwork + " connect to: " + s);
        }
    };
}
