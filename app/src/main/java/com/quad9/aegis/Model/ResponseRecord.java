package com.quad9.aegis.Model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ResponseRecord {

    public short requestId;
    public String type;
    public String name;
    public String IP;
    public String resolver;
    public int time;
    public String timeStamp;
    public byte[] rawData;
    public List<String> provider;
    public List<String> provider_url;


    public ResponseRecord(
            short requestId,
            String type,
            String name,
            String IP,
            String resolver,
            int time,
            String timeStamp,
            byte[] rawData
    ) {
        this.name = name;
        this.requestId = requestId;
        this.IP = IP;
        this.type = type;
        this.resolver = resolver;
        this.time = time;
        this.timeStamp = timeStamp;
        this.rawData = rawData;
    }

    @Override
    public String toString() {
        //ResponseRecord temp = this;
        //if(this.IP.equals("")){
        //    temp = ResponseParser.parseResponseDetail(this);
        //}
        return String.format("Record: %s\n Round Trip Time: %s\n Time:",
                ResponseParser.getDNSString(this), this.time, this.timeStamp);
    }

    public void setProviders() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String result = "";
                HttpURLConnection connection;
                try {
                    URL url = new URL("https://api.quad9.net/search/" + name);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setDoInput(true);

                    InputStream inputStream = connection.getInputStream();
                    int status = connection.getResponseCode();
                    Log.d("requestAPI", String.valueOf(status));
                    if (inputStream != null) {
                        InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                        BufferedReader in = new BufferedReader(reader);

                        String line = "";
                        while ((line = in.readLine()) != null) {
                            result += (line + "\n");
                        }
                    } else {
                        result = "Did not work!";
                    }
                    JSONObject jsonObj = new JSONObject(result);
                    //Log.i("requestapi", jsonObj.toString());
                    JSONArray jArray = jsonObj.getJSONArray("blocked_by");
                    JSONArray meta = jsonObj.getJSONArray("meta");
                    provider_url = new ArrayList<String>();
                    if (meta != null) {
                        for (int i = 0; i < meta.length(); i++) {
                            if (meta.getJSONObject(i).has("url")) {
                                provider_url.add(meta.getJSONObject(i).get("url").toString());
                            } else {
                                provider_url.add("");
                            }
                        }
                    }
                    Log.d("requestapi", provider_url.toString());

                    provider = new ArrayList<String>();
                    if (jArray != null) {
                        for (int i = 0; i < jArray.length(); i++) {
                            provider.add(jArray.getString(i));
                        }
                    }
                    Log.d("requestAPI", provider.toString());
                } catch (Exception e) {
                    Log.d("ATask InputStream", e.getLocalizedMessage());
                    e.printStackTrace();
                    Log.d("requestAPI", e.toString());
                }
            }
        });
        thread.start();
    }

    public List<String> getProviders() {
        try {
            if (provider.size() > 0) {
                return provider;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public List<String> getProvidersUrl() {
        try {
            if (provider_url.size() > 0) {
                return provider_url;
            }
        } catch (Exception e) {
        }
        return null;
    }

}
