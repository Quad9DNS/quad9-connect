package com.quad9.aegis.Model;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteConstraintException;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.RouteInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.net.InetAddress;
import java.util.List;

public class ConnectionMonitor extends ConnectivityManager.NetworkCallback {
    public static final String TAG = "ConnectionMonitor";

    static private ConnectionMonitor instance = null;
    static Context mContext;
    NetworkRequest networkRequest;
    InetAddress defaultDns = null;
    InetAddress defaultGateway = null;
    String currentNetworkName = "";


    public static ConnectionMonitor getInstance() {
        if (instance == null) {
            instance = new ConnectionMonitor(DnsSeeker.getInstance().getApplicationContext());
        }
        return instance;
    }

    public static void setInstance(ConnectionMonitor _instance) {
        instance = _instance;
    }

    public static void setContext(Context context) {
        mContext = context;
    }


    private ConnectionMonitor(Context context) {
        mContext = context;
        networkRequest = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
    }

    public InetAddress getDefaultDns() {
        return defaultDns;
    }

    public InetAddress getDefaultGateway() {
        return defaultGateway;
    }

    public String getCurrentNetwork() {
        listAllAndUpdateNetworks();
        return currentNetworkName;
    }

    public void enable(Context context) {
        mContext = context;
        try {
            setDefaultDNS();
            setGateway();

            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
            connectivityManager.registerNetworkCallback(networkRequest, this);
        } catch (Exception e) {
            Log.i(TAG, "NetworkCallback was already registered");
        }
    }

