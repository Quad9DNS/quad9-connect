package com.quad9.aegis;

import static androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.net.VpnManager;
import android.net.VpnProfileState;
import android.net.VpnService;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.navigation.NavigationView;
import com.quad9.aegis.Model.ConnectionMonitor;
import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.Model.TrustedNetwork;
import com.quad9.aegis.Model.TrustedNetworkDbHelper;
import com.quad9.aegis.Presenter.About;
import com.quad9.aegis.Presenter.Help;
import com.quad9.aegis.Presenter.Home;
import com.quad9.aegis.Presenter.Questions;
import com.quad9.aegis.Presenter.Search;
import com.quad9.aegis.Presenter.Settings;
import com.quad9.aegis.Presenter.Statistics;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, FragmentManager.OnBackStackChangedListener {

    public static final String ACTION_CONNECT = "com.quad9.aegis.MainActivity.ACTION_CONNECT";
    public static final String ACTION_DISCONNECT = "com.quad9.aegis.MainActivity.ACTION_DISCONNECT";

    NavigationView navigationView;
    public static final String HOMETAG = "nav_home";

    public void dumpstack() {
        int index = getSupportFragmentManager().getBackStackEntryCount() - 1;
        for (; index >= 0; index--) {
            FragmentManager.BackStackEntry backEntry = getSupportFragmentManager().getBackStackEntryAt(index);
            String tag = backEntry.getName();
        }
    }

    private String getBackStackNameTop() {
        int index = getSupportFragmentManager().getBackStackEntryCount() - 1;
        if (index >= 0) {
            FragmentManager.BackStackEntry backEntry = getSupportFragmentManager().getBackStackEntryAt(index);
            return backEntry.getName();
        }
        return null;
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        String tag = null;
        Fragment fragment = null;
        Intent intent = null;
        boolean fragmentFlag = true;

        switch (id) {
            case R.id.nav_home:
                tag = HOMETAG;
                fragment = getSupportFragmentManager().findFragmentByTag(tag);
                if (fragment == null) {
                    fragment = new Home();
                }
                break;
            case R.id.nav_settings:
                tag = "nav_settings";
                fragment = getSupportFragmentManager().findFragmentByTag(tag);
                if (fragment == null) {
                    fragment = new Settings();
                }
                break;
            case R.id.nav_support:
                tag = "nav_support";
                fragment = getSupportFragmentManager().findFragmentByTag(tag);
                if (fragment == null) {
                    fragment = new Help();
                }
                break;
            case R.id.nav_FAQ:
                tag = "nav_FAQ";
                fragment = getSupportFragmentManager().findFragmentByTag(tag);
                if (fragment == null) {
                    fragment = new Questions();
                }
                break;
            case R.id.nav_statistics:
                tag = "nav_statistics";
                fragment = getSupportFragmentManager().findFragmentByTag(tag);
                if (fragment == null) {
                    fragment = new Statistics();
                }
                break;
            case R.id.nav_search:
                tag = "nav_search";
                fragment = getSupportFragmentManager().findFragmentByTag(tag);
                if (fragment == null) {
                    fragment = new Search();
                }
                break;
            case R.id.nav_about:
                tag = "nav_about";
                fragment = getSupportFragmentManager().findFragmentByTag(tag);
                if (fragment == null) {
                    fragment = new About();
                }
                break;
            default: // XXX does this ever get hit?
                Log.d("MainActivity", "default was hit");
        }

        dumpstack();
        if (fragmentFlag) {

            if (fragment != null) {
                boolean fragmentPopped = getSupportFragmentManager().popBackStackImmediate(tag, POP_BACK_STACK_INCLUSIVE);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.content_frame, fragment, tag);
                ft.addToBackStack(tag);
                ft.commit();
            }
        } else {
            onBackStackChanged();  // Need this to make state of drawer
            startActivity(intent);
        }
        dumpstack();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        dumpstack();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            String top = getBackStackNameTop();
            if (top != null && top.equals(HOMETAG)) {
                finish();
            } else {
                super.onBackPressed();
            }
        }
        dumpstack();
    }


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Analytics.INSTANCE.initialize(this);

        if (getIntent() != null) {
            if (TextUtils.equals(getIntent().getAction(), ACTION_CONNECT)) {
                beginService();
                finish();
            } else if (TextUtils.equals(getIntent().getAction(), ACTION_DISCONNECT)) {
                stopServiceByUser();
                finish();
            }
        }

        // For Splash page
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences getPrefs = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());
                boolean isFirstStart = getPrefs.getBoolean("firstStart", true);
                if (isFirstStart) {
                    final Intent i = new Intent(MainActivity.this, IntroActivity.class);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(i);
                        }
                    });
                    SharedPreferences.Editor e = getPrefs.edit();
                    e.putBoolean("firstStart", false);
                    e.apply();
                }
            }
        });
        t.start();

        // Toolbar and Drawer setup

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_home);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        // Home is Default entry fragment
        if (savedInstanceState == null) {
            Fragment fragment = new Home();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.content_frame, fragment, HOMETAG); // XXX nav_home should be constant
            ft.addToBackStack(HOMETAG);
            ft.commit();
        }

        ViewCompat.setOnApplyWindowInsetsListener(drawer, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.topMargin = insets.top;
            mlp.rightMargin = insets.right;
            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (TextUtils.equals(intent.getAction(), ACTION_CONNECT)) {
            beginService();
        } else if (TextUtils.equals(intent.getAction(), ACTION_DISCONNECT)) {
            stopServiceByUser();
        }
    }

    @Override
    public void onBackStackChanged() {
        Fragment currentFragment =
                getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (currentFragment != null) {
            if (currentFragment instanceof Home) {
                setTitle(R.string.title_home);
                navigationView.getMenu().findItem(R.id.nav_home).setChecked(true);
            } else if (currentFragment instanceof Settings) {
                setTitle(R.string.title_settings);
                navigationView.getMenu().findItem(R.id.nav_settings).setChecked(true);
            } else if (currentFragment instanceof Statistics) {
                setTitle(R.string.title_statistics);
                navigationView.getMenu().findItem(R.id.nav_statistics).setChecked(true);
            } else if (currentFragment instanceof Search) {
                setTitle(R.string.title_search);
                navigationView.getMenu().findItem(R.id.nav_search).setChecked(true);
            } else if (currentFragment instanceof Questions) {
                setTitle(R.string.title_FAQ);
                navigationView.getMenu().findItem(R.id.nav_FAQ).setChecked(true);
            } else if (currentFragment instanceof Help) {
                setTitle(R.string.title_support);
                navigationView.getMenu().findItem(R.id.nav_support).setChecked(true);
            } else if (currentFragment instanceof About) {
                setTitle(R.string.title_about);
                navigationView.getMenu().findItem(R.id.nav_about).setChecked(true);
            }
        }
    }

    private BroadcastReceiver switchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String message = intent.getStringExtra("key");
            if (message.equals("beginService")) {
                Log.i("Quad9 Connect", "MainActivity beginService");
                beginService();
            } else if (message.equals("stopService")) {
                stopServiceByUser();
            }
        }
    };

    @Override
    public void onResume() {

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                switchReceiver, new IntentFilter("SwitchService"));
        super.onResume();
        onBackStackChanged(); // We might be coming back from opening faq. Update state in Drawer
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(switchReceiver);
        super.onPause();
    }

    public void beginService() {
        Intent intent;
        try {
            intent = VpnService.prepare(DnsSeeker.getInstance());
        } catch (IllegalStateException ex) {
            Analytics.INSTANCE.log("IllegalStateException in beginService");
            Analytics.INSTANCE.log("Message: " + ex.getMessage());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Analytics.INSTANCE.log("Retrieving VPN manager");
                VpnManager vpnManager = (VpnManager) getSystemService(VPN_MANAGEMENT_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    VpnProfileState vpnProfileState = vpnManager.getProvisionedVpnProfileState();
                    Analytics.INSTANCE.log("VPN profile state: " + vpnProfileState);
                    if (vpnProfileState != null) {
                        Analytics.INSTANCE.log("VPN lockdown: " + vpnProfileState.isLockdownEnabled());
                        Analytics.INSTANCE.log("VPN always on: " + vpnProfileState.isAlwaysOn());
                    }
                }
            }
            throw ex;
        }
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            Log.i("Activity", "intent null");
            onActivityResult(0, RESULT_OK, null);
        }
    }

    public void stopServiceByUser() {
        DnsSeeker.getStatus().setShouldAutoConnect(false);
        DnsSeeker.deActivateService();
    }

    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result == RESULT_OK) {
            DnsSeeker.getStatus().setShouldAutoConnect(true);
            boolean trusted = false;
            if (
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
            ) {
                WifiManager wifiManager = getSystemService(WifiManager.class);
                try {
                    trusted = TrustedNetworkDbHelper.getInstance(this).isTrustedNetwork(new TrustedNetwork(wifiManager.getConnectionInfo()));
                } catch (SQLiteConstraintException ex) {
                    // ignore
                }
            }
            DnsSeeker.getStatus().setOnTrustedNetwork(trusted);
            if (trusted) {
                ConnectionMonitor.getInstance().enable(DnsSeeker.getInstance().getApplicationContext());
                DnsSeeker.deActivateService();
            } else {
                DnsSeeker.activateService();
            }
        } else {
            DnsSeeker.getStatus().setShouldAutoConnect(false);
            showUnableDialog();
            Log.i("Activity", "RESULT_NOT_OK");
            DnsSeeker.deActivateService();
        }
    }

    public void showUnableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quad9");// R.string.caution);
        Log.i("Activity", this.getResources().getString(R.string.other_always_vpn));

        builder.setMessage(Html.fromHtml(this.getResources().getString(R.string.other_always_vpn)));
        builder.setNeutralButton("Steps", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://support.quad9.net/hc/en-us/articles/360046736911-Configure-Android-to-use-Private-DNS-feature-with-Quad9"));
                startActivity(intent);
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //action on dialog close
            }
        });
        builder.show();
    }

}







