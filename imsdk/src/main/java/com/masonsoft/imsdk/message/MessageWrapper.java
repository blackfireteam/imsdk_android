package com.masonsoft.imsdk.message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.Message;

/**
 * @since 1.0
 */
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

    private String getProtoMessageObjectShortString() {
        if (mProtoMessageObject != null) {
            return mProtoMessageObject.getClass().getName();
        }
        return "null";
    }

    public String toShortString() {
        return String.format("MessageWrapper[origin:%s, proto message object:%s]", mOrigin, getProtoMessageObjectShortString());
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }
}
