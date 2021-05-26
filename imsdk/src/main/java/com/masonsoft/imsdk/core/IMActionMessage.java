package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.util.Objects;

/**
 * 指令消息
 */
public class IMActionMessage implements EnqueueMessage {

    /**
     * 撤回一条会话消息(聊天消息)
     */
    public static final int ACTION_TYPE_REVOKE = 1;
    /**
     * 回执消息已读
     */
    public static final int ACTION_TYPE_MARK_AS_READ = 2;
    /**
     * 删除会话
     */
    public static final int ACTION_TYPE_DELETE_CONVERSATION = 3;

    private final long mSessionUserId;
    private final int mActionType;
    @Nullable
    private final Object mActionObject;

    private final long mSign = SignGenerator.nextSign();

    @NonNull
    private final IMCallback<GeneralResult> mEnqueueCallback;

    public IMActionMessage(
            long sessionUserId,
            int actionType,
            @Nullable Object actionObject,
            @Nullable IMCallback<GeneralResult> enqueueCallback) {
        mSessionUserId = sessionUserId;
        mActionType = actionType;
        mActionObject = actionObject;
        if (enqueueCallback != null) {
            mEnqueueCallback = enqueueCallback;
        } else {
            mEnqueueCallback = payload -> {
                // ignore
            };
        }
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
    public IMCallback<GeneralResult> getEnqueueCallback() {
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
