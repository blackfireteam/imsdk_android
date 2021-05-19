package com.masonsoft.imsdk;

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

}
