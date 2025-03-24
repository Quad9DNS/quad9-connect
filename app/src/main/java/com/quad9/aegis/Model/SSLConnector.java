package com.quad9.aegis.Model;

import static org.conscrypt.Conscrypt.newProvider;

import android.net.VpnService;
import android.os.Build;
import android.os.PatternMatcher;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && DnsSeeker.getStatus().isCustomServer()) {
                SSLParameters sslParameters = new SSLParameters();
                sslParameters.setServerNames(Collections.singletonList(new SNIHostName(host)));
                dnsSocket.setSSLParameters(sslParameters);
            }
            dnsSocket.setSSLParameters(new SSLParameters());
            //factory.setUseSessionTickets(this.dnsSocket , true);
            //this.dnsSocket = (SSLSocket) factory.createSocket();
            //Conscrypt.setUseSessionTickets(this.dnsSocket,true);

            dnsSocket.setEnabledProtocols(new String[]{"TLSv1.3"});
            //System.setProperty("com.sun.net.ssl.checkRevocation", "true");
            //Security.setProperty("ocsp.enable", "true");


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


            //this.dnsSocket.setEnabledCipherSuites(factory.getSupportedCipherSuites());
            //Log.d(TAG,"length = "+ dnsSocket.getEnabledCipherSuites().length);
            /*
            for (int i=0; i<dnsSocket.getEnabledCipherSuites().length; i++ ) {
                Log.d("Protocol: " + dnsSocket.getEnabledCipherSuites()[i]);
            }*/
            Log.d(TAG, "Registering a handshake listener...");
            handshakeListener = new MyHandshakeListener();
            dnsSocket.addHandshakeCompletedListener(handshakeListener);

            Log.d(TAG, "Starting handshaking...");
            dnsSocket.startHandshake();

            //Log.d("SSLCERT",dnsSocket.getSession().getPeerCertificateChain()[0].getSubjectDN().getName());
            //Log.d("SSLCERT",dnsSocket.getSession().getPeerCertificateChain()[0].getIssuerDN().getName());
            Log.d(TAG, "Just connected to " + dnsSocket.getRemoteSocketAddress());
            if (Build.VERSION.SDK_INT >= 24) {
                CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
                CertPathValidator validator = CertPathValidator.getInstance("PKIX");

                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                ByteArrayInputStream bais = new ByteArrayInputStream(dnsSocket.getSession().getPeerCertificates()[1].getEncoded());
                X509Certificate x509 = (X509Certificate) cf.generateCertificate(bais);

                // Log.d("getPeerCertificates[1]",dnsSocket.getSession().getPeerCertificates()[0].toString());

                TrustAnchor anchor = new TrustAnchor(x509, null);
                Set anchors = Collections.singleton(anchor);
                PKIXParameters params = new PKIXParameters(anchors);
                // Activate certificate revocation checking
                params.setRevocationEnabled(false);
                System.setProperty("com.sun.net.ssl.checkRevocation", "true");
                Security.setProperty("ocsp.enable", "true");

                List list = Arrays.asList(new Certificate[]{dnsSocket.getSession().getPeerCertificates()[0]});
                CertPath path = cf.generateCertPath(list);
                PKIXRevocationChecker pkix = (PKIXRevocationChecker) cpb.getRevocationChecker();

                PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) validator
                        .validate(path, params);
                // Log.d("Revocation check",result.toString());
            }
            /*StrictHostnameVerifier verifier = new StrictHostnameVerifier();

            if (!verifier.verify(dnsSocket.getSession().getPeerHost(), dnsSocket.getSession())) {
                Log.d("SSLCERTVerified failed",dnsSocket.getSession().getPeerHost());
            }*/

            if (!DnsSeeker.getStatus().isDebugMode()) {
                if (DnsSeeker.getStatus().isCustomServer()) {
                    validateCertificates(dnsSocket, host, DnsSeeker.getStatus().getCustomServerIp());
                } else {
                    if (!dnsSocket.getSession().getPeerCertificateChain()[0].getSubjectDN().getName().contains("quad9.net")) {
                        throw new Exception("Hostname error");
                    }
                }
            }

            dnsSocket.setTcpNoDelay(true);
        } catch (Exception e) {
            Log.e(TAG, "connection failed with " + e);
            return null;
        }
        return dnsSocket;
    }

//    public class CreateSocketThread extends Thread {
//        public void run() {
//            try
//            {
//                _buildSocket()
//
//                final String readingFeed = receiveData(listener);
//                if (readingFeed != null){
//                    //execute next code on main Thread (UI update, etc)
//                    runOnUiThread(new Runnable() {
//                        public void run()
//                        {
//                            //do something with the data received
//                        }
//                    });
//                }
//            }
//            catch(Exception ex)
//            {
//                //showToast("Receiving Error: " + ex.toString());
//                //continue = false;
//            }
//        }
//    }

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && DnsSeeker.getStatus().isCustomServer()) {
                SSLParameters sslParameters = new SSLParameters();
                sslParameters.setServerNames(Collections.singletonList(new SNIHostName(host)));
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

            if (!DnsSeeker.getStatus().isDebugMode()) {
                if (DnsSeeker.getStatus().isCustomServer()) {
                    validateCertificates(dnsSocket, host, DnsSeeker.getStatus().getCustomServerIp());
                } else {
                    if (!dnsSocket.getSession().getPeerCertificateChain()[0].getSubjectDN().getName().contains("quad9.net")) {
                        throw new Exception("Hostname error");
                    }
                }
            }

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

    private static void validateCertificates(SSLSocket dnsSocket, String expectedHostName, String expectedIp) throws Exception {
        if (!expectedHostName.equals(expectedIp) && !dnsSocket.getSession().getPeerHost().contains(expectedHostName)) {
            throw new Exception("Hostname error. Expected " + expectedHostName + ", but found " + dnsSocket.getSession().getPeerHost());
        }

        X509Certificate cert = (X509Certificate) dnsSocket.getSession().getPeerCertificates()[0];
        Collection<List<?>> alternativeNames = cert.getSubjectAlternativeNames();
        if (alternativeNames == null) {
            throw new Exception("Missing alt names in certificate.");
        }
        List<String> altDnsNames = new ArrayList<>();
        List<String> altIps = new ArrayList<>();
        for (List<?> altName : alternativeNames) {
            if (((int) altName.get(0) == 2)) {
                altDnsNames.add((String) altName.get(1));
            }
            if (((int) altName.get(0) == 7)) {
                altIps.add((String) altName.get(1));
            }
        }
        if (!expectedHostName.equals(expectedIp)) {
            boolean matchFound = false;
            for (String altName : altDnsNames) {
                if (new PatternMatcher(altName.replace("*", ".*"), PatternMatcher.PATTERN_SIMPLE_GLOB).match(expectedHostName)) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                throw new Exception("Certificate DNS hostname error. Expected " + expectedHostName + ", but found " + altDnsNames);
            }
        }
        if (!altIps.isEmpty() && !altIps.contains(expectedIp)) {
            throw new Exception("Certificate IP error. Expected " + expectedIp + ", but found " + altIps);
        }
    }

    MyHandshakeListener getHandshakeListener() {
        return handshakeListener;
    }
}
