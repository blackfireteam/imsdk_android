package com.masonsoft.imsdk.core.message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.ProtoByteMessage;
import com.masonsoft.imsdk.util.Objects;

/**
 * @since 1.0
 */
public class ProtoByteMessageWrapper {

    @NonNull
    private final ProtoByteMessage mOrigin;

    @Nullable
    private final Object mProtoMessageObject;

    public ProtoByteMessageWrapper(@NonNull ProtoByteMessage origin) {
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

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" origin:").append(this.mOrigin);
        builder.append(" ").append(Objects.defaultObjectTag(this.mProtoMessageObject));
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }
}
