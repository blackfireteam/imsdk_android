package com.masonsoft.imsdk.sample.app.splash;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.idonans.core.WeakAbortSignal;
import com.idonans.core.thread.Threads;
import com.idonans.systeminsets.SystemUiHelper;
import com.masonsoft.imsdk.sample.app.FragmentDelegateActivity;
import com.masonsoft.imsdk.sample.app.main.MainActivity;
import com.masonsoft.imsdk.sample.app.sign.SignInActivity;

public class SplashActivity extends FragmentDelegateActivity {

    private static final String FRAGMENT_TAG_SPLASH = "fragment_splash_20210322";

    private boolean mPendingRedirect;
    private boolean mStarted;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SystemUiHelper.from(getWindow())
                .layoutStatusBar()
                .layoutNavigationBar()
                .layoutStable()
                .setLightStatusBar()
                .setLightNavigationBar()
                .apply();

        setFragmentDelegate(FRAGMENT_TAG_SPLASH, SplashFragment::newInstance);

        Threads.postUi(new RedirectTask(this), 1500L);
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

        if (hasValidSession()) {
            // 已经登录，跳转到主页
            MainActivity.start(this);
        } else {
            // 没有登录，跳转到登录页
            SignInActivity.start(this);
        }

        finish();
    }

    /**
     * 如果存在有效的登录信息，返回 true, 否则返回 false.
     */
    private boolean hasValidSession() {
        // TODO
        return false;
    }

    private static class RedirectTask extends WeakAbortSignal implements Runnable {

        public RedirectTask(@Nullable SplashActivity splashActivity) {
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
