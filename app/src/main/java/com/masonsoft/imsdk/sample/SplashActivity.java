package com.masonsoft.imsdk.sample;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.idonans.core.WeakAbortSignal;
import com.idonans.core.thread.Threads;
import com.idonans.systeminsets.SystemUiHelper;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSplashActivityBinding;

public class SplashActivity extends AppCompatActivity {

    private boolean mPendingRedirect;
    private boolean mStarted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SystemUiHelper.from(getWindow())
                .layoutStatusBar()
                .layoutStable()
                .setLightStatusBar()
                .setLightNavigationBar()
                .apply();

        final ImsdkSampleSplashActivityBinding binding = ImsdkSampleSplashActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.imsdkNameText.setText(com.masonsoft.imsdk.BuildConfig.LIB_NAME);
        binding.imsdkVersionText.setText(buildImsdkVersionText());

        Threads.postUi(new RedirectToMain(this), 1500L);
    }

    private static String buildImsdkVersionText() {
        return com.masonsoft.imsdk.BuildConfig.LIB_VERSION_NAME + "(" + com.masonsoft.imsdk.BuildConfig.LIB_VERSION_CODE + ")";
    }

    @Override
    protected void onStart() {
        super.onStart();
        mStarted = true;

        if (mPendingRedirect) {
            doRedirect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mStarted = false;
    }

    private void dispatchRedirect() {
        if (isFinishing()) {
            return;
        }

        if (!mStarted) {
            mPendingRedirect = true;
            return;
        }

        doRedirect();
    }

    private void doRedirect() {
        if (isFinishing()) {
            return;
        }

        MainActivity.start(this);
        finish();
    }

    private static class RedirectToMain extends WeakAbortSignal implements Runnable {

        public RedirectToMain(@Nullable SplashActivity splashActivity) {
            super(splashActivity);
        }

        @Override
        public void run() {
            if (isAbort()) {
                return;
            }

            SplashActivity splashActivity = (SplashActivity) getObject();
            if (splashActivity == null) {
                return;
            }

            splashActivity.dispatchRedirect();
        }

    }

}
