package com.masonsoft.imsdk.uikit;

import androidx.annotation.Nullable;

import io.github.idonans.core.Singleton;

public class IMUIKitComponentManager {

    private static final Singleton<IMUIKitComponentManager> INSTANCE = new Singleton<IMUIKitComponentManager>() {
        @Override
        protected IMUIKitComponentManager create() {
            return new IMUIKitComponentManager();
        }
    };

    public static IMUIKitComponentManager getInstance() {
        return INSTANCE.get();
    }

    public interface OnConversationViewClickListener {
        void onConversationViewClick(long sessionUserId, long conversationId, long targetUserId);
    }

    @Nullable
    private OnConversationViewClickListener mOnConversationViewClickListener;

    private IMUIKitComponentManager() {
    }

    public void setOnConversationViewClickListener(@Nullable OnConversationViewClickListener onConversationViewClickListener) {
        mOnConversationViewClickListener = onConversationViewClickListener;
    }

    @Nullable
    public OnConversationViewClickListener getOnConversationViewClickListener() {
        return mOnConversationViewClickListener;
    }

}
