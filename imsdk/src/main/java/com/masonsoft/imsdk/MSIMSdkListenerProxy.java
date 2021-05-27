package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.lang.GeneralResult;

/**
 * @since 1.0
 */
public class MSIMSdkListenerProxy extends RunOnUiThread implements MSIMSdkListener {

    @Nullable
    private final MSIMSdkListener mOut;

    public MSIMSdkListenerProxy(@Nullable MSIMSdkListener listener) {
        this(listener, false);
    }

    public MSIMSdkListenerProxy(@Nullable MSIMSdkListener listener, boolean runOnUiThread) {
        super(runOnUiThread);
        mOut = listener;
    }

    @Override
    public void onConnecting() {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onConnecting();
            }
        });
    }

    @Override
    public void onConnectSuccess() {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onConnectSuccess();
            }
        });
    }

    @Override
    public void onConnectClosed() {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onConnectClosed();
            }
        });
    }

    @Override
    public void onSigningIn() {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onSigningIn();
            }
        });
    }

    @Override
    public void onSignInSuccess() {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onSignInSuccess();
            }
        });
    }

    @Override
    public void onSignInFail(@NonNull GeneralResult result) {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onSignInFail(result);
            }
        });
    }

    @Override
    public void onKickedOffline() {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onKickedOffline();
            }
        });
    }

    @Override
    public void onTokenExpired() {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onTokenExpired();
            }
        });
    }

    @Override
    public void onSigningOut() {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onSigningOut();
            }
        });
    }

    @Override
    public void onSignOutSuccess() {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onSignOutSuccess();
            }
        });
    }

    @Override
    public void onSignOutFail(@NonNull GeneralResult result) {
        runOrPost(() -> {
            if (mOut != null) {
                mOut.onSignOutFail(result);
            }
        });
    }

}
