package com.masonsoft.imsdk.core.message;

import androidx.annotation.NonNull;

public class SessionMessageWrapper {

    private final long mSessionUserId;
    @NonNull
    private final MessageWrapper mMessageWrapper;

    public SessionMessageWrapper(long sessionUserId, @NonNull MessageWrapper messageWrapper) {
        mSessionUserId = sessionUserId;
        mMessageWrapper = messageWrapper;
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    @NonNull
    public MessageWrapper getMessageWrapper() {
        return mMessageWrapper;
    }

    public String toShortString() {
        return String.format("SessionMessageWrapper[sessionUserId:%s, messageWrapper:%s]", mSessionUserId, mMessageWrapper.toShortString());
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

}
