package com.masonsoft.imsdk.uikit;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.idonans.core.Singleton;

public class MSIMUikitComponentManager {

    private static final Singleton<MSIMUikitComponentManager> INSTANCE = new Singleton<MSIMUikitComponentManager>() {
        @Override
        protected MSIMUikitComponentManager create() {
            return new MSIMUikitComponentManager();
        }
    };

    public static MSIMUikitComponentManager getInstance() {
        return INSTANCE.get();
    }

    public interface OnConversationViewClickListener {
        void onConversationViewClick(@NonNull Activity activity, long sessionUserId, long conversationId, long targetUserId);
    }

    @Nullable
    private OnConversationViewClickListener mOnConversationViewClickListener;

    private MSIMUikitComponentManager() {
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
