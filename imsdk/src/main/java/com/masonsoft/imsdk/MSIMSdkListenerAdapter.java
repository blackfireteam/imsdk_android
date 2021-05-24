package com.masonsoft.imsdk;

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
    public void onConnectFailed(int errorCode, String errorMessage) {
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
    public void onSignInFail(int errorCode, String errorMessage) {
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
    public void onSignOutFail(int errorCode, String errorMessage) {
        // ignore
    }

}
