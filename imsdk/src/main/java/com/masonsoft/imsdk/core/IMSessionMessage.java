package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.util.Objects;

/**
 * 会话消息(聊天消息)
 */
public class IMSessionMessage implements EnqueueMessage {

    private final long mSessionUserId;
    private long mToUserId;
    private int mConversationType = IMConstants.ConversationType.C2C;

    // 是否是重发消息，重发消息时不会重新生成消息 id, seq 与 timeMs
    private final boolean mResend;

    @NonNull
    private final IMMessage mMessage;

    @NonNull
    private final EnqueueCallback<IMSessionMessage> mEnqueueCallback;

    public IMSessionMessage(
            long sessionUserId,
            long toUserId,
            boolean resend,
            @NonNull IMMessage imMessage,
            @NonNull EnqueueCallback<IMSessionMessage> enqueueCallback) {
        mSessionUserId = sessionUserId;
        mToUserId = toUserId;
        mResend = resend;
        mMessage = imMessage;
        mEnqueueCallback = enqueueCallback;
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    public long getToUserId() {
        return mToUserId;
    }

    public void setToUserId(long toUserId) {
        mToUserId = toUserId;
    }

    public int getConversationType() {
        return mConversationType;
    }

    public void setConversationType(int conversationType) {
        mConversationType = conversationType;
    }

    public boolean isResend() {
        return mResend;
    }

    @NonNull
    public IMMessage getMessage() {
        return mMessage;
    }

    @NonNull
    public EnqueueCallback<IMSessionMessage> getEnqueueCallback() {
        return mEnqueueCallback;
    }

    @NonNull
    public String toShortString() {
        //noinspection StringBufferReplaceableByString
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" sessionUserId:").append(this.mSessionUserId);
        builder.append(" toUserId:").append(this.mToUserId);
        builder.append(" ").append(this.mMessage.toShortString());
        return builder.toString();
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

}