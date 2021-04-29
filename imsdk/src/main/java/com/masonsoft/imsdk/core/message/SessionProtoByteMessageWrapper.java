package com.masonsoft.imsdk.core.message;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.util.Objects;

public class SessionProtoByteMessageWrapper {

    @NonNull
    private final SessionTcpClient mSessionTcpClient;
    private final long mSessionUserId;
    @NonNull
    private final ProtoByteMessageWrapper mProtoByteMessageWrapper;

    public SessionProtoByteMessageWrapper(
            @NonNull SessionTcpClient sessionTcpClient,
            long sessionUserId,
            @NonNull ProtoByteMessageWrapper protoByteMessageWrapper) {
        mSessionTcpClient = sessionTcpClient;
        mSessionUserId = sessionUserId;
        mProtoByteMessageWrapper = protoByteMessageWrapper;
    }

    @NonNull
    public SessionTcpClient getSessionTcpClient() {
        return mSessionTcpClient;
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
        //noinspection StringBufferReplaceableByString
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" mSessionUserId:").append(this.mSessionUserId);
        builder.append(" mSessionTcpClient:").append(this.mSessionTcpClient);
        builder.append(" mProtoByteMessageWrapper:").append(this.mProtoByteMessageWrapper.toShortString());
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

}
