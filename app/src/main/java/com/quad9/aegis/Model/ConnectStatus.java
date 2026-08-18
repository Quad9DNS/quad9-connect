package com.quad9.aegis.Model;

import android.content.SharedPreferences;
import android.net.VpnService;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ConnectStatus {
    private static final String TAG = "ConnectStatus";

    private static final Set<String> STATIC_WHITELIST_DOMAINS = new HashSet<String>() {
        {
            add("router.asus.com");
            add("orbilogin.com");
            add("orbilogin.net");
            add("routerlogin.net");
            add("_hotspot_.m2m");
            add("wpad");
            add("local");
            add("localhost");
            add("invalid");
            add("onion");
            add("test");
            add("lan");
            add("intranet");
            add("internal");
            add("private");
            add("home");
            add("corp");
            add("10.IN-ADDR.ARPA");
            add("16.172.IN-ADDR.ARPA");
            add("17.172.IN-ADDR.ARPA");
            add("18.172.IN-ADDR.ARPA");
            add("19.172.IN-ADDR.ARPA");
            add("20.172.IN-ADDR.ARPA");
            add("21.172.IN-ADDR.ARPA");
            add("22.172.IN-ADDR.ARPA");
            add("23.172.IN-ADDR.ARPA");
            add("24.172.IN-ADDR.ARPA");
            add("25.172.IN-ADDR.ARPA");
            add("26.172.IN-ADDR.ARPA");
            add("27.172.IN-ADDR.ARPA");
            add("28.172.IN-ADDR.ARPA");
            add("29.172.IN-ADDR.ARPA");
            add("30.172.IN-ADDR.ARPA");
            add("31.172.IN-ADDR.ARPA");
            add("168.192.IN-ADDR.ARPA");
        }
    };

    private static final SecureRandom rng = new SecureRandom();

    private boolean activated;
    private boolean connected;
    private boolean resetFlag = false;
    private boolean newNetowrk = false;

    private boolean usingIpv6;
    private boolean usingBlock;
    private boolean usingECS;
    private boolean usingTLS;
    private boolean usingNotification;
    private boolean usingEnhanced;

    private boolean onTrustedNetwork;

    private boolean debugMode = false;
    private boolean toggleIP = true;
    private final Set<InetAddress> dnsSet = new HashSet<InetAddress>();
    private long recent_blocking = 0;
    private final ArrayList<Integer> speedList = new ArrayList();
    private final int[] time_distribution = new int[5];
    private final HashQuery dnsQ = new HashQuery();
    private List<String> serverName;
    private String customServerIp;
    private Set<String> whitelistDomain = new HashSet<String>();
    private Set<String> wildCardDomain = new HashSet<String>();
    private String currentDst = "";
    private int connectionCount = 0;
    private final int queriesSent = 0;
    private final int queriesReceived = 0;


    public boolean isActive() {
        return activated;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isOnline() {
        return ConnectionMonitor.getInstance().isOnline();
    }

    public boolean isRouted() {
        return ConnectionMonitor.getInstance().isRouted();
    }

    public boolean isUsingIpv6() {
        return usingIpv6;
    }

    public boolean isUsingBlock() {
        return usingBlock;
    }

    public boolean isUsingECS() {
        return usingECS;
    }

    public boolean isUsingEnhanced() {
        return usingEnhanced;
    }


    public boolean isUsingTLS() {
        return usingTLS;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isResetFlag() {
        return resetFlag;
    }

    public boolean isNewNetowrk() {
        return newNetowrk;
    }

    public int getConnectionCount() {
        return connectionCount;
    }

    public int getQueriesSent() {
        return queriesSent;
    }

    public int getQueriesReceived() {
        return queriesReceived;
    }

    public boolean isUsingNotification() {
        return usingNotification;
    }

    void setOnline(boolean flag) {
    }

    void changeServer() {
        toggleIP = !toggleIP;
    }

    void setActivated(boolean flag) {
        activated = flag;
        Log.d(TAG, "activate" + flag);
    }

    void setConnected(boolean flag) {
        connected = flag;
        Log.d(TAG, "Connected" + flag);
    }

    public void setCurrentDst(String s) {
        currentDst = s;
    }

    public void setUsingIpv6(boolean flag) {
        usingIpv6 = flag;
    }

    public void setUsingTLS(boolean flag) {
        usingTLS = flag;
    }

    public void setUsingBlock(boolean flag) {
        usingBlock = flag;
    }

    public void setUsingECS(boolean flag) {
        usingECS = flag;
    }

    public void setUsingNotification(boolean flag) {
        usingNotification = flag;
    }

    public void setUsingEnhanced(boolean flag) {
        usingEnhanced = flag;
    }

    public void setDebugMode(boolean flag) {
        debugMode = flag;
    }

    public void setServerName(List<String> l) {
        serverName = l;
    }

    public void setServerIp(String ip) {
        customServerIp = ip;
    }

    public void setPortal(boolean flag) {
    }

    public void setReset(boolean flag) {
        resetFlag = flag;
    }

    public void setNewNetowrk(boolean flag) {
        newNetowrk = flag;
    }

    public boolean isCustomServer() {
        return this.serverName != null && this.serverName.size() == 1;
    }

    public void increTraffic(int type) {

        // TrafficDbHelper dbHelper = TrafficDbHelper.getInstance(DnsSeeker.getInstance());
        // SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ContentValues values = new ContentValues();
        // values.put(TrafficContract.TrafficEntry.COLUMN_NAME_TITLE, type);
        // values.put(TrafficContract.TrafficEntry.COLUMN_NAME_SUBTITLE, subtitle);
        // long newRowId = db.insert(TrafficContract.TrafficEntry.TABLE_NAME, null, values);
        connectionCount++;

    }

    public void updateTraffic() {
//        TrafficDbHelper dbHelper = TrafficDbHelper.getInstance(DnsSeeker.getInstance());
//        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // db.rawQuery("DELETE FROM " + TrafficContract.TrafficEntry.TABLE_NAME + " WHERE " +
//                TrafficContract.TrafficEntry.COLUMN_NAME_SUBTITLE + " <= " +
//                "datetime('now', '-24 hours')",null);
//        db.close();
    }

    public String getCurrentDst() {
        return currentDst;
    }

    public boolean isInStaticWhitelistDomains(String d) {
        return STATIC_WHITELIST_DOMAINS.contains(d);
    }

    public boolean isInWhitelistDomain(String d) {
        if (isInStaticWhitelistDomains(d)) {
            return true;
        }

        if (whitelistDomain.contains(d)) {
            return true;
        }
        if (!wildCardDomain.isEmpty()) {
            Iterator<String> it = wildCardDomain.iterator();
            while (it.hasNext()) {
                if (d.contains(it.next())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkPublicSuffix(String domain) {
        Set<String> temp = new HashSet<>();
        try {
            // File fileDir = new File("wildcardList.txt");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(DnsSeeker.getInstance().getResources().getAssets().open("public_suffix_list.txt"), StandardCharsets.UTF_8));
            String str;
            int count = 0;

            while ((str = in.readLine()) != null) {
                if (!str.startsWith("//") && !str.startsWith("=") && !str.equals("")) {
                    temp.add(str);
                }
                count = count + 1;
                //Log.d(str);
            }

        } catch (Exception e) {
            Log.i(TAG, "" + e);
        }
        Log.d(TAG, "" + temp.contains(domain));
        return temp.contains(domain);
    }

    public void addWhitelistDomain(String d) {
        // return true if get unseen dns server
        int points = d.length() - d.replace(".", "").length();
        if ((points != 1) && !checkPublicSuffix(d)) {
            whitelistDomain.add(d);
        } else {
            wildCardDomain.add(d);
        }
        saveWhitelist();
    }

    public void removeWhitelistDomain(String d) {
        // return true if get unseen dns server
        wildCardDomain.remove(d);
        whitelistDomain.remove(d);
        saveWhitelist();
    }

    public Set<String> getWhitelistDomain() {
        Set<String> mergedSet = new HashSet<String>();
        mergedSet.addAll(STATIC_WHITELIST_DOMAINS);
        mergedSet.addAll(wildCardDomain);
        mergedSet.addAll(whitelistDomain);
        return mergedSet;
    }

    public boolean isInDNSSet(List<InetAddress> l) {

        return dnsSet.containsAll(l);
    }

    public void addDNSSet(List<InetAddress> l) {
        dnsSet.addAll(l);
    }

    public Set<InetAddress> getDNSSet() {
        return dnsSet;
    }

    public HashQuery getDnsQ() {
        return dnsQ;
    }

    void configBySetting(boolean block, boolean ecs, boolean tls, boolean notification, boolean enhanced, Set whitelist, Set wildcard) {
        setUsingBlock(block);
        setUsingECS(ecs);
        setUsingTLS(tls);
        setUsingNotification(notification);
        setUsingEnhanced(enhanced);
        usingIpv6 = true;
        whitelistDomain = whitelist;
        wildCardDomain = wildcard;
    }

    public double getRecentDelay() {
        double sum = 0;
        if (!speedList.isEmpty()) {
            for (Integer each : speedList) {
                sum += each;
            }
            return sum / speedList.size();
        }
        return sum;
    }

    void updateSpeed(int t) {
        if (speedList.size() > 300) {
            speedList.remove(0);
        }
        if (t < 50) {
            time_distribution[0]++;
        } else if (t < 100) {
            time_distribution[1]++;
        } else if (t < 200) {
            time_distribution[2]++;
        } else if (t < 600) {
            time_distribution[3]++;
        } else if (t > 600) {
            time_distribution[4]++;
        }
        speedList.add(t);
    }

    void saveWhitelist() {
        SharedPreferences.Editor editor = DnsSeeker.getInstance().getSharedConfig().edit();
        editor.putStringSet("whitelistDomain", whitelistDomain);
        editor.putStringSet("wildcardDomain", wildCardDomain);
        editor.apply();
        editor.commit();
    }

    void resetSpeed() {
        speedList.clear();
    }

    public void setRecentBlocking() {
        recent_blocking = System.currentTimeMillis();
    }

    public boolean recentBlocking() {
        return (System.currentTimeMillis() - recent_blocking) < 120 * 1000;
    }

    private boolean getRandomBoolean() {
        return rng.nextDouble() < 0.5;
    }

    public String getServerName() {
        String server;
        //return "1.0.0.1";

        if (debugMode) {
            server = "1.0.0.1";
            return server;
        }
        if (!usingTLS && !isCustomServer()) {
            if (usingECS) {
                if (getRandomBoolean()) {
                    return "9.9.9.11";
                } else {
                    return "149.112.112.11";
                }
            } else if (usingBlock) {
                if (getRandomBoolean()) {
                    return "9.9.9.9";
                } else {
                    return "149.112.112.112";
                }
            } else {
                if (getRandomBoolean()) {
                    return "9.9.9.10";
                } else {
                    return "149.112.112.10";
                }
            }
        }
        if (serverName == null) {
            server = "9.9.9.9";
            return server;
        }
        if (serverName.isEmpty()) {
            server = "9.9.9.9";
            return server;
        } else {
            try {
                server = serverName.get(rng.nextInt(serverName.size()));
            } catch (Exception e) {
                server = null;
            }
            if (server == null) {
                server = "9.9.9.9";
            }
            return server;
        }

    }

    public String getCustomServerIp() {
        return customServerIp;
    }

    public boolean isOnTrustedNetwork() {
        return onTrustedNetwork;
    }

    public void setOnTrustedNetwork(boolean onTrustedNetwork) {
        this.onTrustedNetwork = onTrustedNetwork;
    }

    public boolean shouldAutoConnect() {
        // Besides the flag being set, we can auto connect only if we are already the chosen VPN provider
        return DnsSeeker.getInstance().getSharedConfig().getBoolean("autoConnect", false) && VpnService.prepare(DnsSeeker.getInstance()) == null;
    }

    public void setShouldAutoConnect(boolean shouldAutoConnect) {
        SharedPreferences.Editor editor = DnsSeeker.getInstance().getSharedConfig().edit();
        editor.putBoolean("autoConnect", shouldAutoConnect);
        editor.apply();
    }
}
