package com.quad9.aegis;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.quad9.aegis.Model.DnsSeeker;
import com.quad9.aegis.databinding.ActivityLogBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class LogActivity extends Activity {
    private ActivityLogBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            Process process = Runtime.getRuntime().exec("logcat -d *:I");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line + "\n");
            }
            binding.textView.setMovementMethod(new ScrollingMovementMethod());
            binding.textView.setText(log.toString());
            binding.textView.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) DnsSeeker.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("logs", log.toString());
                clipboard.setPrimaryClip(clip);
                DnsSeeker.popToast("Added to clipboard");
                return true;
            });
        } catch (IOException e) {
            // Handle Exception
        }
        binding.ScrollView1.post(() -> binding.ScrollView1.fullScroll(ScrollView.FOCUS_DOWN));

        ViewCompat.setOnApplyWindowInsetsListener(binding.logRoot, (v, windowInsets) -> {
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