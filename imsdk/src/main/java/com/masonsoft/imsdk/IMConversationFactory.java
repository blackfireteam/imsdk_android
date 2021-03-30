package com.masonsoft.imsdk;

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
        target.id.apply(input.localId);
        target.lastModifyMs.apply(input.localLastModifyMs);
        target.seq.apply(input.localSeq);
        target.type.apply(input.localConversationType);
        target.targetUserId.apply(input.targetUserId);
        target.showMessageId.apply(input.localShowMessageId);
        target.unreadCount.apply(input.localUnreadCount);
        target.timeMs.apply(input.localTimeMs);
        target.delete.apply(input.localDelete);
        target.matched.apply(input.matched);
        target.newMessage.apply(input.newMessage);
        target.myMove.apply(input.myMove);
        target.iceBreak.apply(input.iceBreak);
        target.tipFree.apply(input.tipFree);
        target.topAlbum.apply(input.topAlbum);
        target.iBlockU.apply(input.iBlockU);
        target.connected.apply(input.connected);
        return target;
    }

}
