package com.masonsoft.imsdk.sample.splash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSplashFragmentBinding;

public class SplashFragment extends Fragment {

    public static SplashFragment newInstance() {
        Bundle args = new Bundle();
        SplashFragment fragment = new SplashFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final ImsdkSampleSplashFragmentBinding binding = ImsdkSampleSplashFragmentBinding.inflate(inflater, container, false);
        binding.imsdkNameText.setText(com.masonsoft.imsdk.BuildConfig.LIB_NAME);
        binding.imsdkVersionText.setText(buildImsdkVersionText());

        return binding.getRoot();
    }

    private static String buildImsdkVersionText() {
        return com.masonsoft.imsdk.BuildConfig.LIB_VERSION_NAME + "(" + com.masonsoft.imsdk.BuildConfig.LIB_VERSION_CODE + ")";
    }

}
