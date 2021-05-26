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
        return isMatched(false);
    }

    public boolean isMatched(boolean defaultValue) {
        if (mConversation.matched.isUnset()) {
            return defaultValue;
        }
        final Integer matched = mConversation.matched.get();
        if (matched == null) {
            return defaultValue;
        }
        return matched == IMConstants.TRUE;
    }

    public boolean isNewMessage() {
        return isNewMessage(false);
    }

    public boolean isNewMessage(boolean defaultValue) {
        if (mConversation.newMessage.isUnset()) {
            return defaultValue;
        }
        final Integer newMessage = mConversation.newMessage.get();
        if (newMessage == null) {
            return defaultValue;
        }
        return newMessage == IMConstants.TRUE;
    }

    public boolean isMyMove() {
        return isMyMove(false);
    }

    public boolean isMyMove(boolean defaultValue) {
        if (mConversation.myMove.isUnset()) {
            return defaultValue;
        }
        final Integer myMove = mConversation.myMove.get();
        if (myMove == null) {
            return defaultValue;
        }
        return myMove == IMConstants.TRUE;
    }

    public boolean isIceBreak() {
        return isIceBreak(false);
    }

    public boolean isIceBreak(boolean defaultValue) {
        if (mConversation.iceBreak.isUnset()) {
            return defaultValue;
        }
        final Integer iceBreak = mConversation.iceBreak.get();
        if (iceBreak == null) {
            return defaultValue;
        }
        return iceBreak == IMConstants.TRUE;
    }

    public boolean isTipFree() {
        return isTipFree(false);
    }

    public boolean isTipFree(boolean defaultValue) {
        if (mConversation.tipFree.isUnset()) {
            return defaultValue;
        }
        final Integer tipFree = mConversation.tipFree.get();
        if (tipFree == null) {
            return defaultValue;
        }
        return tipFree == IMConstants.TRUE;
    }

    public boolean isTopAlbum() {
        return isTopAlbum(false);
    }

    public boolean isTopAlbum(boolean defaultValue) {
        if (mConversation.topAlbum.isUnset()) {
            return defaultValue;
        }
        final Integer topAlbum = mConversation.topAlbum.get();
        if (topAlbum == null) {
            return defaultValue;
        }
        return topAlbum == IMConstants.TRUE;
    }

    public boolean isIBlockU() {
        return isIBlockU(false);
    }

    public boolean isIBlockU(boolean defaultValue) {
        if (mConversation.iBlockU.isUnset()) {
            return defaultValue;
        }
        final Integer iBlockU = mConversation.iBlockU.get();
        if (iBlockU == null) {
            return defaultValue;
        }
        return iBlockU == IMConstants.TRUE;
    }

    public boolean isConnected() {
        return isConnected(false);
    }

    public boolean isConnected(boolean defaultValue) {
        if (mConversation.connected.isUnset()) {
            return defaultValue;
        }
        final Integer connected = mConversation.connected.get();
        if (connected == null) {
            return defaultValue;
        }
        return connected == IMConstants.TRUE;
    }

}
