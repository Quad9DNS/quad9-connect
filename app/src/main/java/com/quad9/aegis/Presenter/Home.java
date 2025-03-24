package com.quad9.aegis.Presenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.quad9.aegis.Analytics;
import com.quad9.aegis.Model.ConnectionMonitor;
import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.Model.GlobalVariables;
import com.quad9.aegis.Model.TestQuad9;
import com.quad9.aegis.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class Home extends Fragment {
    private static final String TAG = "Home";

    private ToggleButton btn_start;
    private Button btn_simple_log;
    private TextView text_network_status;
    private TextView networkServer;
    private TextView server_dst;
    private TextView current_network;

    ImageView view_logo;
    private boolean init_checked = false;

    Context ctx;
    View rootView;

    public Home() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);
        return rootView;
    }

    @Override
    public void onDetach() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(updateReceiver);
        super.onDetach();
    }

    private void sendToDnsSeeker() {
        Intent intent = new Intent("AskForStatistic");
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    /*private BroadcastReceiver saveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            total_q = intent.getIntExtra("success",0) + intent.getIntExtra("fail",0);
            blocked_q = intent.getIntExtra("blocked",0);
            text_total_q.setText(Integer.toString(total_q));
            text_blocked_q.setText(Integer.toString(blocked_q));
        }
    };*/
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        // SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        btn_simple_log = rootView.findViewById(R.id.simple_log);

        ctx = getActivity().getApplicationContext();
        btn_start = rootView.findViewById(R.id.on_off_switch);
        view_logo = rootView.findViewById(R.id.logo);
        text_network_status = rootView.findViewById(R.id.text_network_status);
        networkServer = rootView.findViewById(R.id.server_status);
        server_dst = rootView.findViewById(R.id.server_dst);
        current_network = rootView.findViewById(R.id.current_network);
    }

    @Override
    public void onStart() {

        // The problem is that button is listened by onCheckChangedListener. We cannot distinguish it is triggered
        // by "click" or by "set" with syncing current connect state.
        // TODO: check VPN here

        super.onStart();

        btn_start.setChecked(DnsSeeker.getStatus().isActive());
        btn_start.setOnCheckedChangeListener(mOnCheckedChangeListener);
        String temp;
        if (DnsSeeker.getStatus().isUsingTLS()) {
            temp = String.format(getResources().getString(R.string.queries_are_secured), DnsSeeker.getInstance().getTotalCount());
        } else {
            temp = String.format(getResources().getString(R.string.queries_are_sent), DnsSeeker.getInstance().getTotalCount());
        }
        btn_simple_log.setText(temp);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                updateReceiver, new IntentFilter("ResponseResult"));
        btn_simple_log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                boolean blocked = false;
                bundle.putInt("isBlocked", blocked ? 1 : 0);
                Fragment nextFrag = new Record();
                nextFrag.setArguments(bundle);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_frame, nextFrag)
                        .hide(Home.this)
                        .addToBackStack(null)
                        .commit();
            }
        });

        if (DnsSeeker.getInstance().getTotalCount() > 200000) {
            //particleView.setVisibility(View.VISIBLE);
        }

        BroadcastReceiver networkStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean connected = intent.getBooleanExtra("connected", false);
                if (connected) {
                    btn_start.setOnCheckedChangeListener(null);
                    onStatusConnected();
                    btn_start.setOnCheckedChangeListener(mOnCheckedChangeListener);
                } else {
                    btn_start.setOnCheckedChangeListener(null);
                    onStatusOff();
                    btn_start.setOnCheckedChangeListener(mOnCheckedChangeListener);
                }
            }
        };
        LocalBroadcastManager.getInstance(DnsSeeker.getInstance()).registerReceiver(
                networkStatusReceiver, new IntentFilter(GlobalVariables.NetworkStatus));

        if (DnsSeeker.getStatus().isActive()) {
            if (DnsSeeker.getStatus().isConnected()) {
                init_checked = true;
                // btn_start.setOnCheckedChangeListener(null);
                // NEOMT! should fix this
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TestQuad9.dig_over_tls(DnsSeeker.getInstance(), getServerCallback);
                    }
                });
                t.start();
            } else {
                DnsSeeker.getInstance().deActivateService();
            }
        } else {
            btn_start.setOnCheckedChangeListener(null);
            onStatusOff();
            btn_start.setOnCheckedChangeListener(mOnCheckedChangeListener);
        }

    }


    @Override
    public void onResume() {
        super.onResume();
        //particleView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        //particleView.pause();
    }

    private static CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener;

    {
        mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch (buttonView.getId()) {
                    case R.id.on_off_switch:
                        Intent intentbr = new Intent("SwitchService");
                        if (isChecked) {
                            intentbr.putExtra("key", "beginService");
                            Log.i(TAG, "Start button turning on");
                        } else {
                            intentbr.putExtra("key", "stopService");
                            Log.i(TAG, "Start button turning off");
                        }
                        LocalBroadcastManager.getInstance(DnsSeeker.getInstance()).sendBroadcast(intentbr);
                        break;
                }
            }
        };
    }

    // Set current layout
    public void onStatusConnected() {
        Log.i(TAG, "onStatusConnected");
        btn_start.setChecked(true);
        if (DnsSeeker.getStatus().isUsingBlock()) {
            text_network_status.setText(R.string.enable_connected);
        } else {
            text_network_status.setText(R.string.enable_connected_nonblock);
        }
        if (DnsSeeker.getInstance().getConnectionMonitor().isPrivateDnsActive()) {
            networkServer.setText(R.string.connect_privateDns);// + DnsSeeker.getStatus().getServerName());
            networkServer.setTypeface(null, Typeface.NORMAL);
        } else if (DnsSeeker.getStatus().isUsingTLS()) {
            networkServer.setText(R.string.connect_encrypted);// + DnsSeeker.getStatus().getServerName());
            networkServer.setTypeface(null, Typeface.NORMAL);
        } else {
            networkServer.setText(R.string.connect_unencrypted);
            networkServer.setTypeface(null, Typeface.BOLD);
        }
        server_dst.setText(DnsSeeker.getStatus().getCurrentDst());
        current_network.setText(DnsSeeker.getInstance().getConnectionMonitor().getCurrentNetwork());
        text_network_status.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.active));
        networkServer.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.active));
        view_logo.setImageResource(R.drawable.ic_logo_light);

        btn_simple_log.setVisibility(View.VISIBLE);
        view_logo.setColorFilter(Color.argb(0, 255, 255, 255));
    }

    public void onStatusOff() {
        Log.i(TAG, "onStatusOff");
        if (DnsSeeker.getStatus().shouldAutoConnect() && DnsSeeker.getStatus().isOnTrustedNetwork()) {
            text_network_status.setText(R.string.not_connnected_trusted_network);
            btn_start.setChecked(true);
        } else {
            text_network_status.setText(R.string.not_enable);
            btn_start.setChecked(false);
        }
        view_logo.setColorFilter(Color.argb(0, 255, 255, 255));
        //view_logo.setColorFilter(DnsSeeker.getInstance().getResources().getColor(R.color.notActive));
        view_logo.setImageResource(R.drawable.ic_logo_dark);
        btn_simple_log.setVisibility(View.INVISIBLE);
        server_dst.setText("");
        current_network.setText("");
        text_network_status.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.notActive));
        networkServer.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.notActive));

        networkServer.setText(R.string.not_connected);
    }


    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!DnsSeeker.getInstance().getStatus().recentBlocking()) {
                String temp;
                if (DnsSeeker.getStatus().isUsingTLS()) {
                    temp = String.format(getResources().getString(R.string.queries_are_secured), DnsSeeker.getInstance().getTotalCount());
                } else {
                    temp = String.format(getResources().getString(R.string.queries_are_sent), DnsSeeker.getInstance().getTotalCount());
                }
                btn_simple_log.setText(temp);
                btn_simple_log.setCompoundDrawablesWithIntrinsicBounds(DnsSeeker.getInstance().getResources().getDrawable(R.drawable.ic_check_white_24dp), null, null, null);
            } else {
                String temp = String.format(getActivity().getResources().getString(R.string.queries_are_blocked), DnsSeeker.getInstance().getBlockedCount());
                btn_simple_log.setText(temp);
                btn_simple_log.setCompoundDrawablesWithIntrinsicBounds(DnsSeeker.getInstance().getResources().getDrawable(R.drawable.ic_block_white_24dp), null, null, null);
            }
        }
    };
    private TestQuad9.Callback getServerCallback = new TestQuad9.Callback() {
        public void complete(String s) {
            Analytics.INSTANCE.setCustomCrashlyticsKey("CurrentServer", s);
            String currentNetwork = DnsSeeker.getInstance().getConnectionMonitor().getCurrentNetwork();
            Analytics.INSTANCE.setCustomCrashlyticsKey("CurrentNetwork", currentNetwork);
            Log.d(ConnectionMonitor.TAG, currentNetwork + " connect to: " + s);
            // https://stackoverflow.com/questions/39712117/ui-is-not-updated-during-a-listener-callback-in-android
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onStatusConnected();
                }
            });
        }
    };

}