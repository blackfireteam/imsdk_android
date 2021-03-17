package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

public class IMSessionMessage {

    private final long mSessionUserId;
    @NonNull
    private final IMMessage mIMMessage;

    public IMSessionMessage(long sessionUserId, @NonNull IMMessage imMessage) {
        mSessionUserId = sessionUserId;
        mIMMessage = imMessage;
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    @NonNull
    public IMMessage getIMMessage() {
        return mIMMessage;
    }

    @NonNull
    public String toShortString() {
        return "IMSessionMessage sessionUserId:" + this.mSessionUserId + ", " + this.mIMMessage.toShortString();
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

}
