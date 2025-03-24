package com.quad9.aegis.Presenter;


import static com.quad9.aegis.Model.GlobalVariables.ALL;
import static com.quad9.aegis.Model.GlobalVariables.BLOCKED;
import static com.quad9.aegis.Model.GlobalVariables.FAILED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.R;

import java.text.DecimalFormat;


/**
 * A simple {@link Fragment} subclass.
 */
public class Statistics extends Fragment {

    int success = 0;
    int fail = 0;
    int block = 0;
    LinearLayout circle_success;
    TextView circle_success_text;
    TextView circle_success_rate;
    LinearLayout circle_block;
    TextView circle_block_text;
    TextView circle_block_rate;
    LinearLayout circle_fail;
    TextView circle_fail_text;
    TextView circle_fail_rate;
    TextView in_total;
    LinearLayout layout_max;

    public Statistics() {
        // Required empty public constructor
    }

    private Statistics getInstance() {
        return this;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mView = inflater.inflate(R.layout.fragment_statistics, container, false);
        final Button restBtn = mView.findViewById(R.id.btn_reset_counter);
        in_total = mView.findViewById(R.id.in_total);
        circle_success = mView.findViewById(R.id.circle_success);
        circle_success_text = mView.findViewById(R.id.circle_success_text);
        circle_success_rate = mView.findViewById(R.id.circle_success_rate);
        circle_block = mView.findViewById(R.id.circle_blocked);
        circle_block_text = mView.findViewById(R.id.circle_blocked_text);
        circle_block_rate = mView.findViewById(R.id.circle_blocked_rate);
        circle_fail = mView.findViewById(R.id.circle_failed);
        circle_fail_text = mView.findViewById(R.id.circle_failed_text);
        circle_fail_rate = mView.findViewById(R.id.circle_failed_rate);
        layout_max = mView.findViewById(R.id.circle_max);

        restBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.caution);
                builder.setMessage(R.string.reset_dialog);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //action on dialog close
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt("success", 0);
                        editor.putInt("fail", 0);
                        editor.putInt("total_q", 0);
                        editor.putInt("blocked_q", 0);
                        editor.apply();
                        sendResetToActivity();
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.detach(getInstance()).attach(getInstance()).commit();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //action on dialog close
                    }
                });
                builder.show();

                //button.setSummary("All time queries : " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("total_q",0));
            }
        });
        circle_success.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putInt("isBlocked", ALL);
                Fragment nextFrag = new Record();
                nextFrag.setArguments(bundle);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_frame, nextFrag)
                        .hide(Statistics.this)
                        .addToBackStack(null)
                        .commit();
            }
        });
        circle_block.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                boolean blocked = true;
                bundle.putInt("isBlocked", BLOCKED);
                Fragment nextFrag = new Record();
                nextFrag.setArguments(bundle);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_frame, nextFrag)
                        .hide(Statistics.this)
                        .addToBackStack(null)
                        .commit();
            }
        });
        circle_fail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putInt("isBlocked", FAILED);
                Fragment nextFrag = new Record();
                nextFrag.setArguments(bundle);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_frame, nextFrag)
                        .hide(Statistics.this)
                        .addToBackStack(null)
                        .commit();
            }
        });
        makeGraph();

        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        //LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
        //        updateReceiver, new IntentFilter("UpdateRecord"));
        makeGraph();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(updateReceiver);
        super.onPause();

    }

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent

            new Thread() {
                @Override
                public void run() {
                    try {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                makeGraph();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    };

    public void prepareData(int a, int b, int c) {

        int total = success + fail;
        in_total.setText(String.format(getResources().getString(R.string.in_total), total));

        // Prevent dividing 0.
        if (total == 0) {
            total = 1;
        }
        float r_s = (float) (success - block) / (float) total;
        float r_f = (float) fail / (float) total;
        float r_b = (float) block / (float) total;
        Log.d("statistic", "preapring Data: " + String.format("r_s: %f, r_f: %f, r_b: %f ", r_s, r_f, r_b));

        float circle_max = layout_max.getHeight();
        float circle_min = getResources().getDimension(R.dimen.circle_lower_bound);

        DecimalFormat df = new DecimalFormat("##.#%");

        circle_success_text.setText(String.valueOf(success - block));
        LinearLayout.LayoutParams paramsS = new LinearLayout.LayoutParams(
                circle_success.getLayoutParams());

        paramsS.weight = (float) r_s * 70 + 50;
        circle_success.setLayoutParams(paramsS);
        String temp = "";
        Log.d("Show Stat layout", paramsS.debug(temp));
        if (r_s > 0 && r_s < 0.1) {
            circle_success_rate.setText("< 0.1%");
        } else {
            circle_success_rate.setText(df.format(r_s));
        }

        circle_block_text.setText(String.valueOf(block));
        LinearLayout.LayoutParams paramsB = new LinearLayout.LayoutParams(
                circle_block.getLayoutParams());
        paramsB.weight = r_b * 70 + 60;

        circle_block.setLayoutParams(paramsB);
        if (r_b > 0 && r_b < 0.1) {
            circle_block_rate.setText("< 0.1%");
        } else {
            circle_block_rate.setText(df.format(r_b));
        }

        circle_fail_text.setText(String.valueOf(fail));
        LinearLayout.LayoutParams paramsF = new LinearLayout.LayoutParams(
                circle_fail.getLayoutParams());
        paramsF.weight = r_f * 70 + 60;

        circle_fail.setLayoutParams(paramsF);
        if (r_f > 0 && r_f < 0.1) {
            circle_fail_rate.setText("< 0.1%");
        } else {
            circle_fail_rate.setText(df.format(r_f));
        }


    }

    private void makeGraph() {
        success = DnsSeeker.getInstance().getSuccessCount();
        fail = DnsSeeker.getInstance().getFailCount();
        block = DnsSeeker.getInstance().getBlockedCount();
        if (success == 0) {
        } else {
            prepareData(success, fail, block);
        }
    }

    private void sendResetToActivity() {
        Intent intent = new Intent("ResetStats");

        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }
}
