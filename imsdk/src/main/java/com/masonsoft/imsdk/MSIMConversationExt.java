package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMConversation;

public class MSIMConversationExt {

    @NonNull
    private final IMConversation mConversation;

    MSIMConversationExt(@NonNull IMConversation conversation) {
        mConversation = conversation;
    }

    @NonNull
    IMConversation getConversation() {
        return mConversation;
    }

    public boolean isMatched() {
        return mConversation.matched.getOrDefault(IMConstants.FALSE) == IMConstants.TRUE;
    }

    public boolean isNewMessage() {
        return mConversation.newMessage.getOrDefault(IMConstants.FALSE) == IMConstants.TRUE;
    }

    public boolean isMyMove() {
        return mConversation.myMove.getOrDefault(IMConstants.FALSE) == IMConstants.TRUE;
    }

    public boolean isIceBreak() {
        return mConversation.iceBreak.getOrDefault(IMConstants.FALSE) == IMConstants.TRUE;
    }

    public boolean isTipFree() {
        return mConversation.tipFree.getOrDefault(IMConstants.FALSE) == IMConstants.TRUE;
    }

    public boolean isTopAlbum() {
        return mConversation.topAlbum.getOrDefault(IMConstants.FALSE) == IMConstants.TRUE;
    }

    public boolean isIBlockU() {
        return mConversation.iBlockU.getOrDefault(IMConstants.FALSE) == IMConstants.TRUE;
    }

    public boolean isConnected() {
        return mConversation.connected.getOrDefault(IMConstants.FALSE) == IMConstants.TRUE;
    }

}
