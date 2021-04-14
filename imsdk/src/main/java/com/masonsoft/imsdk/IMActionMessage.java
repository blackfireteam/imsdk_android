package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.util.Objects;

/**
 * 指令消息
 */
public class IMActionMessage implements EnqueueMessage {

    /**
     * 撤回一条会话消息(聊天消息)
     */
    public static final int ACTION_TYPE_REVOKE = 1;

    private final long mSessionUserId;
    private final int mActionType;
    @Nullable
    private final Object mActionObject;

    private final long mSign = SignGenerator.next();

    @NonNull
    private final EnqueueCallback<IMActionMessage> mEnqueueCallback;

    public IMActionMessage(
            long sessionUserId,
            int actionType,
            @Nullable Object actionObject,
            @NonNull EnqueueCallback<IMActionMessage> enqueueCallback) {
        mSessionUserId = sessionUserId;
        mActionType = actionType;
        mActionObject = actionObject;
        mEnqueueCallback = enqueueCallback;
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    public int getActionType() {
        return mActionType;
    }

    @Nullable
    public Object getActionObject() {
        return mActionObject;
    }

    public long getSign() {
        return mSign;
    }

    @NonNull
    public EnqueueCallback<IMActionMessage> getEnqueueCallback() {
        return mEnqueueCallback;
    }

    @NonNull
    public String toShortString() {
        //noinspection StringBufferReplaceableByString
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" actionType:").append(mActionType);
        builder.append(" actionObject:").append(mActionObject);
        builder.append(" sign:").append(mSign);
        return builder.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return this.toShortString();
    }

}
