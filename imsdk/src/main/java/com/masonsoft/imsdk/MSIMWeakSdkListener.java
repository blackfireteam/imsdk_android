package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * @since 1.0
 */
public class MSIMWeakSdkListener implements MSIMSdkListener {

    @NonNull
    private final WeakReference<MSIMSdkListener> mOutRef;

    public MSIMWeakSdkListener(@Nullable MSIMSdkListener listener) {
        mOutRef = new WeakReference<>(listener);
    }

    @Override
    public void onConnecting() {
        final MSIMSdkListener out = mOutRef.get();
        if (out != null) {
            out.onConnecting();
        }
    }

    @Override
    public void onConnectSuccess() {
        final MSIMSdkListener out = mOutRef.get();
        if (out != null) {
            out.onConnectSuccess();
        }
    }

    @Override
    public void onConnectClosed() {
        final MSIMSdkListener out = mOutRef.get();
        if (out != null) {
            out.onConnectClosed();
        }
    }

    @Override
    public void onSigningIn() {
        final MSIMSdkListener out = mOutRef.get();
        if (out != null) {
            out.onSigningIn();
        }
    }

    @Override
    public void onSignInSuccess() {
        final MSIMSdkListener out = mOutRef.get();
        if (out != null) {
            out.onSignInSuccess();
        }
    }

    @Override
    public void onSignInFail(int errorCode, String errorMessage) {
        final MSIMSdkListener out = mOutRef.get();
        if (out != null) {
            out.onSignInFail(errorCode, errorMessage);
        }
    }

    @Override
    public void onKickedOffline() {
        final MSIMSdkListener out = mOutRef.get();
        if (out != null) {
            out.onKickedOffline();
        }
    }

    @Override
    public void onTokenExpired() {
        final MSIMSdkListener out = mOutRef.get();
        if (out != null) {
            out.onTokenExpired();
        }
    }

    @Override
    public void onSigningOut() {
        final MSIMSdkListener out = mOutRef.get();
        if (out != null) {
            out.onSigningOut();
        }
    }

    @Override
    public void onSignOutSuccess() {
        final MSIMSdkListener out = mOutRef.get();
        if (out != null) {
            out.onSignOutSuccess();
        }
    }

    @Override
    public void onSignOutFail(int errorCode, String errorMessage) {
        final MSIMSdkListener out = mOutRef.get();
        if (out != null) {
            out.onSignOutFail(errorCode, errorMessage);
        }
    }

}
