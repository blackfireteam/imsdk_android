package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public interface MSIMCallback {

    void onError(int code, String desc);

    void onSuccess();

}
