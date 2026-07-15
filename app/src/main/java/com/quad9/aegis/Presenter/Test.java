package com.quad9.aegis.Presenter;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.Model.TestQuad9;
import com.quad9.aegis.R;
import com.quad9.aegis.databinding.FragmentTestBinding;

/**
 * A simple {@link Fragment} subclass.
 */
public class Test extends Fragment {
    private FragmentTestBinding binding;
    private static final int MSG_UDP_TEST_OK = 1;
    private static final int MSG_UDP_TEST_FAILED = 2;
    private static final int MSG_TLS_TEST_OK = 3;
    private static final int MSG_TLS_TEST_FAILED = 4;

    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_UDP_TEST_OK:
                    //((TextView) getView().findViewById(R.id.test_udp_result)).setText(getActivity().getResources().getString(R.string.pass_test_udp));
                    break;
                case MSG_UDP_TEST_FAILED:
                    ((TextView) getView().findViewById(R.id.test_udp_result)).setText(getActivity().getResources().getString(R.string.fail_test_udp));
                    binding.testTime.setText(DnsSeeker.getInstance().getResources().getString(R.string.toast_unreachable));
                    binding.ms.setText("");
                    break;
                case MSG_TLS_TEST_OK:
                    //((TextView) getView().findViewById(R.id.test_tls_result)).setText(getActivity().getResources().getString(R.string.pass_test_tls));
                    //test_time.setText("Cannot Reach Quad9 Unencrypted!!");
                    //encrypt_type.setText("DNS over TLS");
                    break;
                case MSG_TLS_TEST_FAILED:
                    binding.testTlsTime.setText(DnsSeeker.getInstance().getResources().getString(R.string.toast_unreachable));
                    ((TextView) getView().findViewById(R.id.test_tls_result)).setText(getActivity().getResources().getString(R.string.fail_test_tls));
                    binding.tlsMs.setText("");
                    break;


            }
            super.handleMessage(msg);
        }
    };

    public Test() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        binding = FragmentTestBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        binding.testActivated.setText(String.format(DnsSeeker.getInstance().getResources().getString(R.string.test_activated), DnsSeeker.getStatus().isActive()));
        binding.testConnected.setText(String.format(DnsSeeker.getInstance().getResources().getString(R.string.test_connected), DnsSeeker.getStatus().isConnected()));

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(new MybrRc(), new IntentFilter("TestResult"));

        Thread thread = new Thread(Connection);
        thread.start();
    }

    private Runnable Connection = () -> {
        if (testDns()) {
            myHandler.sendEmptyMessage(MSG_UDP_TEST_OK);
        } else {
            myHandler.sendEmptyMessage(MSG_UDP_TEST_FAILED);
        }
        if (testDnsTls()) {
            myHandler.sendEmptyMessage(MSG_TLS_TEST_OK);
        } else {
            myHandler.sendEmptyMessage(MSG_TLS_TEST_FAILED);
        }
    };

    public boolean testDns() {
        return TestQuad9.dig_over_udp(DnsSeeker.getInstance().getApplicationContext());
    }

    public boolean testDnsTls() {
        return TestQuad9.dig_over_tls(DnsSeeker.getInstance().getApplicationContext(), TestQuad9.getInstance().getServerCallback);
    }

    public class MybrRc extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String time = Integer.toString(intent.getIntExtra("time", 0));
            String server = intent.getStringExtra("server");
            if (!intent.getBooleanExtra("tls", false)) {
                binding.testTime.setText(time);
                binding.testUdpServer.setText("Server: " + server);
            } else {
                binding.testTlsTime.setText(time);
                binding.testTlsServer.setText("Server: " + server);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
