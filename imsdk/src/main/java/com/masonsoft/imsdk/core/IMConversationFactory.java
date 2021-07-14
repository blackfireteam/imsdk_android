package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.db.Conversation;

public class IMConversationFactory {

    private IMConversationFactory() {
    }

    /**
     * 复制一份具有相同内容的 IMConversation
     */
    @NonNull
    public static IMConversation copy(@NonNull IMConversation input) {
        final IMConversation target = new IMConversation();
        target.apply(input);
        return target;
    }

    @NonNull
    public static IMConversation create(@NonNull Conversation input) {
        final IMConversation target = new IMConversation();
        target._sessionUserId.apply(input._sessionUserId);
        target.id.apply(input.localId);
        target.lastModifyMs.apply(input.localLastModifyMs);
        target.seq.apply(input.localSeq);
        target.type.apply(input.localConversationType);
        target.targetUserId.apply(input.targetUserId);
        target.messageLastRead.apply(input.messageLastRead);
        target.showMessageId.apply(input.localShowMessageId);
        target.unreadCount.apply(input.localUnreadCount);
        target.timeMs.apply(input.localTimeMs);
        target.delete.apply(input.delete);
        target.iBlockU.apply(input.iBlockU);
        return target;
    }

}
