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
import android.widget.CompoundButton;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.quad9.aegis.Analytics;
import com.quad9.aegis.Model.ConnectionMonitor;
import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.Model.GlobalVariables;
import com.quad9.aegis.Model.TestQuad9;
import com.quad9.aegis.R;
import com.quad9.aegis.databinding.FragmentHomeBinding;

/**
 * A simple {@link Fragment} subclass.
 */
public class Home extends Fragment {
    private static final String TAG = "Home";
    private FragmentHomeBinding binding;

    public Home() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        mOnCheckedChangeListener = (buttonView, isChecked) -> {
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
        };

        return binding.getRoot();
    }

    @Override
    public void onDetach() {
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(updateReceiver);
        super.onDetach();
    }

    @Override
    public void onStart() {

        // The problem is that button is listened by onCheckChangedListener. We cannot distinguish it is triggered
        // by "click" or by "set" with syncing current connect state.
        // TODO: check VPN here

        super.onStart();

        binding.onOffSwitch.setChecked(DnsSeeker.getStatus().isActive());
        binding.onOffSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        String temp;
        if (DnsSeeker.getStatus().isUsingTLS()) {
            temp = String.format(getResources().getString(R.string.queries_are_secured), DnsSeeker.getInstance().getTotalCount());
        } else {
            temp = String.format(getResources().getString(R.string.queries_are_sent), DnsSeeker.getInstance().getTotalCount());
        }
        binding.simpleLog.setText(temp);
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
                updateReceiver, new IntentFilter("ResponseResult"));
        binding.simpleLog.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            boolean blocked = false;
            bundle.putInt("isBlocked", blocked ? 1 : 0);
            Fragment nextFrag = new Record();
            nextFrag.setArguments(bundle);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_frame, nextFrag)
                    .hide(Home.this)
                    .addToBackStack(null)
                    .commit();
        });

        if (DnsSeeker.getInstance().getTotalCount() > 200000) {
            //particleView.setVisibility(View.VISIBLE);
        }

        BroadcastReceiver networkStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean connected = intent.getBooleanExtra("connected", false);
                if (connected) {
                    binding.onOffSwitch.setOnCheckedChangeListener(null);
                    onStatusConnected();
                    binding.onOffSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
                } else {
                    binding.onOffSwitch.setOnCheckedChangeListener(null);
                    onStatusOff();
                    binding.onOffSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
                }
            }
        };
        LocalBroadcastManager.getInstance(DnsSeeker.getInstance()).registerReceiver(
                networkStatusReceiver, new IntentFilter(GlobalVariables.NetworkStatus));

        if (DnsSeeker.getStatus().isActive()) {
            if (DnsSeeker.getStatus().isConnected()) {
                // btn_start.setOnCheckedChangeListener(null);
                // NEOMT! should fix this
                Thread t = new Thread(() -> TestQuad9.dig_over_tls(DnsSeeker.getInstance(), getServerCallback));
                t.start();
            } else {
                DnsSeeker.getInstance().deActivateService();
            }
        } else {
            binding.onOffSwitch.setOnCheckedChangeListener(null);
            onStatusOff();
            binding.onOffSwitch.setOnCheckedChangeListener(mOnCheckedChangeListener);
        }

    }

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener;

    // Set current layout
    public void onStatusConnected() {
        if (binding == null) {
            return;
        }

        Log.i(TAG, "onStatusConnected");
        binding.onOffSwitch.setChecked(true);
        if (DnsSeeker.getStatus().isUsingBlock()) {
            binding.textNetworkStatus.setText(R.string.enable_connected);
        } else {
            binding.textNetworkStatus.setText(R.string.enable_connected_nonblock);
        }
        if (DnsSeeker.getInstance().getConnectionMonitor().isPrivateDnsActive()) {
            binding.serverStatus.setText(R.string.connect_privateDns);// + DnsSeeker.getStatus().getServerName());
            binding.serverStatus.setTypeface(null, Typeface.NORMAL);
        } else if (DnsSeeker.getStatus().isUsingTLS()) {
            binding.serverStatus.setText(R.string.connect_encrypted);// + DnsSeeker.getStatus().getServerName());
            binding.serverStatus.setTypeface(null, Typeface.NORMAL);
        } else {
            binding.serverStatus.setText(R.string.connect_unencrypted);
            binding.serverStatus.setTypeface(null, Typeface.BOLD);
        }
        binding.serverDst.setText(DnsSeeker.getStatus().getCurrentDst());
        binding.currentNetwork.setText(DnsSeeker.getInstance().getConnectionMonitor().getCurrentNetwork());
        binding.textNetworkStatus.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.active));
        binding.serverStatus.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.active));
        binding.logo.setImageResource(R.drawable.ic_logo_light);

        binding.simpleLog.setVisibility(View.VISIBLE);
        binding.logo.setColorFilter(Color.argb(0, 255, 255, 255));
    }

    public void onStatusOff() {
        Log.i(TAG, "onStatusOff");
        if (DnsSeeker.getStatus().shouldAutoConnect() && DnsSeeker.getStatus().isOnTrustedNetwork()) {
            binding.textNetworkStatus.setText(R.string.not_connnected_trusted_network);
            binding.onOffSwitch.setChecked(true);
        } else {
            binding.textNetworkStatus.setText(R.string.not_enable);
            binding.onOffSwitch.setChecked(false);
        }
        binding.logo.setColorFilter(Color.argb(0, 255, 255, 255));
        //view_logo.setColorFilter(DnsSeeker.getInstance().getResources().getColor(R.color.notActive));
        binding.logo.setImageResource(R.drawable.ic_logo_dark);
        binding.simpleLog.setVisibility(View.INVISIBLE);
        binding.serverDst.setText("");
        binding.currentNetwork.setText("");
        binding.textNetworkStatus.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.notActive));
        binding.serverStatus.setTextColor(DnsSeeker.getInstance().getResources().getColor(R.color.notActive));

        binding.serverStatus.setText(R.string.not_connected);
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
                if (binding != null) {
                    binding.simpleLog.setText(temp);
                    binding.simpleLog.setCompoundDrawablesWithIntrinsicBounds(DnsSeeker.getInstance().getResources().getDrawable(R.drawable.ic_check_white_24dp), null, null, null);
                }
            } else {
                String temp = String.format(requireActivity().getResources().getString(R.string.queries_are_blocked), DnsSeeker.getInstance().getBlockedCount());
                if (binding != null) {
                    binding.simpleLog.setText(temp);
                    binding.simpleLog.setCompoundDrawablesWithIntrinsicBounds(DnsSeeker.getInstance().getResources().getDrawable(R.drawable.ic_block_white_24dp), null, null, null);
                }
            }
        }
    };
    private TestQuad9.Callback getServerCallback = s -> {
        Analytics.INSTANCE.setCustomCrashlyticsKey("CurrentServer", s);
        String currentNetwork = DnsSeeker.getInstance().getConnectionMonitor().getCurrentNetwork();
        Analytics.INSTANCE.setCustomCrashlyticsKey("CurrentNetwork", currentNetwork);
        Log.d(ConnectionMonitor.TAG, currentNetwork + " connect to: " + s);
        // https://stackoverflow.com/questions/39712117/ui-is-not-updated-during-a-listener-callback-in-android
        requireActivity().runOnUiThread(this::onStatusConnected);
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        mOnCheckedChangeListener = null;
    }
}