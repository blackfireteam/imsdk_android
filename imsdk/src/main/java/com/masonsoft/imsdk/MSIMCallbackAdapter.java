package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

/**
 * @since 1.0
 */
public class MSIMCallbackAdapter<T> implements MSIMCallback<T> {

    @Override
    public void onCallback(@NonNull T payload) {
        // ignore
    }

}
