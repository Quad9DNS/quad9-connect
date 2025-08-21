package com.quad9.aegis.Model;

import static org.conscrypt.Conscrypt.newProvider;

import android.net.VpnService;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.Security;
import java.util.Collections;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SSLConnector {
    public MyHandshakeListener handshakeListener = null;
    private static final String TAG = "SSLConnector";

    // For test
    public SSLConnector() {
        System.setProperty("com.sun.net.ssl.checkRevocation", "true");
        Security.setProperty("ocsp.enable", "true");
    }

    public SSLSocket connectSSL(boolean firstTime, VpnService service) {
        Log.w(TAG, Thread.currentThread().toString());
        // The time out has problem when testing.
        int timeout = 2000;

        if (firstTime) {
            timeout = 1000;
        }

        SSLSocket dnsSocket = buildSocket(service, timeout);

        if (dnsSocket == null) {

            DnsSeeker.getStatus().changeServer();
            dnsSocket = buildSocket(service, timeout);
        }

        if (dnsSocket == null && DnsSeeker.status.isUsingIpv6()) {
            DnsSeeker.status.setUsingIpv6(false);
            dnsSocket = buildSocket(service, timeout);
        }
        if (dnsSocket != null) {
            DnsSeeker.getStatus().increTraffic(GlobalVariables.CONNECTION);
        }

/*
        if(firstTime && dnsSocket == null){
            Log.w(TAG,"connect failed");
            try {
                dnsSocket.close();
            }catch (Exception a){

            }
            if(DnsSeeker.status.isUsingIpv6()){
                DnsSeeker.status.setUsingIpv6(false);
                dnsSocket = buildSocket(service,timeout);
            }

        }
*/

        return dnsSocket;
    }

    public String getServerName() {
        return DnsSeeker.getStatus().getServerName();
    }

    //    private SSLSocket buildSocket(VpnService service, int timeout) {
//        CreateSocketThread readThread = new CreateSocketThread();
//        readThread.start();
//    }
    private SSLSocket buildSocket(VpnService service, int timeout) {
        SSLContext sc = null;
        SSLSocketFactory factory = null;
        SSLSocket dnsSocket = null;
        String host = getServerName();
        int port = 853;

        try {
            Log.d(TAG, "Locating socket factory for SSL...");
            if (sc == null) {
                sc = SSLContext.getInstance("TLSv1.3", newProvider());
                sc.init(null, null, null);
                //factory =(SSLCertificateSocketFactory)SSLCertificateSocketFactory.getDefault(5000, sslSessionCache);
                factory = sc.getSocketFactory();
            }
            Log.d(TAG, "Provider " + sc.getProvider());

            Log.d(TAG, "Creating secure socket to " + host + ":" + port);
            dnsSocket = (SSLSocket) factory.createSocket();
            dnsSocket.setReuseAddress(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SSLParameters sslParameters = new SSLParameters();
                if (DnsSeeker.getStatus().isCustomServer()) {
                    sslParameters.setServerNames(Collections.singletonList(new SNIHostName(host)));
                }
                sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
                dnsSocket.setSSLParameters(sslParameters);
            }
            dnsSocket.setEnabledProtocols(new String[]{"TLSv1.3"});


            if (service != null) {
                dnsSocket.bind(null);
                service.protect(dnsSocket);
            }
            if (DnsSeeker.getStatus().isCustomServer()) {
                dnsSocket.connect(new InetSocketAddress(DnsSeeker.getStatus().getCustomServerIp(), port), timeout);
            } else {
                dnsSocket.connect(new InetSocketAddress(host, port), timeout);
            }
            Log.d(TAG, "Enabling all available cipher suites...");
            //For tls v1.3, setEnabledCipherSuites is useless

            if (true) {
                String[] suites = {"TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256"};
                dnsSocket.setEnabledCipherSuites(suites);
            } else {
                String[] suites = {"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"};//,"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"};
                dnsSocket.setEnabledCipherSuites(suites);
            }

            Log.d(TAG, "Registering a handshake listener...");
            handshakeListener = new MyHandshakeListener();
            dnsSocket.addHandshakeCompletedListener(handshakeListener);

            Log.d(TAG, "Starting handshaking...");
            dnsSocket.startHandshake();

            Log.d(TAG, "Just connected to " + dnsSocket.getRemoteSocketAddress());

            dnsSocket.setTcpNoDelay(true);
        } catch (Exception e) {
            Log.e(TAG, "connection failed with " + e);
            return null;
        }
        return dnsSocket;
    }

    public static boolean testSocket(String host, VpnService service) {
        SSLContext sc = null;
        SSLSocketFactory factory = null;
        SSLSocket dnsSocket = null;
        int port = 853;

        try {
            Log.d(TAG, "Locating socket factory for SSL...");
            if (sc == null) {

                sc = SSLContext.getInstance("TLSv1.3", newProvider());
                sc.init(null, null, null);
                factory = sc.getSocketFactory();
            }

            Log.d(TAG, "Creating secure socket to " + host + ":" + port);
            //dnsSocket.setSoTimeout(timeout);
            dnsSocket = (SSLSocket) factory.createSocket();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SSLParameters sslParameters = new SSLParameters();
                if (DnsSeeker.getStatus().isCustomServer()) {
                    sslParameters.setServerNames(Collections.singletonList(new SNIHostName(host)));
                }
                sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
                dnsSocket.setSSLParameters(sslParameters);
            }
            dnsSocket.setEnabledProtocols(new String[]{"TLSv1.3"});


            if (service != null) {
                dnsSocket.bind(null);
                service.protect(dnsSocket);
            }

            if (DnsSeeker.getStatus().isCustomServer()) {
                dnsSocket.connect(new InetSocketAddress(DnsSeeker.getStatus().getCustomServerIp(), port), 900);
            } else {
                dnsSocket.connect(new InetSocketAddress(host, port), 900);
            }
            Log.d(TAG, "Enabling all available cipher suites...");
            //For tls v1.3, setEnabledCipherSuites is useless

            String[] suites = {"TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256"};
            dnsSocket.setEnabledCipherSuites(suites);
            Log.d(TAG, "Registering a handshake listener...");
            // MyHandshakeListener handshakeListener = new MyHandshakeListener();
            // dnsSocket.addHandshakeCompletedListener(handshakeListener);

            Log.d(TAG, "Starting handshaking...");
            dnsSocket.startHandshake();
            Log.d(TAG, "Just connected to " + dnsSocket.getRemoteSocketAddress());

            dnsSocket.setTcpNoDelay(true);
            dnsSocket.close();
        } catch (Exception e) {
            Log.d("Test socket failed: ", "" + e);
            if (dnsSocket != null) {
                try {
                    dnsSocket.close();
                } catch (IOException ex) {
                    Log.d("Closing test socket failed: ", "" + ex);
                }
            }
            return false;
        }
        return true;
    }

    MyHandshakeListener getHandshakeListener() {
        return handshakeListener;
    }
}
