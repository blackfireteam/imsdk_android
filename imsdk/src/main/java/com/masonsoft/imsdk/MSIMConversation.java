package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMConversation;

/**
 * @since 1.0
 */
public class MSIMConversation {

    @NonNull
    private final IMConversation mConversation;
    @NonNull
    private final MSIMConversationExt mExt;

    MSIMConversation(@NonNull IMConversation conversation) {
        mConversation = conversation;
        mExt = new MSIMConversationExt(conversation);
    }

    @NonNull
    IMConversation getConversation() {
        return mConversation;
    }

    public long getConversationId() {
        return getConversationId(0L);
    }

    public long getConversationId(long defaultValue) {
        return mConversation.id.getOrDefault(defaultValue);
    }

    public long getSeq() {
        return getSeq(0L);
    }

    public long getSeq(long defaultValue) {
        return mConversation.seq.getOrDefault(defaultValue);
    }

    public long getTargetUserId() {
        return getTargetUserId(0L);
    }

    public long getTargetUserId(long defaultValue) {
        return mConversation.targetUserId.getOrDefault(defaultValue);
    }

    public long getShowMessageId() {
        return getShowMessageId(0L);
    }

    public long getShowMessageId(long defaultValue) {
        return mConversation.showMessageId.getOrDefault(defaultValue);
    }

    public long getTimeMs() {
        return getTimeMs(0L);
    }

    public long getTimeMs(long defaultValue) {
        return mConversation.timeMs.getOrDefault(defaultValue);
    }

    public boolean isDelete() {
        return mConversation.delete.getOrDefault(IMConstants.FALSE) == IMConstants.TRUE;
    }

    @NonNull
    public MSIMConversationExt getExt() {
        return mExt;
    }

}
