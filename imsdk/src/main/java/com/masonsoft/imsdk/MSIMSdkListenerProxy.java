package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.lang.GeneralResult;

/**
 * @since 1.0
 */
public class MSIMSdkListenerProxy implements MSIMSdkListener {

    @Nullable
    private final MSIMSdkListener mOut;

    public MSIMSdkListenerProxy(@Nullable MSIMSdkListener listener) {
        mOut = listener;
    }

    @Override
    public void onConnecting() {
        if (mOut != null) {
            mOut.onConnecting();
        }
    }

    @Override
    public void onConnectSuccess() {
        if (mOut != null) {
            mOut.onConnectSuccess();
        }
    }

    @Override
    public void onConnectClosed() {
        if (mOut != null) {
            mOut.onConnectClosed();
        }
    }

    @Override
    public void onSigningIn() {
        if (mOut != null) {
            mOut.onSigningIn();
        }
    }

    @Override
    public void onSignInSuccess() {
        if (mOut != null) {
            mOut.onSignInSuccess();
        }
    }

    @Override
    public void onSignInFail(@NonNull GeneralResult result) {
        if (mOut != null) {
            mOut.onSignInFail(result);
        }
    }

    @Override
    public void onKickedOffline() {
        if (mOut != null) {
            mOut.onKickedOffline();
        }
    }

    @Override
    public void onTokenExpired() {
        if (mOut != null) {
            mOut.onTokenExpired();
        }
    }

    @Override
    public void onSigningOut() {
        if (mOut != null) {
            mOut.onSigningOut();
        }
    }

    @Override
    public void onSignOutSuccess() {
        if (mOut != null) {
            mOut.onSignOutSuccess();
        }
    }

    @Override
    public void onSignOutFail(@NonNull GeneralResult result) {
        if (mOut != null) {
            mOut.onSignOutFail(result);
        }
    }

}
