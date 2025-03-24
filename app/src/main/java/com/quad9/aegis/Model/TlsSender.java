package com.quad9.aegis.Model;

import static java.util.Arrays.copyOfRange;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.quad9.aegis.Model.DnsResolver.ReadyQuery;

import org.pcap4j.packet.IpPacket;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.LinkedList;

import javax.net.ssl.SSLSocket;

public class TlsSender {
    private SSLSocket dnsSocket = null;
    private ParcelFileDescriptor pfd = null;
    private FileDescriptor fd = null;
    private static final String TAG = "TlsSender";

    byte[] allData = new byte[2048];
    int allLength = 0;
    private VpnSeekerService service;
    private SSLConnector sslConnector;

    public TlsSender(VpnSeekerService s) {
        this.service = s;
        this.sslConnector = new SSLConnector();
    }

    public SSLSocket getSocket() {
        return this.dnsSocket;
    }

    public FileDescriptor getFd() {
        return this.fd;
    }

    public void closeSocket() {
        try {
            this.dnsSocket.close();
            this.dnsSocket = null;
            this.pfd.close();
            this.pfd = ParcelFileDescriptor.fromSocket(this.dnsSocket);
            this.fd = this.pfd.getFileDescriptor();
        } catch (Exception e) {
        }
    }

    public void reConnect() {
        try {
            this.dnsSocket.close();
        } catch (Exception e) {
            Log.e(TAG, "dnsSocket closed: " + e);
        }
        this.dnsSocket = this.sslConnector.connectSSL(false, this.service);
        if (this.dnsSocket != null) {
            this.pfd = ParcelFileDescriptor.fromSocket(this.dnsSocket);
            this.fd = this.pfd.getFileDescriptor();
        }
    }

    public byte[] read(boolean firstTime) {
        int queriesCount = 0;
        try {
            Log.d(TAG, "Reading blocked");
            if (firstTime) {
                allData = new byte[2048];
                allLength = dnsSocket.getInputStream().read(allData);
            }
            Log.d(TAG, "All length: " + allLength);
            if (allLength <= 0) {
                return null;
            }
            System.out.print(" allLength " + allLength);
            LinkedList<ReadyQuery> responselist = new LinkedList<ReadyQuery>();
            String key = "";
            byte[] data;
            byte[] temp;
            int length = 0;
            int bufferReadLength = 0;

            if (allLength > 0) {
                temp = copyOfRange(allData, 0, 2);
                short shortVal = (short) (((temp[0] & 0xFF) << 8) | (temp[1] & 0xFF));
                length = shortVal >= 0 ? shortVal : 0x10000 + shortVal;
            }
            DnsSeeker.getStatus().increTraffic(GlobalVariables.RECEIVED);

            // What if length is split.
            if (length != 0) {
                queriesCount++;
                //data = new byte[length];
                data = copyOfRange(allData, 2, length + 2);
                allData = copyOfRange(allData, length + 2, allLength);
                allLength = allLength - length - 2;
                return data;
            }
            Log.d(TAG, "Length: " + length);

        } catch (EOFException e) {
            Log.d(TAG, "Break by EOF");
            // old code, should delete.
            System.out.print("error response" + e);
            return null;

        } catch (IOException e) {
            Log.d(TAG, "Queries in one Packet: " + queriesCount);
            Log.d(TAG, "Break by IOE");
            System.out.print("error response" + e);
            return null;
        } catch (Exception e) {
            Log.d(TAG, "Exception");
            System.out.print("error response" + e);
            return null;
        }
        return null;
    }

    public boolean write(ReadyQuery rq) {

        if (rq == null) {
            return true;
        }
        try (DataOutputStream dos = new DataOutputStream(dnsSocket.getOutputStream())) {
            dos.write(rq.data);
            dos.flush();
            dos.close();
            DnsSeeker.getStatus().increTraffic(GlobalVariables.SENT);
        } catch (Exception e) {
            Log.d(TAG, "Writing error: " + e);
            // TODO What to do with this
            return false;
        }
        return true;
    }

    public byte[] send(byte[] outPacket, IpPacket parsedPacket, String hashCode) {
        try {
            if (parsedPacket != null) {
                if (DnsSeeker.getStatus().getDnsQ().containsKey(hashCode)) {
                    DnsSeeker.getStatus().getDnsQ().delete(hashCode);
                }
                DnsSeeker.getStatus().getDnsQ().add(hashCode, new Util.PendingQuery(parsedPacket));
                Log.d(TAG, "output length: " + outPacket.length);
                return outPacket;
            }
            //writeToSocket();
        } catch (Exception e) {
            Log.d(TAG, "did not add into DnsQueue key = " + hashCode);
        }
        return null;
    }

}
