package com.masonsoft.imsdk.sample.splash;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.idonans.core.WeakAbortSignal;
import com.idonans.core.thread.Threads;
import com.idonans.systeminsets.SystemUiHelper;
import com.masonsoft.imsdk.sample.app.FragmentDelegateActivity;
import com.masonsoft.imsdk.sample.main.MainActivity;

public class SplashActivity extends FragmentDelegateActivity {

    private static final String FRAGMENT_TAG_SPLASH = "fragment_splash_20210322";

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

        setFragmentDelegate(FRAGMENT_TAG_SPLASH, SplashFragment::newInstance);

        Threads.postUi(new RedirectToMain(this), 1500L);
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
