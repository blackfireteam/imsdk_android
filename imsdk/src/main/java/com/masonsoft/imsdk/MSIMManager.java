package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    private String mAppId;
    @NonNull
    private MSIMSdkListener mSdkListener = new MSIMSdkListenerAdapter();

    private MSIMManager() {
    }

    public void initSdk(String appId, @Nullable MSIMSdkListener listener) {
        mAppId = appId;
        mSdkListener = new MSIMSdkListenerProxy(listener);
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
