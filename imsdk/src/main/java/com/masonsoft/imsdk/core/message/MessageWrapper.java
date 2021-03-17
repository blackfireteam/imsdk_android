package com.masonsoft.imsdk.core.message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.ProtoByteMessage;

/**
 * @since 1.0
 */
public class MessageWrapper {

    @NonNull
    private final ProtoByteMessage mOrigin;

    @Nullable
    private final Object mProtoMessageObject;

    public MessageWrapper(@NonNull ProtoByteMessage origin) {
        mOrigin = origin;
        mProtoMessageObject = ProtoByteMessage.Type.decode(origin);
    }

    @NonNull
    public ProtoByteMessage getOrigin() {
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
