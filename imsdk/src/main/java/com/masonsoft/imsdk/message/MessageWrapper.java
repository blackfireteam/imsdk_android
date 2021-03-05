package com.masonsoft.imsdk.message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.Message;

public class MessageWrapper {

    @NonNull
    private final Message mOrigin;

    @Nullable
    private final Object mProtoMessageObject;

    public MessageWrapper(@NonNull Message origin) {
        mOrigin = origin;
        mProtoMessageObject = Message.Type.decode(origin);
    }

    @NonNull
    public Message getOrigin() {
        return mOrigin;
    }

    @Nullable
    public Object getProtoMessageObject() {
        return mProtoMessageObject;
    }

}
