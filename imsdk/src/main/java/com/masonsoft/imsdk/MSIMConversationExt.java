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

}
