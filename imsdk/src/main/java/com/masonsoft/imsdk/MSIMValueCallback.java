package com.masonsoft.imsdk;

/**
 * @since 1.0
 */
public interface MSIMValueCallback<T> {
    void onError(int code, String desc);

    void onSuccess(T t);
}
