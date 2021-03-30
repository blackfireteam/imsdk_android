package com.masonsoft.imsdk.core.message;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.util.Objects;

public class SessionProtoByteMessageWrapper {

    private final long mSessionUserId;
    @NonNull
    private final ProtoByteMessageWrapper mProtoByteMessageWrapper;

    public SessionProtoByteMessageWrapper(long sessionUserId, @NonNull ProtoByteMessageWrapper protoByteMessageWrapper) {
        mSessionUserId = sessionUserId;
        mProtoByteMessageWrapper = protoByteMessageWrapper;
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    @NonNull
    public ProtoByteMessageWrapper getProtoByteMessageWrapper() {
        return mProtoByteMessageWrapper;
    }

    @NonNull
    public String toShortString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" sessionUserId:").append(this.mSessionUserId);
        builder.append(" ").append(mProtoByteMessageWrapper.toShortString());
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

}
