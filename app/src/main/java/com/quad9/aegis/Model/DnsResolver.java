package com.quad9.aegis.Model;

import static java.util.Arrays.copyOfRange;

import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.UdpPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import de.measite.minidns.DNSName;
import de.measite.minidns.Record;
import de.measite.minidns.edns.EDNSOption;
import de.measite.minidns.record.OPT;

public class DnsResolver {
    static private int dnsQueryTimes = 0;
    private static final String TAG = "DnsResolver";

    final int NONETWORK = 2;
    final int CONNECTED = 1;
    final int PORTAL = 3;
    private ParcelFileDescriptor descriptor;
    private ParcelFileDescriptor TLSpfd;
    private VpnSeekerService service;
    private boolean shutdown = true;
    private double totalTime = 0;
    private int reConnectTooMuch = 0;
    private boolean splitFlag = false;
    private FileDescriptor writeEndFd = null;
    private FileDescriptor readEndFd = null;
    private Queue<byte[]> deviceWrites = new LinkedList<>();
    private Queue<ReadyQuery> tlsSocketWrites = new LinkedList<>();
    private int STATUS = 0;
    private boolean BEWRITTEN = false;
    private int ResponsePerSession = 0;
    private int whitelistCount = 0;
    private NewHashQuery whitelistDnsQ = new NewHashQuery();
    private NewHashQuery udpDnsQ = new NewHashQuery();
    private final SharedPreferences sharedPreferences;

    TlsSender mTlsSender;
    UdpSender mUdpSender;


    public DnsResolver(ParcelFileDescriptor descriptor, VpnSeekerService service) {
        this.descriptor = descriptor;
        this.service = service;
        dnsQueryTimes = 0;
        whitelistDnsQ = new NewHashQuery();
        udpDnsQ = new NewHashQuery();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(service.getApplicationContext());
    }

    public final void start() {
        shutdown = false;
        reConnectTooMuch = 0;
        mTlsSender = new TlsSender(this.service);
        mUdpSender = new UdpSender(this.service);
    }

