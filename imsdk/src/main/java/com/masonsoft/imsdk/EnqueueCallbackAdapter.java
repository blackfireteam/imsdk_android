package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMLog;

public class EnqueueCallbackAdapter<T extends EnqueueMessage> implements EnqueueCallback<T> {

    @Override
    public void onEnqueueSuccess(@NonNull T enqueueMessage) {
        IMLog.v("onEnqueueSuccess %s", enqueueMessage);
    }

    @Override
    public void onEnqueueFail(@NonNull T enqueueMessage, int errorCode, String errorMessage) {
        IMLog.v("onEnqueueFail errorCode:%s, errorMessage:%s, %s", errorCode, errorMessage, enqueueMessage);
    }

}
