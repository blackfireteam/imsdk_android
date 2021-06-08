package com.masonsoft.imsdk.sample;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.masonsoft.imsdk.MSIMSdkListener;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.sample.app.main.MainActivity;
import com.masonsoft.imsdk.sample.common.TopActivity;
import com.masonsoft.imsdk.sample.common.simpledialog.SimpleContentNoticeDialog;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.Threads;

/**
 * 处理登录失效状态通知
 */
public class IMTokenOfflineManager {

    private static final Singleton<IMTokenOfflineManager> INSTANCE = new Singleton<IMTokenOfflineManager>() {
        @Override
        protected IMTokenOfflineManager create() {
            return new IMTokenOfflineManager();
        }
    };

    public static IMTokenOfflineManager getInstance() {
        return INSTANCE.get();
    }

    private boolean mLastKickedOffline;
    private boolean mLastTokenExpired;

    private boolean mAttached;

    private String mConnectStateHumanString;
    private String mSignStateHumanString;

    @NonNull
    private final MSIMSdkListener mSdkListener = new MSIMSdkListener() {
        @Override
        public void onConnecting() {
            SampleLog.v("onConnecting");
            mConnectStateHumanString = "onConnecting";
        }

        @Override
        public void onConnectSuccess() {
            SampleLog.v("onConnectSuccess");
            mConnectStateHumanString = "onConnectSuccess";
        }

        @Override
        public void onConnectClosed() {
            SampleLog.v("onConnectClosed");
            mConnectStateHumanString = "onConnectClosed";
        }

        @Override
        public void onSigningIn() {
            SampleLog.v("onSigningIn");
            mSignStateHumanString = "onSigningIn";
        }

        @Override
        public void onSignInSuccess() {
            SampleLog.v("onSignInSuccess");
            mSignStateHumanString = "onSignInSuccess";
        }

        @Override
        public void onSignInFail(@NonNull GeneralResult result) {
            SampleLog.v("onSignInFail result:%s", result);
            mSignStateHumanString = "onSignInFail:" + result.getCause().message;
        }

        @Override
        public void onKickedOffline() {
            SampleLog.v("onKickedOffline");
            mSignStateHumanString = "onKickedOffline";
            setLastKickedOffline();
        }

        @Override
        public void onTokenExpired() {
            SampleLog.v("onTokenExpired");
            mSignStateHumanString = "onTokenExpired";
            setLastTokenExpired();
        }

        @Override
        public void onSigningOut() {
            SampleLog.v("onSigningOut");
            mSignStateHumanString = "onSigningOut";
        }

        @Override
        public void onSignOutSuccess() {
            SampleLog.v("onSignOutSuccess");
            mSignStateHumanString = "onSignOutSuccess";
        }

        @Override
        public void onSignOutFail(@NonNull GeneralResult result) {
            SampleLog.v("onSignOutFail result:%s", result);
            mSignStateHumanString = "onSignOutFail:" + result.getCause().message;
        }
    };

    public String getConnectStateHumanString() {
        return mConnectStateHumanString;
    }

    public String getSignStateHumanString() {
        return mSignStateHumanString;
    }

    private IMTokenOfflineManager() {
    }

    @NonNull
    public MSIMSdkListener getSdkListener() {
        return mSdkListener;
    }

    private void clearLast() {
        mLastKickedOffline = false;
        mLastTokenExpired = false;
    }

    private void setLastKickedOffline() {
        mLastKickedOffline = true;
        mLastTokenExpired = false;

        if (!mAttached) {
            return;
        }
        Threads.postUi(() -> {
            if (mAttached) {
                showKickedOffline();
            }
        });
    }

    private void setLastTokenExpired() {
        mLastKickedOffline = false;
        mLastTokenExpired = true;

        if (!mAttached) {
            return;
        }
        Threads.postUi(() -> {
            if (mAttached) {
                showTokenExpired();
            }
        });
    }

    @UiThread
    private void showKickedOffline() {
        final Activity topActivity = TopActivity.getInstance().get();
        if (topActivity == null) {
            SampleLog.v(Constants.ErrorLog.ACTIVITY_IS_NULL);
            return;
        }

        if (topActivity.isFinishing()) {
            SampleLog.v(Constants.ErrorLog.ACTIVITY_IS_FINISHING);
            return;
        }

        final SimpleContentNoticeDialog dialog = new SimpleContentNoticeDialog(
                topActivity,
                I18nResources.getString(R.string.imsdk_sample_tip_kicked_offline)
        );
        dialog.setOnHideListener(cancel -> MainActivity.start(topActivity, true));
        dialog.show();
    }

    @UiThread
    private void showTokenExpired() {
        final Activity topActivity = TopActivity.getInstance().get();
        if (topActivity == null) {
            SampleLog.v(Constants.ErrorLog.ACTIVITY_IS_NULL);
            return;
        }

        if (topActivity.isFinishing()) {
            SampleLog.v(Constants.ErrorLog.ACTIVITY_IS_FINISHING);
            return;
        }

        final SimpleContentNoticeDialog dialog = new SimpleContentNoticeDialog(
                topActivity,
                I18nResources.getString(R.string.imsdk_sample_tip_token_expired)
        );
        dialog.setOnHideListener(cancel -> MainActivity.start(topActivity, true));
        dialog.show();
    }

    public void attach() {
        mAttached = true;
    }

    public void detach() {
        mAttached = false;
    }

}
