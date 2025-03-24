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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.R;

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
    private View rootView;
    Button button;
    EditText mEdit;
    TextView mUrl;
    TextView mProvider;
    TextView websiteStatus;
    TextView searchResult;
    TextView providerExp;
    LinearLayout mResultLayout;


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
        LayoutInflater lf = getActivity().getLayoutInflater();
        rootView = lf.inflate(R.layout.fragment_search, container, false);
        // Inflate the layout for this fragment
        button = rootView.findViewById(R.id.search_btn);
        mEdit = (EditText) rootView.findViewById(R.id.search_input);
        mProvider = rootView.findViewById(R.id.provider_list);
        searchResult = rootView.findViewById(R.id.search_result);
        providerExp = rootView.findViewById(R.id.provider_exp);
        mResultLayout = rootView.findViewById(R.id.search_frame_line);
        mUrl = rootView.findViewById(R.id.search_url);
        websiteStatus = rootView.findViewById(R.id.website_status);
        button.setOnClickListener(example);
        queryStop = false;
        return rootView;
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

    Boolean validateDomain(String validate) {
        Pattern pattern = Pattern.compile("/^\\(\\(?=[a-z0-9-]{1,63}\\.\\)\\(xn--\\)?[a-z0-9]+\\(-[a-z0-9]+\\)*\\.\\)+[a-z]{2,63}$/i");
        boolean zeeMatch = pattern.matcher(validate).find();
        return zeeMatch;
    }

    private View.OnClickListener example = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d("search", "searching");
            submit(v);
        }
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
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String result = "";
                HttpURLConnection connection;
                try {
                    URL url = new URL("https://api.quad9.net/search/" + getDomain(mEdit.getText().toString()));
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
            }
        });
        thread.start();
    }

    public void setString() {
        TextView providerView = rootView.findViewById(R.id.provider_list);
        providerView.setText(providerStr);
    }

    public void setBlocked(boolean b) {
        mResultLayout.setVisibility(View.VISIBLE);
        mUrl.setText(urlStr);
        if (b) {
            websiteStatus.setTextColor(getResources().getColor(R.color.colorAccent));
            searchResult.setBackground(getResources().getDrawable(R.drawable.rounded_corner_blocked));
            searchResult.setText(getResources().getString(R.string.search_statu_blocked));
            providerExp.setVisibility(View.VISIBLE);
            mProvider.setVisibility(View.VISIBLE);
        } else {
            websiteStatus.setTextColor(getResources().getColor(R.color.nonblockSerach));
            searchResult.setBackground(getResources().getDrawable(R.drawable.rounded_corner_nonblocked));
            searchResult.setText(getResources().getString(R.string.search_statu_nonblocked));
            mProvider.setVisibility(View.INVISIBLE);
            providerExp.setVisibility(View.INVISIBLE);

        }
    }

    public void setBlockedStr() {

        String temp = listToString(provider, provider_url);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // FROM_HTML_MODE_LEGACY is the behaviour that was used for versions below android N
            // we are using this flag to give a consistent behaviour
            mProvider.setText(Html.fromHtml(temp, Html.FROM_HTML_MODE_COMPACT));

        } else {
            mProvider.setText(Html.fromHtml(temp));
        }
        mProvider.setMovementMethod(LinkMovementMethod.getInstance());
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
}
