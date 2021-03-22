package com.masonsoft.imsdk.sample;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSplashActivityBinding;

public class SplashActivity extends AppCompatActivity {

    private ImsdkSampleSplashActivityBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = ImsdkSampleSplashActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
    }

}
