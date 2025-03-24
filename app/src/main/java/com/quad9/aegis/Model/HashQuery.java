package com.quad9.aegis.Model;

import static com.quad9.aegis.Model.Util.sendFailToApp;

import android.util.Log;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class HashQuery {
    private static final String TAG = "HashQuery";

    private final Hashtable<String, Util.PendingQuery> hash = new Hashtable<>();

    public void add(String key, Util.PendingQuery q) {
        String temp;
        if (!hash.isEmpty()) {
            Iterator<String> it = hash.keySet().iterator();
            while (it.hasNext()) {
                temp = it.next();
                if (hash.get(temp).lastSeconds() > 5) {
                    sendFailToApp(ResponseParser.parseQuestion(hash.get(temp).packet, "TIMEOUT"));

                    Log.d(TAG, "Timeout on Packet " + temp);
                    it.remove();
                }
            }
        }
        hash.put(key, q);
    }

    public void delete_all() {
        String temp;
        if (!hash.isEmpty()) {
            Iterator<String> it = hash.keySet().iterator();
            while (it.hasNext()) {
                temp = it.next();
                sendFailToApp(ResponseParser.parseQuestion(hash.get(temp).packet, "No Network Available"));
                Log.d(TAG, "Send fail" + temp);
                it.remove();
            }
        }
    }

    public double getDuration(String key) {
        Log.d(TAG, "time spent : " + hash.get(key).lastSeconds());
        return hash.get(key).lastSeconds();
    }

    public void delete(String key) {
        hash.remove(key);
    }

    public Util.PendingQuery get(String key) {
        return hash.get(key);
    }

    Boolean containsKey(String key) {
        return hash.containsKey(key);
    }

    Set keySet() {
        String temp;
        if (!hash.isEmpty()) {
            Iterator<String> it = hash.keySet().iterator();
            while (it.hasNext()) {
                temp = it.next();
                if (hash.get(temp).lastSeconds() > 5) {
                    sendFailToApp(ResponseParser.parseQuestion(hash.get(temp).packet, "TIMEOUT"));

                    Log.d(TAG, "Timeout on Packet " + temp);
                    it.remove();
                }
            }
        }
        return hash.keySet();

    }

    int size() {
        return hash.size();
    }

    boolean isEmpty() {
        return hash.isEmpty();
    }

}