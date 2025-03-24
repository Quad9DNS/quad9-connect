package com.quad9.aegis.Presenter;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.quad9.aegis.Analytics;
import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */


public class Settings extends PreferenceFragmentCompat {
    private static final String TAG = "Home";

    static MultiSelectListPreference mListPreference;
    PackageManager packageManager;
    SharedPreferences sharedPreferences;
    private static CharSequence entries[];
    private static CharSequence entryValues[];
    private static boolean listReady = false;
    private static MybrRc mReceiver;

    public Settings() {

        // Required empty public constructor

    }

    private static class GetAppList extends AsyncTask<PackageManager, Integer, String> {
        @Override
        protected String doInBackground(PackageManager... packageManager) {
            listReady = false;
            ArrayList<ApplicationInfo> installedApplications = new ArrayList<ApplicationInfo>();
            List<PackageInfo> installedPackages =
                    packageManager[0].getInstalledPackages(PackageManager.GET_PERMISSIONS);

            for (PackageInfo packageInfo : installedPackages) {

                if (packageInfo.requestedPermissions == null)
                    continue;

                for (String permission : packageInfo.requestedPermissions) {

                    if (TextUtils.equals(permission, android.Manifest.permission.INTERNET)) {
                        installedApplications.add(packageInfo.applicationInfo);
                        Log.d("tag", "getting list");
                        break;
                    }
                }
            }
            Collections.sort(installedApplications, new Comparator<ApplicationInfo>() {
                @Override
                public int compare(ApplicationInfo s1, ApplicationInfo s2) {
                    return s1.loadLabel(packageManager[0]).toString().compareToIgnoreCase(s2.loadLabel(packageManager[0]).toString());

                }
            });
            entries = new CharSequence[installedApplications.size()];
            entryValues = new CharSequence[installedApplications.size()];
            int i = 0;
            for (ApplicationInfo app : installedApplications) {
                entries[i] = app.loadLabel(packageManager[0]);
                entryValues[i] = app.packageName;
                i++;
            }
            mListPreference.setEntries(entries);
            mListPreference.setEntryValues(entryValues);
            listReady = true;
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(DnsSeeker.getInstance());
            Intent localIntent = new Intent("ListReady");
            localBroadcastManager.sendBroadcast(localIntent);
            return "";
        }

    }

