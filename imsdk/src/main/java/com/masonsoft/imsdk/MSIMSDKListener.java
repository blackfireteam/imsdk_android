package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public interface MSIMSDKListener {
    void onConnecting();

    void onConnectSuccess();

    void onConnectFailed(int code, String error);

    void onKickedOffline();

    void onTokenExpired();

}
