package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.lang.GeneralResult;

@Deprecated
public class EnqueueCallbackAdapter<T extends EnqueueMessage> implements EnqueueCallback<T> {

    @Override
    public void onEnqueueSuccess(@NonNull T enqueueMessage) {
        IMLog.v("onEnqueueSuccess %s", enqueueMessage);
    }

    @Override
    public void onEnqueueFail(@NonNull T enqueueMessage, @NonNull GeneralResult result) {
        IMLog.v("onEnqueueFail result:%s, %s", result, enqueueMessage);
    }

}