    @MainThread
    public void prepareAppList(PackageManager packageManager) {
        GetAppList getAppListTask = new GetAppList();
        getAppListTask.execute(packageManager);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preference, rootKey);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        //onSharedPreferenceChanged(sharedPreferences, getString(R.string.movies_categories_key));
    }

    public void onClick(View v) {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        return view;
    }

    @Nullable
    @Override
    public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
        view.setBackground(getResources().getDrawable(R.drawable.gradient));
        setDivider(DnsSeeker.getInstance().getDrawable(R.drawable.divider));
        //setDividerHeight(2);
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        mListPreference = (MultiSelectListPreference) findPreference("white_list");
        mListPreference.setEnabled(false);
        mReceiver = new MybrRc();
        prepareAppList(getActivity().getPackageManager());
        //addPreferencesFromResource(R.xml.preference);
        //findPreference("app_version").setSummary();

        final Preference button = findPreference("checkbox_tls");
        button.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {
                DnsSeeker.getStatus().setUsingTLS(newValue.equals(true));
                Log.d("preference", "setUsingTLS" + ((SwitchPreference) preference).isChecked());
                if (DnsSeeker.getStatus().isActive()) {
                    DnsSeeker.scheduleRestart(1, 1);
                }
                if (newValue.equals(false)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.caution);
                    builder.setMessage(R.string.p_dialog_tls);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //action on dialog close
                        }
                    });
                    builder.show();
                }
                return true;
            }
        });

        final Preference button_mal = findPreference("checkbox_malicious");

        final Preference.OnPreferenceChangeListener button_mal_Listener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {

                DnsSeeker.getStatus().setUsingBlock(newValue.equals(true));
                Log.d("preference", "setUsingBlock" + ((SwitchPreference) preference).isChecked());
                if (DnsSeeker.getStatus().isActive()) {
                    DnsSeeker.scheduleRestart(1, 1);
                }
                return true;
            }
        };
        button_mal.setOnPreferenceChangeListener(button_mal_Listener);

        final Preference button_ecs = findPreference("checkbox_ecs");
        button_ecs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {

                DnsSeeker.getStatus().setUsingECS(newValue.equals(true));
                Log.d("preference", "setUsingECS" + ((SwitchPreference) preference).isChecked());
                // button_mal.setOnPreferenceChangeListener(null);
                //if(newValue.equals(true)) {
                //    ((SwitchPreference) button_mal).setChecked(newValue.equals(true));
                //}
                //button_mal.setEnabled(!newValue.equals(true));
                //button_mal.setOnPreferenceChangeListener(button_mal_Listener);

                if (DnsSeeker.getStatus().isActive()) {
                    DnsSeeker.scheduleRestart(1, 1);
                }
                return true;
            }
        });

        final Preference button_enhanced = findPreference("checkbox_enhanced");
        button_enhanced.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {

                DnsSeeker.getStatus().setUsingEnhanced(newValue.equals(true));
                Log.d("preference", "setUsingEnhanced" + ((SwitchPreference) preference).isChecked());

                return true;
            }
        });

        final Preference button_collection = findPreference("checkbox_collection");
        button_collection.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {
                Analytics.INSTANCE.setAnalyticsCollectionEnabled(requireContext(), ((SwitchPreference) preference).isChecked());
                Log.d("preference", "setUsingCollection" + ((SwitchPreference) preference).isChecked());
                return true;
            }
        });

        Preference myPref = findPreference("white_list");
        myPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if (DnsSeeker.getStatus().isActive()) {
                    DnsSeeker.scheduleRestart(1, 1);
                }
                return true;
            }

        });

        Preference domain_white_list = findPreference("domain_white_list");
        domain_white_list.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Fragment nextFrag = new Whitelist();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_frame, nextFrag)
                        .hide(Settings.this)
                        .addToBackStack(null)
                        .commit();
                return true;
            }
        });
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, new IntentFilter("ListReady"));

        Preference trustedNetworks = findPreference("trusted_networks");
        trustedNetworks.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Fragment nextFrag = new TrustedNetworks();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_frame, nextFrag)
                        .hide(Settings.this)
                        .addToBackStack(null)
                        .commit();
                return true;
            }
        });


        final Preference button_noti = findPreference("checkbox_notification");
        button_noti.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                                              Object newValue) {
                DnsSeeker.getStatus().setUsingNotification(newValue.equals(true));
                Log.d("preference", "setUsingNotification" + ((SwitchPreference) preference).isChecked());

                return true;
            }
        });
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        return new PreferenceGroupAdapter(preferenceScreen) {
            @SuppressLint("RestrictedApi")
            @Override
            public void onBindViewHolder(PreferenceViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                Preference preference = getItem(position);
                Log.d(TAG, preference.getClass().getName());
                if (preference instanceof PreferenceCategory) {
                    Log.d(TAG, "set Zeropadding");
                    setZeroPaddingToLayoutChildren(holder.itemView);
                } else {
                    View iconFrame = holder.itemView.findViewById(R.id.icon_frame);
                    if (iconFrame != null) {
                        iconFrame.setVisibility(preference.getIcon() == null ? View.GONE : View.VISIBLE);
                    }
                }
            }
        };
    }

    private void setZeroPaddingToLayoutChildren(View view) {
        if (!(view instanceof ViewGroup))
            return;
        ViewGroup viewGroup = (ViewGroup) view;
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            setZeroPaddingToLayoutChildren(viewGroup.getChildAt(i));
            Log.d(TAG, "setZeropadding");
            viewGroup.setPaddingRelative(40, 0, 0, 0);
        }
    }

    private class MybrRc extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mListPreference.setEnabled(true);
        }
    }
}

