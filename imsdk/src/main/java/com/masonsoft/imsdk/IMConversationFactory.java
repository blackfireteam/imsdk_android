package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

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

}
