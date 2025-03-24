package com.quad9.aegis.Presenter;


import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.quad9.aegis.Analytics;
import com.quad9.aegis.LogActivity;
import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.Model.GlobalVariables;
import com.quad9.aegis.Model.ServerSelector;
import com.quad9.aegis.Model.TraceRouteCopied;
import com.quad9.aegis.Model.TrafficContract;
import com.quad9.aegis.Model.TrafficDbHelper;
import com.quad9.aegis.R;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A simple {@link Fragment} subclass.
 */
public class Debug extends Fragment {
    private Button btn_crash_test;
    private Button btn_test;
    private Button btn_restart_test;
    private Button btn_edns_code;
    private Button btn_edns_payload;
    private Button btn_counts;
    private Button btn_reconnect_test;
    private Button btn_log;
    private Button btn_distribution;
    private Button btn_dnsset;
    private ProgressBar pb;
    int CONNECTION = 4280;
    int PACKET = 200;

    public Debug() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_debug, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        btn_crash_test = view.findViewById(R.id.btn_crash);
        btn_test = view.findViewById(R.id.btn_test);
        btn_restart_test = view.findViewById(R.id.btn_test_restart);
        btn_edns_code = view.findViewById(R.id.btn_edns_code);
        btn_edns_payload = view.findViewById(R.id.btn_edns_payload);
        btn_counts = view.findViewById(R.id.btn_counts);
        btn_reconnect_test = view.findViewById(R.id.btn_reconnect);
        btn_log = view.findViewById(R.id.btn_log);
        btn_distribution = view.findViewById(R.id.btn_distribution);
        btn_dnsset = view.findViewById(R.id.btn_dnsset);

