package com.quad9.aegis.Presenter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Created by HP on 10/23/2016.
 */
public class IntroFragment extends Fragment {
    //Layout id
    private static final String ARG_LAYOUT_RES_ID = "layoutResId";

    public static IntroFragment newInstance(int layoutResId) {
        IntroFragment sampleSlide = new IntroFragment();

        Bundle bundleArgs = new Bundle();
        bundleArgs.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        sampleSlide.setArguments(bundleArgs);
        return sampleSlide;
    }

    private int layoutResId;

    public IntroFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID))
            layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(layoutResId, container, false);
    }

}