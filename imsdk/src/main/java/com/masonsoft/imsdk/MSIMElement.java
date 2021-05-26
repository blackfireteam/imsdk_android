package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMMessage;

/**
 * @since 1.0
 */
public abstract class MSIMElement {

    @NonNull
    private final IMMessage mMessage;

    MSIMElement(@NonNull IMMessage message) {
        mMessage = message;
    }

    @NonNull
    IMMessage getMessage() {
        return mMessage;
    }

}
