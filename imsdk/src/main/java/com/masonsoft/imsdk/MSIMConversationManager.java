package com.masonsoft.imsdk;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.lang.GeneralResult;

import io.github.idonans.core.Singleton;

/**
 * @since 1.0
 */
public class MSIMConversationManager {

    private static final Singleton<MSIMConversationManager> INSTANCE = new Singleton<MSIMConversationManager>() {
        @Override
        protected MSIMConversationManager create() {
            return new MSIMConversationManager();
        }
    };

    static MSIMConversationManager getInstance() {
        return INSTANCE.get();
    }

    public void deleteConversation(MSIMConversation conversation) {
        deleteConversation(conversation, null);
    }

    public void deleteConversation(MSIMConversation conversation, @Nullable MSIMCallback<GeneralResult> callback) {
        IMMessageQueueManager.getInstance().enqueueDeleteConversationActionMessage(conversation.getConversation(), callback);
    }

}
