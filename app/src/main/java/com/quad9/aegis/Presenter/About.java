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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class About extends Fragment {
    private View rootView;
    private int debugCounter = 0;

    public About() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_about, container, false);
        TextView appVersion = rootView.findViewById(R.id.app_version);
        appVersion.setText(getLocalVersion(DnsSeeker.getInstance().getApplicationContext()));
        ImageView icon = rootView.findViewById(R.id.imageView);
        TextView privacyBtn = rootView.findViewById(R.id.privacyBtn);
        privacyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v1) {
                Uri uri = Uri.parse("https://quad9.net/service/privacy");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        icon.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v1) {
                debugCounter++;
                if (debugCounter >= 9) {
                    Fragment nextFrag = new Debug();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .add(R.id.content_frame, nextFrag)
                            .hide(About.this)
                            .addToBackStack(null)
                            .commit();
                }
            }

        });
        return rootView;
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