    public void process() {
        Log.w(TAG, Thread.currentThread().toString());
        boolean firstTime = true;
        int i = 0;
        while (!shutdown) {
            // selectServer
            // Try to reach the four servers with multiThreading.
            DnsSeeker.getStatus().setReset(false);
            List<String> candidateList = ServerSelector.getCandidate(service);
            Log.i(TAG, String.format("candidateList: %s", candidateList.toString()));
            // Set IP from candidatList (priority: IPv6 > IPv4?)

            // setStatus PORTAL, CONNECTED, NEWNETWORK, NONETWORK,
            if (candidateList.isEmpty()) {

                if (DnsSeeker.getStatus().isOnline()) {
                    STATUS = PORTAL;
                    // This would work because chrome can bypass to the DHCP DNS.
                } else {
                    STATUS = NONETWORK;
                }
            } else {
                DnsSeeker.getStatus().setServerName(candidateList);
                STATUS = CONNECTED;
            }

            Log.d(TAG, "Status: " + STATUS);
            // maintain vpn
            if (STATUS == CONNECTED && !firstTime) {
                // DnsSeeker.getStatus().setPortal(false);
                // If comes in first time, don't come in since it would create a loop.
                if (DnsSeeker.getStatus().isNewNetowrk()) {
                    Log.d(TAG, "Used to restart.");
                    reConnect("New network");
                    DnsSeeker.scheduleRestart(1, 1);
                }
            } else if (STATUS == PORTAL) {
                if (DnsSeeker.getStatus().isNewNetowrk() && DnsSeeker.getStatus().isRouted()) {
                    Log.d(TAG, "Wifi issue, Used to restart.");

                    // DnsSeeker.checkWifiAvailable();
                    DnsSeeker.getStatus().setNewNetowrk(false);
                }
                //        clean up routing table, do not route DHCP DNS.
                //        schedule a timer to break the vpn loop every 30 seconds.
            } else if (STATUS == CONNECTED) {
                // Pass
            } else if (STATUS == NONETWORK) {
                Log.d(TAG, "No Network");
            }

            firstTime = false;

            try {
                Log.d(TAG, "Starting DNS over TLS.");

                FileInputStream inputStream = new FileInputStream(descriptor.getFileDescriptor());
                FileOutputStream outputStream = new FileOutputStream(descriptor.getFileDescriptor());

                byte[] packet = new byte[1024];
                while (!DnsSeeker.getStatus().isResetFlag()) {

                    StructPollfd deviceFd = new StructPollfd();
                    deviceFd.fd = inputStream.getFD();
                    deviceFd.events = (short) (GlobalVariables.POLLIN | GlobalVariables.POLLERR);

                    StructPollfd dnsFd = new StructPollfd();

                    if (mTlsSender.getSocket() == null) {
                        reConnect("dnsSocket is null");
                    }
                    if (mTlsSender.getSocket() != null) {
                        dnsFd.fd = mTlsSender.getFd();
                        // TODO if fd is not set, what will Poll react??
                    }
                    if (!deviceWrites.isEmpty()) {
                        deviceFd.events |= (short) GlobalVariables.POLLOUT;
                    }
                    if (!DnsSeeker.getStatus().getDnsQ().isEmpty() && BEWRITTEN) {
                        Log.d(TAG, "setPoll");
                        dnsFd.events = (short) (GlobalVariables.POLLIN | GlobalVariables.POLLERR);
                    }

                    if (!tlsSocketWrites.isEmpty()) {
                        dnsFd.events |= (short) GlobalVariables.POLLOUT;
                        dnsFd.events |= (short) GlobalVariables.POLLERR;
                    }

                    StructPollfd[] polls = new StructPollfd[2 + whitelistDnsQ.size() + udpDnsQ.size()];
                    Log.d(TAG, String.format("Total POLLing %d", (2 + whitelistDnsQ.size() + udpDnsQ.size())));
                    polls[0] = deviceFd;
                    polls[1] = dnsFd;
                    Set<String> keys = whitelistDnsQ.keySet();

                    {
                        int j = -1;
                        for (String query : keys) {
                            j++;
                            polls[2 + j] = new StructPollfd();
                            Log.d(TAG, query);
                            StructPollfd pollFd = polls[2 + j];
                            pollFd.fd = ParcelFileDescriptor.fromDatagramSocket(whitelistDnsQ.get(query).socket).getFileDescriptor();
                            pollFd.events = (short) OsConstants.POLLIN;
                        }
                    }
                    Set<String> udp_keys = udpDnsQ.keySet();

                    {
                        int j = -1 + whitelistDnsQ.size();
                        for (String query : udp_keys) {
                            j++;
                            polls[2 + j] = new StructPollfd();
                            Log.d(TAG, query);
                            StructPollfd pollFd = polls[2 + j];
                            pollFd.fd = ParcelFileDescriptor.fromDatagramSocket(udpDnsQ.get(query).socket).getFileDescriptor();
                            pollFd.events = (short) OsConstants.POLLIN;
                        }
                    }
                    // TODO timeout
                    Os.poll(polls, -1);
                    Log.d(TAG, "POLL: dnsFd" + dnsFd.revents);

                    //WHITELIST
                    List<String> to_delete = new ArrayList();
                    {
                        int j = 1;
                        for (String queryKey : keys) {
                            j++;
                            Log.d(TAG, queryKey);
                            if ((polls[j].revents & OsConstants.POLLIN) != 0) {
                                PendingQuery query = whitelistDnsQ.get(queryKey);
                                ResponseRecord r = null;
                                byte[] datagramData = new byte[1024];
                                if (!query.isDns) {
                                    Log.d(TAG, "Getting Non Dns Packet");
                                    DatagramPacket replyPacket = new DatagramPacket(datagramData, datagramData.length);
                                    query.socket.receive(replyPacket);
                                    queueDeviceWrite(ResponseParser.generatePacket(query.packet, datagramData));

                                    to_delete.add(queryKey);
                                    break;
                                }
                                try {
                                    DatagramPacket replyPacket = new DatagramPacket(datagramData, datagramData.length);
                                    query.socket.receive(replyPacket);
                                    r = ResponseParser.parseResponse(datagramData);
                                } catch (Exception e) {
                                    Log.d(TAG, "" + e);
                                }
                                double time = whitelistDnsQ.getDuration(queryKey);
                                // whitelistDnsQ.delete(queryKey);
                                to_delete.add(queryKey);
                                if (r != null) {
                                    queueDeviceWrite(ResponseParser.generatePacket(query.packet, datagramData));
                                    query.socket.close();
                                    r.time = (int) (time * 1000);
                                    sendResponseToApp(r);
                                }
                                totalTime += time;
                                dnsQueryTimes++;
                                break;
                            }
                        }
                    }
                    // Save Delete Key for safely deletion in iteration.
                    for (String key : to_delete) {
                        whitelistDnsQ.delete(key);
                    }

                    //WHITELIST

                    List<String> udp_to_delete = new ArrayList();
                    {
                        int j = 1 + whitelistDnsQ.size();
                        for (String queryKey : udp_keys) {
                            j++;
                            Log.d(TAG, queryKey);
                            if ((polls[j].revents & OsConstants.POLLIN) != 0) {
                                PendingQuery query = udpDnsQ.get(queryKey);
                                ResponseRecord r = null;
                                byte[] datagramData = new byte[1024];
                                try {
                                    DatagramPacket replyPacket = new DatagramPacket(datagramData, datagramData.length);
                                    query.socket.receive(replyPacket);
                                    r = ResponseParser.parseResponse(datagramData);
                                } catch (Exception e) {
                                    Log.d(TAG, "" + e);
                                }
                                double time = udpDnsQ.getDuration(queryKey);
                                // whitelistDnsQ.delete(queryKey);
                                udp_to_delete.add(queryKey);
                                if (r != null) {
                                    queueDeviceWrite(ResponseParser.generatePacket(query.packet, datagramData));
                                    query.socket.close();
                                    r.time = (int) (time * 1000);
                                    sendResponseToApp(r);
                                }
                                totalTime += time;
                                dnsQueryTimes++;
                                break;
                            }
                        }
                    }
                    for (String key : udp_to_delete) {
                        udpDnsQ.delete(key);
                    }

                    if ((dnsFd.revents & (GlobalVariables.POLLNVAL | GlobalVariables.POLLERR | GlobalVariables.POLLHUP)) != 0
                        // ((System.currentTimeMillis() - this.time) / 1000 > 10
                        //DnsSeeker.getStatus().getDnsQ().isEmpty()
                        //(tlsSocketWrites.size() != 0 && (polls[1].revents & GlobalVariables.POLLOUT) == 0)
                    ) {
                        if (DnsSeeker.getStatus().getDnsQ().isEmpty()) {
                            mTlsSender.closeSocket();
                            continue;
                        } else {
                            reConnect("Socket ERROR");
                        }
                        //reConnect("Socket ERROR");
                    }

                    //Read the socket first.
                    {
                        if (!DnsSeeker.getStatus().getDnsQ().isEmpty() && ((dnsFd.revents & GlobalVariables.POLLIN) != 0)) {
                            //READ broken，reconnect
                            Log.d(TAG, "tls polled ");
                            boolean second = false;
                            boolean reconnectFlag = false;
                            while (true) {

                                byte[] data = mTlsSender.read(!second);
                                if (data == null) {
                                    if (second) {
                                        break;
                                    } else {
                                        reConnect("Reading");
                                        reconnectFlag = true;
                                        break;
                                    }
                                } else {
                                    handleResponse(data);
                                    second = true;
                                }
                            }
                            if (reconnectFlag) {
                                continue;
                            }
                        }
                    }

                    // Flush to TLS socket
                    /*
                    while (tlsSocketWrites.size() != 0) {
                        Log.i(TAG, "dnsSocket is closed: " + mTlsSender.getSocket().isClosed());
                        ReadyQuery rq = tlsSocketWrites.poll();
                        BEWRITTEN = true;
                        mTlsSender.write(rq);
                        Log.i(TAG, "Write to socket (All)" + tlsSocketWrites.size());
                    }
                    */
                    if ((deviceFd.revents & GlobalVariables.POLLIN) != 0) {
                        packet = new byte[2048];
                        Log.d(TAG, "Read from device");
                        byte[] readPacket = readPacketFromDevice(inputStream, packet);
                        //For two queries in one PacketFromDevice
                        while (readPacket != null) {
                            Log.d(TAG, "readPacket length: " + readPacket.length);
                            readPacket = handleDnsRequest(readPacket);
                        }
                    }

                    // Flush to device

                    while (deviceWrites.size() != 0 && (deviceFd.revents & GlobalVariables.POLLOUT) != 0) {
                        writeToDevice(outputStream);
                        Log.d(TAG, "Write to device");
                    }
                    Log.d(TAG, "How many queries in queue: " + DnsSeeker.getStatus().getDnsQ().size());
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                }
                // TODO poll here?
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            } catch (Exception e) {
                reConnect("NEVER comes here");
                // TODO: Log more info

                Log.e(TAG, "Loop dead, NEVER comes here" + e.getCause());

            }
        }
    }