        pb = view.findViewById(R.id.progressBar_cyclic);
        btn_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment nextFrag = (Fragment) new Test();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, nextFrag)
                        .addToBackStack(null)
                        .commit();
            }
        });
        if (Analytics.INSTANCE.isSupported()) {
            btn_crash_test.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Analytics.INSTANCE.testCrash();
                }
            });
        } else {
            btn_crash_test.setVisibility(View.GONE);
        }

        btn_reconnect_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("restartService");
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
        });
        btn_restart_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                String title = null;

                builder.setTitle(title);
                builder.setMessage("Set Dns server");
                LinearLayout layout = new LinearLayout(getActivity());
                layout.setOrientation(LinearLayout.VERTICAL);
                final EditText domain = new EditText(getActivity());
                domain.setHint("9.9.9.9");

                layout.addView(domain);

                builder.setView(layout);

                builder.setPositiveButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_add), null);
                builder.setNegativeButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

                final AlertDialog mAlertDialog = builder.create();
                mAlertDialog.setOnShowListener(dialog -> {

                    Button b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

                    if (DnsSeeker.getStatus().isCustomServer()) {
                        domain.setText(DnsSeeker.getStatus().getServerName());
                    }
                    b.setOnClickListener(view1 -> {
                        if (domain.getText() != null) {
                            String domainText = domain.getText().toString();
                            new Thread(() -> {
                                try {
                                    InetAddress address = InetAddress.getByName(domainText);
                                    ServerSelector.setBlockingPool(Collections.singletonList(domainText));
                                    DnsSeeker.getStatus().setServerIp(address.getHostAddress());
                                } catch (UnknownHostException e) {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Invalid DNS server", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                mAlertDialog.dismiss();
                            }).start();
                        }
                    });
                });
                mAlertDialog.show();
            }
        });
        btn_edns_code.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            String title = null;

            builder.setTitle(title);
            builder.setMessage("Set EDNS option code");
            LinearLayout layout = new LinearLayout(getActivity());
            layout.setOrientation(LinearLayout.VERTICAL);
            final EditText code = new EditText(getActivity());
            code.setHint("0xFEFE");

            layout.addView(code);

            builder.setView(layout);

            builder.setPositiveButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_add), null);
            builder.setNegativeButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_cancel), (dialog, id) -> {
            });

            final AlertDialog mAlertDialog = builder.create();
            mAlertDialog.setOnShowListener(dialog -> {
                SharedPreferences getPrefs = PreferenceManager
                        .getDefaultSharedPreferences(requireContext());

                int storedCode = getPrefs.getInt("ednsCode", 0);
                if (storedCode != 0) {
                    code.setText(String.format("0x%s", Integer.toString(storedCode, 16)));
                } else {
                    code.setText("");
                }
                Button b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(bView -> {
                    if (code.getText() != null) {
                        try {
                            getPrefs.edit().putInt("ednsCode", Integer.decode(code.getText().toString())).apply();
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), "Invalid EDNS Code", Toast.LENGTH_SHORT).show();
                        }
                    }
                    mAlertDialog.dismiss();

                });
            });
            mAlertDialog.show();

        });
        btn_edns_payload.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            String title = null;

            builder.setTitle(title);
            builder.setMessage("Set EDNS option payload");
            LinearLayout layout = new LinearLayout(getActivity());
            layout.setOrientation(LinearLayout.VERTICAL);
            final EditText payload = new EditText(getActivity());
            payload.setHint("UTF-8 payload");

            layout.addView(payload);

            builder.setView(layout);

            builder.setPositiveButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_add), null);
            builder.setNegativeButton(DnsSeeker.getInstance().getResources().getString(R.string.whitelist_cancel), (dialog, id) -> {
            });

            final AlertDialog mAlertDialog = builder.create();
            mAlertDialog.setOnShowListener(dialog -> {
                SharedPreferences getPrefs = PreferenceManager
                        .getDefaultSharedPreferences(requireContext());

                Button b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                payload.setText(getPrefs.getString("ednsPayload", ""));
                b.setOnClickListener(bView -> {
                    if (payload.getText() != null) {
                        getPrefs.edit().putString("ednsPayload", payload.getText().toString()).apply();
                    }
                    mAlertDialog.dismiss();

                });
            });
            mAlertDialog.show();

        });
        btn_counts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.btn_show_counts);
                ArrayList<Integer> a3600 = getTrafficStats(1);
                ArrayList<Integer> a86400 = getTrafficStats(24);
                builder.setMessage(String.format(getResources().getString(R.string.dia_traffic),
                        a3600.get(0),
                        a3600.get(1),
                        a3600.get(2),
                        calculateKB(a3600),
                        a86400.get(0),
                        a86400.get(1),
                        a86400.get(2),
                        calculateKB(a86400))
                );
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //action on dialog close
                    }
                });
                builder.show();
            }
        });
        btn_log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(),
                        LogActivity.class);
                startActivity(intent);
            }
        });

        btn_dnsset.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Traffic send to this ips are routed into app because they are labeled as dns server.");
            LinearLayout layout = new LinearLayout(getActivity());
            layout.setOrientation(LinearLayout.VERTICAL);
            TextView textView = new TextView(getActivity());
            textView.setText(DnsSeeker.getStatus().getDNSSet().toString());

            layout.addView(textView);
            builder.setView(layout);
            final AlertDialog mAlertDialog = builder.create();
            mAlertDialog.show();

        });
        btn_distribution.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TraceRouteCopied.Callback callback = new TraceRouteCopied.Callback() {
                    public void complete(TraceRouteCopied.Result r) {
                        pb.setVisibility(View.INVISIBLE);
                        Looper.prepare();
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.btn_test_traceroute);
                        builder.setMessage(r.content());
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //action on dialog close
                            }
                        });
                        builder.show();
                        Log.d("Debug", r.content() + " registered successfully!!\n");
                        Looper.loop();
                    }

                    public void update(TraceRouteCopied.Result r) {
                    }

                };
                pb.setVisibility(View.VISIBLE);
                TraceRouteCopied.start("9.9.9.9", callback);

                /*
                int[] time_d = DnsSeeker.getStatus().getTimeDistribution();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.caution);

                if(time_d!=null){
                    builder.setMessage(String.format(getResources().getString(R.string.test_distribution),time_d[0],time_d[1],time_d[2],time_d[3],time_d[4]));
                }else{
                    builder.setMessage("No recent Queries");
                }
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //action on dialog close
                    }
                });
                builder.show();

                 */
            }
        });
    }

    private ArrayList<Integer> getTrafficStats(int hours) {
        ArrayList a = new ArrayList<Integer>();
        TrafficDbHelper dbHelper = TrafficDbHelper.getInstance(DnsSeeker.getInstance());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TrafficContract.TrafficEntry.TABLE_NAME + " WHERE " + TrafficContract.TrafficEntry.COLUMN_NAME_TITLE + " = "
                        + GlobalVariables.CONNECTION + " AND " + TrafficContract.TrafficEntry.COLUMN_NAME_SUBTITLE + " > " + "datetime('now', '-" + hours + " hours')"
                , null);
        a.add(cursor.getCount());
        cursor = db.rawQuery("SELECT * FROM " + TrafficContract.TrafficEntry.TABLE_NAME + " WHERE " + TrafficContract.TrafficEntry.COLUMN_NAME_TITLE + " = "
                        + GlobalVariables.SENT + " AND " + TrafficContract.TrafficEntry.COLUMN_NAME_SUBTITLE + " > " + "datetime('now', '-" + hours + " hours')"
                , null);
        a.add(cursor.getCount());
        cursor = db.rawQuery("SELECT * FROM " + TrafficContract.TrafficEntry.TABLE_NAME + " WHERE " + TrafficContract.TrafficEntry.COLUMN_NAME_TITLE + " = "
                        + GlobalVariables.RECEIVED + " AND " + TrafficContract.TrafficEntry.COLUMN_NAME_SUBTITLE + " > " + "datetime('now', '-" + hours + " hours')"
                , null);
        a.add(cursor.getCount());
        db.close();

        return a;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    private void addBlocks() {
        openUrlInChrome("http://q9.agog.com/api/domains");
        //openNetworkSettings();
    }

    private void openUrlInChrome(String urlString) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.android.chrome");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            // Chrome browser presumably not installed so allow user to choose instead
            intent.setPackage(null);
            startActivity(intent);
        }
    }

    private void openNetworkSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
            } catch (ActivityNotFoundException ex) {
            }
        }
    }

    private double calculateKB(ArrayList<Integer> a) {
        int allBytes =
                a.get(0) * CONNECTION +
                        a.get(1) * PACKET +
                        a.get(2) * PACKET;
        Log.d("calculate", "" + (float) allBytes / 1000 / 1000);
        return (float) allBytes / 1000 / 1000;
    }

}
