package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public interface MSIMValueCallback<T> {

    void onError(int errorCode, String errorMessage);

    void onSuccess(T value);

}
