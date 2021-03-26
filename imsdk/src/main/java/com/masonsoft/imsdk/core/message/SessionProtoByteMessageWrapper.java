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

    public String toShortString() {
        return String.format(Objects.defaultObjectTag(this) + "[sessionUserId:%s, messageWrapper:%s]", mSessionUserId, mProtoByteMessageWrapper.toShortString());
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

}
