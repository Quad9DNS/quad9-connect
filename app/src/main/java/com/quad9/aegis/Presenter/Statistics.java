package com.quad9.aegis.Presenter;


import static com.quad9.aegis.Model.GlobalVariables.ALL;
import static com.quad9.aegis.Model.GlobalVariables.BLOCKED;
import static com.quad9.aegis.Model.GlobalVariables.FAILED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.R;
import com.quad9.aegis.databinding.FragmentStatisticsBinding;

import java.text.DecimalFormat;


/**
 * A simple {@link Fragment} subclass.
 */
public class Statistics extends Fragment {

    int success = 0;
    int fail = 0;
    int block = 0;
    private FragmentStatisticsBinding binding;

    public Statistics() {
        // Required empty public constructor
    }

    private Statistics getInstance() {
        return this;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);

        binding.btnResetCounter.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.caution);
            builder.setMessage(R.string.reset_dialog);
            builder.setPositiveButton("OK", (dialog, id) -> {
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
            });
            builder.setNegativeButton("Cancel", (dialog, id) -> {
                //action on dialog close
            });
            builder.show();

            //button.setSummary("All time queries : " + PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("total_q",0));
        });
        binding.circleSuccessClickArea.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("isBlocked", ALL);
            Fragment nextFrag = new Record();
            nextFrag.setArguments(bundle);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_frame, nextFrag)
                    .hide(Statistics.this)
                    .addToBackStack(null)
                    .commit();
        });
        binding.circleBlockedClickArea.setOnClickListener(v -> {
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
        });
        binding.circleFailedClickArea.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("isBlocked", FAILED);
            Fragment nextFrag = new Record();
            nextFrag.setArguments(bundle);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_frame, nextFrag)
                    .hide(Statistics.this)
                    .addToBackStack(null)
                    .commit();
        });
        makeGraph();

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
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
                        getActivity().runOnUiThread(() -> makeGraph());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    };

    public void prepareData() {

        int total = success + fail;
        binding.inTotal.setText(String.format(getResources().getString(R.string.in_total), total));

        // Prevent dividing 0.
        if (total == 0) {
            total = 1;
        }
        float r_s = (float) (success - block) / (float) total;
        float r_f = (float) fail / (float) total;
        float r_b = (float) block / (float) total;
        Log.d("statistic", "preapring Data: " + String.format("r_s: %f, r_f: %f, r_b: %f ", r_s, r_f, r_b));

        DecimalFormat df = new DecimalFormat("##.#%");

        binding.circleSuccessText.setText(String.valueOf(success - block));
        float scale_s = 0.5f + r_s;
        binding.circleSuccess.setScaleX(scale_s);
        binding.circleSuccess.setScaleY(scale_s);
//        ConstraintLayout.LayoutParams paramsS = new ConstraintLayout.LayoutParams(
//                binding.circleSuccessText.getLayoutParams());
//        int marginS = Math.max(60, (int) (30 / r_s));
//        paramsS.setMargins(marginS, marginS, marginS, marginS);
//        binding.circleSuccessText.setLayoutParams(paramsS);
        if (r_s > 0 && r_s < 0.1) {
            binding.circleSuccessRate.setText("< 0.1%");
        } else {
            binding.circleSuccessRate.setText(df.format(r_s));
        }

        binding.circleBlockedText.setText(String.valueOf(block));
//        float scale_b = 0.5f + r_b;
//        binding.circlb.setScaleX(scale_b);
//        binding.circleSuccess.setScaleY(scale_b);
//        ConstraintLayout.LayoutParams paramsB = new ConstraintLayout.LayoutParams(
//                binding.circleBlockedText.getLayoutParams());
//        int marginB = Math.max(60, (int) (30 / r_b));
//        paramsB.setMargins(marginB, marginB, marginB, marginB);
//        binding.circleBlockedText.setLayoutParams(paramsB);

        if (r_b > 0 && r_b < 0.1) {
            binding.circleBlockedRate.setText("< 0.1%");
        } else {
            binding.circleBlockedRate.setText(df.format(r_b));
        }

        binding.circleFailedText.setText(String.valueOf(fail));
//        binding.circleFailedText.setScaleX(1 + );
//        ConstraintLayout.LayoutParams paramsF = new ConstraintLayout.LayoutParams(
//                binding.circleFailedText.getLayoutParams());
//        int marginF = Math.max(60, (int) (30 / r_f));
//        paramsF.setMargins(marginF, marginF, marginF, marginF);
//        binding.circleFailedText.setLayoutParams(paramsF);

        if (r_f > 0 && r_f < 0.1) {
            binding.circleFailedRate.setText("< 0.1%");
        } else {
            binding.circleFailedRate.setText(df.format(r_f));
        }
    }

    private void makeGraph() {
        success = DnsSeeker.getInstance().getSuccessCount();
        fail = DnsSeeker.getInstance().getFailCount();
        block = DnsSeeker.getInstance().getBlockedCount();
        if (success != 0) {
            prepareData();
        }
    }

    private void sendResetToActivity() {
        Intent intent = new Intent("ResetStats");

        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
