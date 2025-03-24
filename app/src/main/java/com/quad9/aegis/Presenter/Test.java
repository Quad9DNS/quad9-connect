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

/**
 * A simple {@link Fragment} subclass.
 */
public class Test extends Fragment {
    private MybrRc mreceiver;
    TextView test_time;
    TextView encrypt_type;
    TextView ms;
    TextView tls_ms;
    TextView test_udp_server;
    TextView test_tls_server;
    TextView test_activated;
    TextView test_connected;
    TextView text_delay;
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
                    test_time.setText(DnsSeeker.getInstance().getResources().getString(R.string.toast_unreachable));
                    ms.setText("");
                    break;
                case MSG_TLS_TEST_OK:
                    //((TextView) getView().findViewById(R.id.test_tls_result)).setText(getActivity().getResources().getString(R.string.pass_test_tls));
                    //test_time.setText("Cannot Reach Quad9 Unencrypted!!");
                    //encrypt_type.setText("DNS over TLS");
                    break;
                case MSG_TLS_TEST_FAILED:
                    encrypt_type.setText(DnsSeeker.getInstance().getResources().getString(R.string.toast_unreachable));
                    ((TextView) getView().findViewById(R.id.test_tls_result)).setText(getActivity().getResources().getString(R.string.fail_test_tls));
                    tls_ms.setText("");
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


        return inflater.inflate(R.layout.fragment_test, container, false);
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        test_time = (TextView) view.findViewById(R.id.test_time);
        encrypt_type = (TextView) view.findViewById(R.id.test_tls_time);
        ms = (TextView) view.findViewById(R.id.ms);
        tls_ms = (TextView) view.findViewById(R.id.tls_ms);
        test_udp_server = (TextView) view.findViewById(R.id.test_udp_server);
        test_tls_server = (TextView) view.findViewById(R.id.test_tls_server);
        test_activated = (TextView) view.findViewById(R.id.test_activated);
        test_connected = (TextView) view.findViewById(R.id.test_connected);
        text_delay = view.findViewById(R.id.recent_delay_value);
        text_delay.setText(Double.toString(DnsSeeker.getStatus().getRecentDelay()));

        test_activated.setText(String.format(DnsSeeker.getInstance().getResources().getString(R.string.test_activated), DnsSeeker.getStatus().isActive()));
        test_connected.setText(String.format(DnsSeeker.getInstance().getResources().getString(R.string.test_connected), DnsSeeker.getStatus().isConnected()));
        mreceiver = new MybrRc();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mreceiver, new IntentFilter("TestResult"));

        Thread thread = new Thread(Connection);                //賦予執行緒工作
        thread.start();
/*
        if (testDns()) {
            ((TextView) getView().findViewById(R.id.test_udp_result)).setText(getActivity().getResources().getString(R.string.pass_test_udp));
        } else {
            ((TextView) getView().findViewById(R.id.test_udp_result)).setText(getActivity().getResources().getString(R.string.fail_test_udp));
            test_time.setText(DnsSeeker.getInstance().getResources().getString(R.string.toast_unreachable));
            ms.setText("");
        }

        if (testDnsTls()) {
            ((TextView) getView().findViewById(R.id.test_tls_result)).setText(getActivity().getResources().getString(R.string.pass_test_tls));
            test_time.setText("Cannot Reach Quad9 Unencrypted!!");
            encrypt_type.setText("DNS over TLS");
        } else {
            encrypt_type.setText(DnsSeeker.getInstance().getResources().getString(R.string.toast_unreachable));
            ((TextView) getView().findViewById(R.id.test_tls_result)).setText(getActivity().getResources().getString(R.string.fail_test_tls));
            tls_ms.setText("");
        }
        */


    }

    private Runnable Connection = new Runnable() {
        @Override
        public void run() {
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
                test_time.setText(time);
                test_udp_server.setText("Server: " + server);
            } else {
                encrypt_type.setText(time);
                test_tls_server.setText("Server: " + server);
            }
            //     update_counter(intent.getStringExtra("counter"));
        }
    }


}
