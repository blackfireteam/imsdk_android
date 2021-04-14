package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    @NonNull
    public EnqueueCallback<IMActionMessage> getEnqueueCallback() {
        return mEnqueueCallback;
    }

}
