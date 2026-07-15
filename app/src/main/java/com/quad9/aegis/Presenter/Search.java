package com.quad9.aegis.Presenter;


import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.R;
import com.quad9.aegis.databinding.FragmentSearchBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class Search extends Fragment {
    private FragmentSearchBinding binding;

    Handler handler;
    String providerStr, urlStr;
    ArrayList<String> provider_url;
    ArrayList<String> provider;
    private static final int MSG_FAILED = 0;
    private static final int MSG_BLOCKED = 1;
    private static final int MSG_NONBLOCKED = 2;

    private static boolean queryStop;

    public Search() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        binding.searchBtn.setOnClickListener(example);
        queryStop = false;
        return binding.getRoot();
    }

    @Override
    public void onDetach() {
        queryStop = true;
        super.onDetach();
    }

    String getDomain(String search) {
        Pattern pattern = Pattern.compile("^(?:(\\w+:)//)?(([^:/?#]*)(?:\\:([0-9]+))?)([/]{0,1}[^?#]*)(\\?[^#]*|)(#.*|)$");
        // String[] subUrl = pattern.split(search);
        Matcher zeeMatch = pattern.matcher(search);
        String result = search;
        while (zeeMatch.find()) {
            Log.d("search", zeeMatch.group());

            result = zeeMatch.group(2).replace("\\.$", "");
        }
        Log.d("search", result);
        return result;
    }

    private View.OnClickListener example = v -> {
        Log.d("search", "searching");
        submit(v);
    };

    public void submit(View view) {
        // EditText ed = (EditText) getLayoutInflater().inflate(R.id.search_input,null);
        // TODO: Fix Handler
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_FAILED:
                        DnsSeeker.popToast(providerStr);
                        break;
                    case MSG_BLOCKED:
                        setBlocked(true);
                        setBlockedStr();
                        break;
                    case MSG_NONBLOCKED:
                        setBlocked(false);
                        break;

                }
                super.handleMessage(msg);

            }
        };
        Thread thread = new Thread(() -> {
            String result = "";
            HttpURLConnection connection;
            try {
                URL url = new URL("https://api.quad9.net/search/" + getDomain(binding.searchInput.getText().toString()));
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);

                InputStream inputStream;
                int status = connection.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    inputStream = connection.getErrorStream();
                } else {
                    inputStream = connection.getInputStream();
                }
                Log.d("requestAPI", String.valueOf(status));
                if (queryStop) {
                    return;
                }
                if (inputStream != null) {
                    InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                    BufferedReader in = new BufferedReader(reader);

                    String line = "";
                    while ((line = in.readLine()) != null) {
                        result += (line + "\n");
                    }
                } else {
                    result = "Did not work!";

                    Log.d("requestapi", result);

                    providerStr = "Network Unavailable!";
                    handler.sendEmptyMessage(MSG_FAILED);
                    return;
                }
                JSONObject jsonObj = new JSONObject(result);
                Log.d("requestapi", jsonObj.toString());
                JSONArray meta = null;
                provider_url = new ArrayList<String>();
                provider = new ArrayList<String>();
                if (jsonObj.has("error")) {
                    Log.d("requestapi", "error");

                    providerStr = jsonObj.getString("error");
                    handler.sendEmptyMessage(MSG_FAILED);
                    return;
                } else {
                    if (jsonObj.has("blocked")) {
                        urlStr = jsonObj.getString("domain");
                        if (!jsonObj.getBoolean("blocked")) {

                            handler.sendEmptyMessage(MSG_NONBLOCKED);
                        } else {
                            meta = jsonObj.getJSONArray("meta");

                            handler.sendEmptyMessage(MSG_BLOCKED);
                        }
                    }
                }

                if (meta != null) {
                    for (int i = 0; i < meta.length(); i++) {
                        if (meta.getJSONObject(i).has("name")) {
                            provider.add(meta.getJSONObject(i).get("name").toString());
                        } else {
                            provider.add("");
                        }
                        if (meta.getJSONObject(i).has("url")) {
                            provider_url.add(meta.getJSONObject(i).get("url").toString());
                        } else {
                            provider_url.add("");
                        }
                    }
                } else {

                }
                if (jsonObj.has("blocked")) {
                    urlStr = jsonObj.getString("domain");
                    if (!jsonObj.getBoolean("blocked")) {
                        handler.sendEmptyMessage(MSG_NONBLOCKED);
                    } else {
                        handler.sendEmptyMessage(MSG_BLOCKED);
                    }
                } else {
                    // ERROR
                }
                Log.d("requestapi", provider_url.toString());
                Log.d("requestAPI", provider.toString());

            } catch (Exception e) {
                providerStr = "Network Unavailable!";
                handler.sendEmptyMessage(MSG_FAILED);
                Log.d("ATask InputStream", e.getLocalizedMessage());
                e.printStackTrace();
                Log.d("requestAPI", e.toString());
            }
        });
        thread.start();
    }

    public void setBlocked(boolean b) {
        binding.searchFrameLine.setVisibility(View.VISIBLE);
        binding.searchUrl.setText(urlStr);
        if (b) {
            binding.websiteStatus.setTextColor(getResources().getColor(R.color.colorAccent));
            binding.searchResult.setBackground(getResources().getDrawable(R.drawable.rounded_corner_blocked));
            binding.searchResult.setText(getResources().getString(R.string.search_statu_blocked));
            binding.providerExp.setVisibility(View.VISIBLE);
            binding.providerList.setVisibility(View.VISIBLE);
        } else {
            binding.websiteStatus.setTextColor(getResources().getColor(R.color.nonblockSerach));
            binding.searchResult.setBackground(getResources().getDrawable(R.drawable.rounded_corner_nonblocked));
            binding.searchResult.setText(getResources().getString(R.string.search_statu_nonblocked));
            binding.providerList.setVisibility(View.INVISIBLE);
            binding.providerExp.setVisibility(View.INVISIBLE);

        }
    }

    public void setBlockedStr() {

        String temp = listToString(provider, provider_url);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // FROM_HTML_MODE_LEGACY is the behaviour that was used for versions below android N
            // we are using this flag to give a consistent behaviour
            binding.providerList.setText(Html.fromHtml(temp, Html.FROM_HTML_MODE_COMPACT));

        } else {
            binding.providerList.setText(Html.fromHtml(temp));
        }
        binding.providerList.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private String listToString(List<String> l, List<String> url) {
        String str = "";
        for (int i = 0; i < l.size(); i++) {
            if (url.get(i).equals("")) {
                str += "<br>" + l.get(i);
            } else {
                str += "<br>" + "<a href='" + url.get(i) + "'> " + l.get(i) + " </a>";
            }
        }
        return str;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
