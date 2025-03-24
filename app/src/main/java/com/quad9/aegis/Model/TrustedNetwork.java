package com.quad9.aegis.Model;

import android.net.wifi.WifiInfo;

public class TrustedNetwork {
    private final String bssid;
    private final String ssid;

    public TrustedNetwork(String bssid, String ssid) {
        this.bssid = bssid;
        this.ssid = ssid;
    }

    public TrustedNetwork(WifiInfo wifiInfo) {
        this.bssid = wifiInfo.getBSSID();
        this.ssid = wifiInfo.getSSID();
    }

    public String getBssid() {
        return bssid;
    }

    public String getSsid() {
        return ssid;
    }
}