    public void disable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(this);
            } catch (Exception e) {
                // pass
            }
        }
    }

    // Likewise, you can have a disable method that simply calls ConnectivityManager#unregisterCallback(networkRequest) too.
    public boolean isOnline() {
        ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        Network[] activeNetworks = connManager.getAllNetworks();
        for (Network n : activeNetworks) {
            if (!isVPNLP(connManager.getLinkProperties(n))) {
                if (connManager.getNetworkInfo(n) == null) {
                    return false;
                } else {
                    return connManager.getNetworkCapabilities(n).hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                }
            }
        }
        return false;
        /*
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        if(connectivityManager.getLinkProperties(connectivityManager.getActiveNetwork())!=null) {
            return connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork()).hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }else{
            return false;
        }*/
    }

    public boolean isRouted() {

        // ANDROIDQ

        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= 23) {
            if (connectivityManager.getActiveNetwork() != null) {
                return DnsSeeker.getStatus().isInDNSSet(connectivityManager.getLinkProperties(connectivityManager.getActiveNetwork()).getDnsServers());
            }
        }
        return false;
    }

    private void setDefaultDNS(Network network) {
        // If LinkProperties not set, it would be a leak.
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        LinkProperties lp = connectivityManager.getLinkProperties(network);
        if (lp != null && lp.getInterfaceName() != null) {
            if (isVPNLP(lp)) {
                List<InetAddress> temp = connectivityManager.getLinkProperties(network).getDnsServers();
                Log.i(TAG, "setDefaultDNS" + temp);
                for (InetAddress l : temp) {
                    defaultDns = l;
                    return;
                }
            }
        }
    }

    // TODO CHECK if the activeNetwork is a problem.
    private void setDefaultDNS() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        Network network;
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                network = connectivityManager.getActiveNetwork();
            } else {
                network = connectivityManager.getAllNetworks()[0];
            }
            if (!connectivityManager.getLinkProperties(network).getInterfaceName().contains("tun")) {
                List<InetAddress> temp = connectivityManager.getLinkProperties(network).getDnsServers();
                Log.i(TAG, "setDefaultDNS" + temp);
                for (InetAddress l : temp) {
                    defaultDns = l;
                    return;
                }
            }
        }
    }

    private void setGateway() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        Network network;
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= 23) {
                network = connectivityManager.getActiveNetwork();
            } else {
                network = connectivityManager.getAllNetworks()[0];
            }
            LinkProperties lp = connectivityManager.getLinkProperties(network);
            if (lp != null && lp.getInterfaceName() != null) {
                if (!lp.getInterfaceName().contains("tun")) {
                    for (RouteInfo ri : lp.getRoutes()) {
                        if (!ri.getGateway().isAnyLocalAddress()) {
                            defaultGateway = ri.getGateway();
                            Log.i(TAG, "GateWay: " + ri.getGateway());
                        }
                    }
                }
            }
        }
    }

    private void setGateway(Network network) {
        ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        if (connManager != null) {

            LinkProperties linkProperties = connManager.getLinkProperties(network);
            if (linkProperties != null) {
                for (RouteInfo ri : linkProperties.getRoutes()) {
                    if (!ri.getGateway().isAnyLocalAddress()) {
                        defaultGateway = ri.getGateway();
                        Log.i(TAG, "GateWay: " + ri.getGateway());
                    }
                }
            }
        }
    }

    @Override
    public void onAvailable(Network network) {
        Log.i(TAG, "=== Monitor: available start ===");

        // Without location permission we can't do anything with WiFi, so just let it work as before
        if (
                ContextCompat.checkSelfPermission(
                        mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        ) {
            WifiManager wifiManager = getSystemService(mContext, WifiManager.class);
            boolean onWifi = false;
            if (wifiManager != null && DnsSeeker.getStatus().shouldAutoConnect()) {
                Log.i(TAG, "Autoconnect enabled, checking wifi network");
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    onWifi = true;
                    Log.i(TAG, "WiFi is not null (" + wifiInfo + ")");
                    try {
                        boolean trusted = TrustedNetworkDbHelper.getInstance(mContext).isTrustedNetwork(new TrustedNetwork(wifiInfo));
                        Log.i(TAG, "Network is trusted: " + trusted + ", but are we connected: " + DnsSeeker.getStatus().isConnected());
                        if (trusted && DnsSeeker.getStatus().isConnected()) {
                            DnsSeeker.getStatus().setOnTrustedNetwork(true);
                            DnsSeeker.deActivateService();
                            return;
                        } else if (!trusted && !DnsSeeker.getStatus().isConnected()) {
                            DnsSeeker.getStatus().setOnTrustedNetwork(false);
                            DnsSeeker.activateService();
                            return;
                        }
                    } catch (SQLiteConstraintException ex) {
                        // Ignore
                    }
                }
            }

            // We are not on WiFi, cellular is never trusted
            if (!onWifi && !DnsSeeker.getStatus().isConnected()) {
                Log.i(TAG, "We are not connected, and we are not on wifi, so lets activate");
                DnsSeeker.getStatus().setOnTrustedNetwork(false);
                DnsSeeker.activateService();
                return;
            }
        }

        DnsSeeker.getStatus().setOnline(true);
        // wait android update NET_CAPABILITY_CAPTIVE_PORTAL for a while
        // We have a mismatch here.
        setDefaultDNS(network);
        setGateway(network);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {

                    ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
                    // When VPN is connected, getActiveNetwork always return vpn (tun0), which make the api useless
                    Network[] activeNetworks = connManager.getAllNetworks();
                    for (Network n : activeNetworks) {
                        if (connManager.getNetworkInfo(n) == null) {
                            Log.d(TAG, "Monitor getNetworkInfo is null");
                        } else {
                            if (connManager.getNetworkInfo(n).isConnected()) {
                                NetworkCapabilities networkCapabilities = connManager.getNetworkCapabilities(n);
                                if (networkCapabilities == null) {
                                    Log.d(TAG, "Monitor networkCapabilities is null");
                                } else {
                                    if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)) {
                                        DnsSeeker.getStatus().setPortal(true);
                                        Log.i(TAG, "It's Portal.");
                                    } else {
                                        LinkProperties linkProperties = connManager.getLinkProperties(n);
                                        // networkRequest
                                        Log.i(TAG, "Monitor Domain: " + linkProperties.getInterfaceName() + " "
                                                + "Monitor Dns: " + linkProperties.getDnsServers());

                                        if (!DnsSeeker.getStatus().isInDNSSet(linkProperties.getDnsServers()) && !linkProperties.getInterfaceName().contains("tun")) {
                                            //if(linkProperties.getDomains() != null) {
                                            DnsSeeker.getStatus().setPortal(false);
                                            DnsSeeker.getStatus().setReset(true);
                                            DnsSeeker.getStatus().setNewNetowrk(true);

                                            if (!DnsSeeker.getStatus().isUsingTLS()) {
                                                Log.i(TAG, "Used to restart");
                                                DnsSeeker.scheduleRestart(1, 1);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Log.i(TAG, "=== Monitor: available end ===");
                } catch (Exception e) {
                    Log.e("Monitor", "" + e);
                }
            }
        }, 10 * 1000);


    }

    @Override
    public void onUnavailable() {
        DnsSeeker.getStatus().setOnline(false);
    }

    @Override
    public void onLost(Network network) {
        Log.i("Monitor", "Network Lost");
        DnsSeeker.getStatus().setOnline(false);
    }

    private void sendSignalMessageToActivity(String msg) {
        Intent intent = new Intent("ThreadAction");
        intent.putExtra("key", msg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    public void listAllAndUpdateNetworks() {
        // API 23
        Log.i(TAG, "=== Monitor: listAllAndUpdateNetworks start ===");
        ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        Network[] activeNetworks = connManager.getAllNetworks();
        currentNetworkName = "";
        Log.d("Monitor", "All network counts (including vpn): " + activeNetworks.length);
        for (Network n : activeNetworks) {
            NetworkInfo ni = connManager.getNetworkInfo(n);
            if (ni != null && ni.isConnected()) {
                NetworkCapabilities networkCapabilities = connManager.getNetworkCapabilities(n);
                if (networkCapabilities == null) {
                    Log.d("", "Monitor networkCapabilities is null");
                    break;
                }
                if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        currentNetworkName = "WIFI";
                    } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        TelephonyManager tManager = (TelephonyManager) mContext
                                .getSystemService(Context.TELEPHONY_SERVICE);
                        if (currentNetworkName.equals("")) {
                            currentNetworkName = tManager.getNetworkOperatorName();
                        }
                    }
                }
                LinkProperties linkProperties = connManager.getLinkProperties(n);
                Log.i(TAG, "Monitor Domain: " + linkProperties.getInterfaceName() + " "
                        + "Monitor Dns: " + linkProperties.getDnsServers());
            }
        }
        Log.i(TAG, "=== Monitor: listAllAndUpdateNetworks end ===");
    }

    public boolean isPrivateDnsActive() {
        boolean isPrivateDnsActive = false;
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                // mContext ins not guaranteed safe...
                ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
                Network[] activeNetworks = connManager.getAllNetworks();
                for (Network n : activeNetworks) {
                    Log.i(TAG, connManager.getLinkProperties(n).getInterfaceName() + " isPrivateDnsActive: " + connManager.getLinkProperties(n).isPrivateDnsActive());
                    if (connManager.getLinkProperties(n).isPrivateDnsActive()) {
                        isPrivateDnsActive = true;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "" + e);
            }
        }
        return isPrivateDnsActive;
    }

    public boolean isVPNLP(LinkProperties lp) {
        return lp.getInterfaceName().contains("tun") || lp.getInterfaceName().contains("ipsec");
    }
}