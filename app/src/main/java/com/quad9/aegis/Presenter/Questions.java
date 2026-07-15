package com.quad9.aegis.Presenter;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.fragment.app.Fragment;

import com.quad9.aegis.R;
import com.quad9.aegis.databinding.FragmentQuestionsBinding;

/**
 * A simple {@link Fragment} subclass.
 */
public class Questions extends Fragment {

    public Questions() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentQuestionsBinding binding = FragmentQuestionsBinding.inflate(inflater, container, false);
        // Inflate the layout for this fragment
        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.setWebViewClient(new WebViewClient());
        binding.webView.loadUrl("https://www.quad9.net/support/faq/");
        return binding.getRoot();
    }


}
