package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import io.github.idonans.core.Singleton;

/**
 * @since 1.0
 */
public class MSIMManager {

    private static final Singleton<MSIMManager> INSTANCE = new Singleton<MSIMManager>() {
        @Override
        protected MSIMManager create() {
            return new MSIMManager();
        }
    };

    @NonNull
    public static MSIMManager getInstance() {
        return INSTANCE.get();
    }

    private MSIMManager() {
    }

    @NonNull
    public MSIMMessageManager getMessageManager() {
        return MSIMMessageManager.getInstance();
    }

    @NonNull
    public MSIMConversationManager getConversationManager() {
        return MSIMConversationManager.getInstance();
    }

}
