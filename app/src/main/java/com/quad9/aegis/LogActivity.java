package com.quad9.aegis;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.quad9.aegis.Model.DnsSeeker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class LogActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        try {
            Process process = Runtime.getRuntime().exec("logcat -d *:I");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line + "\n");
            }
            TextView tv = (TextView) findViewById(R.id.textView);
            tv.setMovementMethod(new ScrollingMovementMethod());
            tv.setText(log.toString());
            tv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) DnsSeeker.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("logs", log.toString());
                    clipboard.setPrimaryClip(clip);
                    DnsSeeker.popToast("Added to clipboard");
                    return true;
                }
            });
        } catch (IOException e) {
            // Handle Exception
        }
        final ScrollView scrollview = ((ScrollView) findViewById(R.id.ScrollView1));
        scrollview.post(new Runnable() {

            @Override
            public void run() {
                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.log_root), (v, windowInsets) -> {
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

}