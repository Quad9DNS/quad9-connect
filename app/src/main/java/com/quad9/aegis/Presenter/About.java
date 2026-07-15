package com.quad9.aegis.Presenter;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.R;
import com.quad9.aegis.databinding.FragmentAboutBinding;

/**
 * A simple {@link Fragment} subclass.
 */
public class About extends Fragment {
    private int debugCounter = 0;
    private FragmentAboutBinding binding;

    public About() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        binding.appVersion.setText(getLocalVersion(DnsSeeker.getInstance().getApplicationContext()));
        binding.privacyBtn.setOnClickListener(v1 -> {
            Uri uri = Uri.parse("https://quad9.net/service/privacy");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        });
        binding.imageView.setOnClickListener(v1 -> {
            debugCounter++;
            if (debugCounter >= 9) {
                Fragment nextFrag = new Debug();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_frame, nextFrag)
                        .hide(About.this)
                        .addToBackStack(null)
                        .commit();
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static String getLocalVersion(Context ctx) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return ctx.getResources().getString(R.string.app_version) + " " + localVersion;
    }
}
