package com.quad9.aegis.Presenter;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.Model.TestQuad9;
import com.quad9.aegis.Model.TraceRouteCopied;
import com.quad9.aegis.R;

import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class Help extends Fragment {
    private Button btn_send_mail = null;
    private Button btn_gen_traceroute = null;

    private String idserver = "";
    private ProgressBar pb = null;
    private Handler handler;
    TraceRouteCopied.Result r;
    private TestQuad9.Callback testCallback = s -> idserver = s;

    public Help() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_support, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        // We need to handle NetworkOnMainThreadException issue, it used to work because in
        // we happen to call Connection in a callback function before.
        btn_send_mail = getView().findViewById(R.id.btn_send_mail);
        btn_gen_traceroute = getView().findViewById(R.id.btn_gen_traceroute);
        pb = getView().findViewById(R.id.progressBar_cyclic);
        btn_send_mail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{"android-support@quad9.net"});
                i.putExtra(Intent.EXTRA_SUBJECT, "support for app");
                i.putExtra(Intent.EXTRA_TEXT, generateInfo());
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    DnsSeeker.popToast(R.string.toast_no_mail);
                }
            }
        });
        btn_gen_traceroute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateTraceroute();
            }
        });
    }

    private String generateInfo() {
        // Blocking return, should move Connection.run somewhere else.
        Connection.run();
        return String.format(Locale.getDefault(),
                "Basic Information: \nMODEL = %s \nApp Version = %s \nAndroid Version = %d\nId Server = %s\n",
                getDeviceName(), getLocalVersion(DnsSeeker.getInstance()), Build.VERSION.SDK_INT, idserver);
    }

    @Override
    public void onDetach() {
        TraceRouteCopied.stop();
        super.onDetach();
    }

    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String getLocalVersion(Context ctx) {
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

    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    private void generateTraceroute() {
        pb.setVisibility(View.VISIBLE);
        AlertDialog alertDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.btn_test_traceroute);
        builder.setMessage("...");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                TraceRouteCopied.stop();
                pb.setVisibility(View.INVISIBLE);
                dialog.dismiss();
            }
        });
        builder.setNeutralButton("Copy", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ClipboardManager myClipboard;
                myClipboard = (ClipboardManager) DnsSeeker.getInstance().getSystemService(CLIPBOARD_SERVICE);
                ClipData myClip;
                myClip = ClipData.newPlainText("text", r.content());
                myClipboard.setPrimaryClip(myClip);
            }
        });
        alertDialog = builder.show();
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                alertDialog.setMessage(r.content());
            }
        };

        TraceRouteCopied.Callback callback = new TraceRouteCopied.Callback() {
            public void complete(TraceRouteCopied.Result r) {
                Looper.prepare();
                update(r);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Dismiss the Dialog
                            pb.setVisibility(View.INVISIBLE);
                            // start selected activity
                        }
                    });
                }
                Looper.loop();
            }

            public void update(TraceRouteCopied.Result updata_r) {
                r = updata_r;
                handler.sendEmptyMessage(0);
            }
        };
        TraceRouteCopied.start("9.9.9.9", callback);
    }

    private Runnable Connection = new Runnable() {
        @Override
        public void run() {
            Log.d("Zzz", "start connect");
            TestQuad9.dig_over_tls(DnsSeeker.getInstance().getApplicationContext(), testCallback);
        }
    };
}