    public byte[] readPacketFromDevice(FileInputStream input, byte[] packet) {
        int length = 0;

        try {
            length = input.read(packet);
        } catch (IOException e) {
            Log.w(TAG, "Err reading packet!");
            return null;
        }
        if (length == 0) {
            Log.w(TAG, "Got empty packet!");
            return null;
        }
        final byte[] readPacket = copyOfRange(packet, 0, length);
        return readPacket;

    }

    private void writeToDevice(FileOutputStream output) {
        try {
            if (reConnectTooMuch > 0) {
                reConnectTooMuch--;
            }
            byte[] temp = deviceWrites.poll();
            Log.d(TAG, "Write to device " + temp.length);
            // Log.d(TAG, Util.bytesToHex(temp));
            output.write(temp);
            output.flush();
        } catch (IOException e) {
            Log.i(TAG, "VPN forced stop " + e);
            // Check VPN forced closed.
        }
    }

    // TODO!!!!!!
    private byte[] handleDnsRequest(byte[] packetData) {

        IpPacket parsedPacket = getDnsPacket(packetData);
        if (parsedPacket == null) {
            return null;
        }

        byte[] dnsRawData = null;
        try {
            dnsRawData = parsedPacket.getPayload().getPayload().getRawData();
        } catch (Exception e) {
            dnsRawData = parsedPacket.getPayload().getRawData();
            //rejectPacket("handleDnsRequest: Discarding invalid IP packet" + e);
            //return null;
        }
        int port = getDstPort(parsedPacket);
        if (port == 0) {
            return null;
        } else if (port != 53) {
            DatagramPacket outPacket = new DatagramPacket(dnsRawData, 0, dnsRawData.length, ConnectionMonitor.getInstance().getDefaultGateway(),
                    port);
            DatagramSocket dnsSocket = mUdpSender.send(outPacket, parsedPacket);
            //whitelistDnsQ.add(""+port, new PendingQuery(dnsSocket, parsedPacket,false));
            Log.d(TAG, "sending Non Dns Packet" + outPacket);
            return null;
            //return copyOfRange(packetData, parsedPacket.length(), packetData.length);
        }
        Log.d(TAG, "protocol getIdentifier: " + getIdentifier(dnsRawData));
        if (getIdentifier(dnsRawData).equals("0") || getIdentifier(dnsRawData).equals("256")) {
            rejectPacket("ICMP from device");
            return null;
        }

        if (false) {
            dnsRawData = addEdnsPayload(dnsRawData);
        }
        if (DnsSeeker.getStatus().isUsingTLS()) {
            if (ResponseParser.checkWhitelist(parsedPacket)) {

                InetAddress destAddr = ConnectionMonitor.getInstance().getDefaultDns();
                //InetAddress destAddr = null;
                //try{ destAddr =  InetAddress.getByName("192.168.2.1");}catch(Exception e){}
                Log.d(TAG, "sending whitelisted" + destAddr);
                DatagramPacket outPacket = new DatagramPacket(dnsRawData, 0, dnsRawData.length, destAddr,
                        53);

                DatagramSocket dnsSocket = mUdpSender.send(outPacket, parsedPacket);

                String hashCode = getIdentifier(outPacket.getData());
                if (whitelistDnsQ.containsKey(hashCode)) {
                    whitelistDnsQ.delete(hashCode);
                }
                whitelistDnsQ.add(hashCode, new PendingQuery(dnsSocket, parsedPacket));
            } else if (mTlsSender.send(dnsRawData, parsedPacket, getIdentifier(dnsRawData)) != null) {
                if (!mTlsSender.write(new ReadyQuery(dnsRawData, (short) dnsRawData.length))) {
                    reConnect("writing error");
                    // TODO writing error...
                } else {
                    BEWRITTEN = true;
                }
            }
        } else {
            InetAddress destAddr = null;
            if (ResponseParser.checkWhitelist(parsedPacket)) {
                destAddr = ConnectionMonitor.getInstance().getDefaultDns();
                DatagramPacket outPacket = new DatagramPacket(dnsRawData, 0, dnsRawData.length, destAddr,
                        53);
                //forwardWhitelistPacket(outPacket,parsedPacket);
                DatagramSocket dnsSocket = mUdpSender.send(outPacket, parsedPacket);
                String hashCode = getIdentifier(outPacket.getData());
                if (whitelistDnsQ.containsKey(hashCode)) {
                    whitelistDnsQ.delete(hashCode);
                }
                whitelistDnsQ.add(hashCode, new PendingQuery(dnsSocket, parsedPacket));
            } else {
                try {
                    destAddr = InetAddress.getByName(DnsSeeker.getStatus().getServerName());
                } catch (Exception e) {
                    Log.d(TAG, "getByName: " + e);
                }
                DatagramPacket outPacket = new DatagramPacket(dnsRawData, 0, dnsRawData.length, destAddr,
                        53);
                //forwardWhitelistPacket(outPacket,parsedPacket);
                DatagramSocket dnsSocket = mUdpSender.send(outPacket, parsedPacket);
                String hashCode = getIdentifier(outPacket.getData());
                if (udpDnsQ.containsKey(hashCode)) {
                    udpDnsQ.delete(hashCode);
                }
                udpDnsQ.add(hashCode, new PendingQuery(dnsSocket, parsedPacket));
            }
            //DatagramPacket outPacket = new DatagramPacket(dnsRawData, 0, dnsRawData.length, destAddr,
            //        53);
            // TODO
            // forwardWhitelistPacket(outPacket,parsedPacket);
            // mUdpSender.send(outPacket, parsedPacket);
            // whitelistDnsQ.add(hashCode, new PendingQuery(dnsSocket, parsedPacket));
        }

        if (parsedPacket.length() != packetData.length) {
            return copyOfRange(packetData, parsedPacket.length(), packetData.length);
        } else {
            return null;
        }
    }

