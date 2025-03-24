package com.quad9.aegis.Model;

import android.util.Log;

import org.pcap4j.packet.DnsPacket;
import org.pcap4j.packet.DnsQuestion;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.Record;

public class ResponseParser {
    private static final String TAG = "ResponseParser";

    //private static ResponseParser intance = new ResponseParser();
    static ResponseRecord r = null;
    static IpPacket ipOutPacket = null;

    ResponseParser() {

    }

    public static ResponseRecord parseQuestion(IpPacket requestPacket, String reason) {
        try {
            //Log.d(TAG,requestPacket.getPayload().toString());
            DnsPacket dns = (DnsPacket) requestPacket.getPayload().get(DnsPacket.class);
            // DNSMessage dns = new DNSMessage(requestPacket.getPayload().getRawData());
            List<DnsQuestion> listQuestion = dns.getHeader().getQuestions();
            String type = "";
            String name = "";
            int time = 0;
            type = listQuestion.get(0).getQType().name();
            name = listQuestion.get(0).getQName().toString();

            if (reason.equals("TIMEOUT")) {
                if (DnsSeeker.getStatus().isOnline()) {
                    time = 5000;
                } else {
                    reason = "No Network Available";
                    time = 0;
                }
            }
            r = new ResponseRecord(
                    (short) 0,
                    type,
                    name,
                    reason,
                    "Unreachable",
                    time,
                    new SimpleDateFormat("MM/dd HH:mm", Locale.US).format(new Date()),
                    null
            );

        } catch (Exception e) {
            Log.i(TAG, "" + e);
        }
        return r;
    }

    public static ResponseRecord parseResponse(byte[] responsePayload) {
        //Boolean recursionAvailable = ((responsePayload[3]>> 7) & 1)==1;
        byte Rcode = responsePayload[3];
        ResponseRecord r = null;
        Boolean blocked = false;
        String IP = "";
        if (Rcode == (byte) 0x03) {
            IP = "MALICIOUS";
        }

        r = new ResponseRecord((short) 0, "", "", IP, "", 0,
                new SimpleDateFormat("MM/dd HH:mm", Locale.US).format(new Date()), responsePayload);
        return r;
    }

    public static IpPacket generatePacket(IpPacket requestPacket, byte[] responsePayload) {
        UdpPacket udpOutPacket = (UdpPacket) requestPacket.getPayload();
        UdpPacket.Builder payLoadBuilder = new UdpPacket.Builder(udpOutPacket)
                .srcPort(udpOutPacket.getHeader().getDstPort())
                .dstPort(udpOutPacket.getHeader().getSrcPort())
                .srcAddr(requestPacket.getHeader().getDstAddr())
                .dstAddr(requestPacket.getHeader().getSrcAddr())
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .payloadBuilder(
                        new UnknownPacket.Builder()
                                .rawData(responsePayload)
                );

        if (requestPacket instanceof IpV4Packet) {
            ipOutPacket = new IpV4Packet.Builder((IpV4Packet) requestPacket)
                    .srcAddr((Inet4Address) requestPacket.getHeader().getDstAddr())
                    .dstAddr((Inet4Address) requestPacket.getHeader().getSrcAddr())
                    .correctChecksumAtBuild(true)
                    .correctLengthAtBuild(true)
                    .payloadBuilder(payLoadBuilder)
                    .build();

        } else {
            ipOutPacket = new IpV6Packet.Builder((IpV6Packet) requestPacket)
                    .srcAddr((Inet6Address) requestPacket.getHeader().getDstAddr())
                    .dstAddr((Inet6Address) requestPacket.getHeader().getSrcAddr())
                    .correctLengthAtBuild(true)
                    .payloadBuilder(payLoadBuilder)
                    .build();
        }
        return ipOutPacket;
    }

    public static String getDNSString(ResponseRecord r) {
        try {
            DNSMessage res = new DNSMessage(r.rawData);
            return res.toString();
        } catch (Exception e) {

        }
        return "";
    }

    public static ResponseRecord parseResponseDetail(ResponseRecord r) {
        try {
            DNSMessage resMessage = new DNSMessage(r.rawData);

            String type = "";
            String IP = "";
            if (r.name.equals("")) {
                if (resMessage.answerSection.size() > 0) {
                    for (Record ans : resMessage.answerSection) {
                        type = ans.type.toString();
                        IP = ans.getPayload().toString();
                        if (ans.type == Record.TYPE.A || ans.type == Record.TYPE.AAAA) {
                            break;
                        }
                    }
                } else {
                    IP = "NXDOMAIN";
                    type = resMessage.questions.get(0).type.toString();
                }
                if (resMessage.responseCode == DNSMessage.RESPONSE_CODE.NX_DOMAIN && !resMessage.recursionAvailable) {
                    // Log.d(TAG, "BLOCKED!!!");
                    IP = "MALICIOUS";
                    if (DnsSeeker.getStatus().isUsingEnhanced()) {
                        r.setProviders();
                    }
                }
                r.type = type;
                r.name = resMessage.questions.get(0).name.toString();
                r.IP = IP;
                r.resolver = "9.9.9.9";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response record" + e);
            return null;
        }
        return r;
    }

    public static boolean checkWhitelist(IpPacket requestPacket) {
        try {
            DnsPacket dns = (DnsPacket) requestPacket.getPayload().get(DnsPacket.class);
            List<DnsQuestion> listQuestion = dns.getHeader().getQuestions();
            String name = "";
            name = listQuestion.get(0).getQName().toString();
            return DnsSeeker.getStatus().isInWhitelistDomain(name);
        } catch (Exception e) {
            Log.i(TAG, "Error when checking white list " + e);
            return false;
        }
    }
}
