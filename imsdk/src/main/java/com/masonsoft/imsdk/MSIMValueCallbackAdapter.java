package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public class MSIMValueCallbackAdapter<T> implements MSIMValueCallback<T> {

    @Override
    public void onError(int errorCode, String errorMessage) {
        // ignore
    }

    @Override
    public void onSuccess(T value) {
        // ignore
    }

}
