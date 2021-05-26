package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.lang.GeneralResult;

/**
 * @since 1.0
 */
public interface MSIMSdkListener {

    void onConnecting();

    void onConnectSuccess();

    void onConnectClosed();

    void onSigningIn();

    void onSignInSuccess();

    void onSignInFail(@NonNull GeneralResult result);

    void onKickedOffline();

    void onTokenExpired();

    void onSigningOut();

    void onSignOutSuccess();

    void onSignOutFail(@NonNull GeneralResult result);

}
