package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public interface MSIMSdkListener {

    void onConnecting();

    void onConnectSuccess();

    void onConnectFailed(int errorCode, String errorMessage);

    void onSigningIn();

    void onSignInSuccess();

    void onSignInFail(int errorCode, String errorMessage);

    void onKickedOffline();

    void onTokenExpired();

    void onSigningOut();

    void onSignOutSuccess();

    void onSignOutFail(int errorCode, String errorMessage);

}
