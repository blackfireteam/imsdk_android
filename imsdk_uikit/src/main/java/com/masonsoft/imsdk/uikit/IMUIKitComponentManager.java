package com.masonsoft.imsdk.uikit;

import android.app.Activity;

import androidx.annotation.NonNull;
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
        void onConversationViewClick(@NonNull Activity activity, long sessionUserId, long conversationId, long targetUserId);
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

    public void dispatchConversationViewClick(@NonNull Activity activity, long sessionUserId, long conversationId, long targetUserId) {
        final OnConversationViewClickListener listener = mOnConversationViewClickListener;
        if (listener != null) {
            listener.onConversationViewClick(activity, sessionUserId, conversationId, targetUserId);
        }
    }

}