    public void reConnect(String reason) {
        DnsSeeker.getStatus().setOnline(true);
        Calendar c = Calendar.getInstance();
        Log.i(TAG, "Reconnect due to " + reason + " with Queue: "
                + DnsSeeker.getStatus().getDnsQ().size() + " at " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND)
                + " Resolved: " + ResponsePerSession + " queries.");
        ResponsePerSession = 0;
        splitFlag = false;
        tlsSocketWrites.clear();
        mTlsSender.closeSocket();
        mTlsSender.reConnect();

        BEWRITTEN = false;
        // Get null return indicates connection problem. Empty the sending and waiting queries.
        if (mTlsSender.getSocket() == null || reConnectTooMuch == 3) {
            DnsSeeker.getStatus().setOnline(false);
            DnsSeeker.getStatus().getDnsQ().delete_all();
            tlsSocketWrites.clear();
            reConnectTooMuch = 0;
            DnsSeeker.getStatus().setReset(true);
        }
        reConnectTooMuch++;
        if (DnsSeeker.getStatus().getDnsQ().size() != 0) {
            Set<String> keys = DnsSeeker.getStatus().getDnsQ().keySet();
            for (String each : keys) {
                Log.d(TAG, "resend packet: " + each);
                reforwardPacket(DnsSeeker.getStatus().getDnsQ().get(each).packet);
            }
        }
    }

    private byte[] addEdnsPayload(byte[] dnsRawData) {
        try {
            ByteArrayInputStream dnsRawDataInput = new ByteArrayInputStream(dnsRawData);
            DataInputStream dis = new DataInputStream(dnsRawDataInput);
            dis.skipBytes(10);
            int additionalCount = dis.readUnsignedShort();
            additionalCount++;
            ByteBuffer dnsRawDataBuffer = ByteBuffer.wrap(dnsRawData);
            dnsRawDataBuffer.putShort(10, (short) additionalCount);
            List<EDNSOption> options = new ArrayList<>();
            options.add(EDNSOption.parse(
                    sharedPreferences.getInt("ednsCode", 0xFEFE),
                    sharedPreferences.getString("ednsPayload", "c-AXZcz41kyUpEsv0pmePqKkvwuJIHftlN-67joUcqo5k9rfieqwZDCW3JG8RgjhBo2zsOvaPo1v_arn8tWv8Z-6ONXp1XvQ").getBytes(StandardCharsets.UTF_8)
            ));
            Record<OPT> newRecord = new Record<>(DNSName.EMPTY, Record.TYPE.OPT, Record.CLASS.ANY, 100, new OPT(options), true);
            byte[] newRecordBytes = newRecord.toByteArray();
            ByteArrayOutputStream dnsRawDataOutput = new ByteArrayOutputStream(dnsRawData.length + newRecordBytes.length);
            dnsRawDataOutput.write(dnsRawData);
            dnsRawDataOutput.write(newRecordBytes);
            dnsRawData = dnsRawDataOutput.toByteArray();
            return dnsRawData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void reforwardPacket(IpPacket parsedPacket) {
        //InetAddress destAddr = parsedPacket.getHeader().getDstAddr();
        //UdpPacket parsedUdp = (UdpPacket) parsedPacket.getPayload();
        byte[] dnsRawData = parsedPacket.getPayload().getPayload().getRawData();
        try {
            ReadyQuery fd = new ReadyQuery(dnsRawData, (short) dnsRawData.length);
            //tlsSocketWrites.add(fd);
            BEWRITTEN = true;
            if (!mTlsSender.write(fd)) {
                reConnect("Writing Error");
            }
        } catch (Exception e) {
            Log.d(TAG, "Reforwarding" + e);
        }
    }

    private void handleResponse(byte[] data) {
        String key = getIdentifier(data);
        Log.d(TAG, "Key: " + key);
        ResponseRecord r = null;
        double time = 0;
        if (DnsSeeker.getStatus().getDnsQ().containsKey(key)) {
            r = ResponseParser.parseResponse(data);
            time = DnsSeeker.getStatus().getDnsQ().getDuration(key);
            totalTime += time;
        } else {
            Log.d(TAG, "Query already answered");
        }

        if (r != null) {
            queueDeviceWrite(ResponseParser.generatePacket(DnsSeeker.getStatus().getDnsQ().get(key).packet, data));
            r.time = (int) (time * 1000);
            sendResponseToApp(r);
            dnsQueryTimes++;
            ResponsePerSession++;
            if (r.IP.equals("MALICIOUS")) {
                sendBlockedToApp(r);
            }
        }
        DnsSeeker.getStatus().getDnsQ().delete(key);
    }

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
        boolean isDns = true;

        PendingQuery(DatagramSocket socket, IpPacket packet) {
            this.socket = socket;
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        PendingQuery(DatagramSocket socket, IpPacket packet, boolean isDns) {
            this.socket = socket;
            this.packet = packet;
            this.time = System.currentTimeMillis();
            this.isDns = isDns;
        }

        PendingQuery(IpPacket packet) {
            this.socket = null;
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }


        double lastSeconds() {
            return (double) (System.currentTimeMillis() - this.time) / 1000;
        }
    }


    void sendResponseToApp(ResponseRecord r) {
        DnsSeeker.getInstance().addResponse(r);
    }

    void sendBlockedToApp(ResponseRecord r) {
        DnsSeeker.getInstance().addBlocked(r);
    }

    static void sendFailToApp(ResponseRecord r) {
        DnsSeeker.getInstance().addFail(r);
    }

    static boolean checkQuery(byte[] packetData) {
        return true;
    }

    char[] hexArray = "0123456789ABCDEF".toCharArray();

    String bytesToHex(byte[] bytes) {
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
                Log.d(TAG, "Src port = " + parsedUdp.getHeader().getSrcPort());
                //mUdpSender.send(outPacket, parsedPacket);
                //rejectPacket("port = " + parsedUdp.getHeader().getDstPort());
            }
        } catch (Exception e) {
            rejectPacket("handleDnsRequest: Discarding invalid IP packet");
            return null;
        }

        return parsedPacket;
    }

    static int getDstPort(IpPacket parsedPacket) {
        try {
            UdpPacket parsedUdp = (UdpPacket) (parsedPacket.getPayload());
            if (parsedUdp.getHeader().getDstPort().valueAsInt() != 53) {
                Log.d(TAG, "Src port = " + parsedUdp.getHeader().getSrcPort());
                //mUdpSender.send(outPacket, parsedPacket);
                //rejectPacket("port = " + parsedUdp.getHeader().getDstPort());
                return parsedUdp.getHeader().getDstPort().valueAsInt();
            }
        } catch (Exception e) {
            rejectPacket("handleDnsRequest: Discarding invalid IP packet");
            return 0;
        }
        return 53;
    }

    static String getIdentifier(byte[] data) {
        int key;
        key = (data[0] & 0xFF) * 256 + (data[1] & 0xFF);
        return Integer.toString(key);
    }

    private void queueDeviceWrite(IpPacket ipOutPacket) {
        deviceWrites.add(ipOutPacket.getRawData());
    }

    public void stop() {
        try {
            shutdown = true;
            DnsSeeker.getStatus().setReset(true);
            if (readEndFd != null) {
                Os.close(readEndFd);
            }
            if (writeEndFd != null) {
                Os.close(writeEndFd);
            }
            if (this.descriptor != null) {
                this.descriptor.close();
                //this.descriptor = null;
            }

        } catch (Exception ignored) {

        }
    }
}

