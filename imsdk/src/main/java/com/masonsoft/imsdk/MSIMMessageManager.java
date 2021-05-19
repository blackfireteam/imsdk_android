package com.masonsoft.imsdk;

import io.github.idonans.core.Singleton;

/**
 * @since 1.0
 */
public class MSIMMessageManager {

    private static final Singleton<MSIMMessageManager> INSTANCE = new Singleton<MSIMMessageManager>() {
        @Override
        protected MSIMMessageManager create() {
            return new MSIMMessageManager();
        }
    };

    static MSIMMessageManager getInstance() {
        return INSTANCE.get();
    }

    private MSIMMessageManager() {
    }

}
