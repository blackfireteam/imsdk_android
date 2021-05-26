package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMMessage;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.lang.GeneralResult;

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

    public void sendMessage(@NonNull MSIMMessage message, long receiver, @Nullable MSIMCallback<GeneralResult> callback) {
        final MSIMCallback<GeneralResult> proxy = new MSIMCallbackProxy<>(callback);
        final IMMessage m = message.getMessage();
        if (m == null) {
            proxy.onCallback(GeneralResult.valueOf(GeneralResult.ERROR_CODE_TARGET_NOT_FOUND));
            return;
        }
        IMMessageQueueManager.getInstance().enqueueSendSessionMessage(
                message.getMessage(),
                receiver,
                callback
        );
    }

}
