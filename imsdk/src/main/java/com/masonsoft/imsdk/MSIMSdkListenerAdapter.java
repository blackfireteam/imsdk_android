package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.lang.GeneralResult;

/**
 * @since 1.0
 */
public class MSIMSdkListenerAdapter implements MSIMSdkListener {

    @Override
    public void onConnecting() {
        // ignore
    }

    @Override
    public void onConnectSuccess() {
        // ignore
    }

    @Override
    public void onConnectClosed() {
        // ignore
    }

    @Override
    public void onSigningIn() {
        // ignore
    }

    @Override
    public void onSignInSuccess() {
        // ignore
    }

    @Override
    public void onSignInFail(@NonNull GeneralResult result) {
        // ignore
    }

    @Override
    public void onKickedOffline() {
        // ignore
    }

    @Override
    public void onTokenExpired() {
        // ignore
    }

    @Override
    public void onSigningOut() {
        // ignore
    }

    @Override
    public void onSignOutSuccess() {
        // ignore
    }

    @Override
    public void onSignOutFail(@NonNull GeneralResult result) {
        // ignore
    }

}
