package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Singleton;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMMessageFactory;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;

/**
 * 处理消息相关内容。
 *
 * @since 1.0
 */
public class IMMessageManager {

    private static final Singleton<IMMessageManager> INSTANCE = new Singleton<IMMessageManager>() {
        @Override
        protected IMMessageManager create() {
            //noinspection InstantiationOfUtilityClass
            return new IMMessageManager();
        }
    };

    @NonNull
    public static IMMessageManager getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private IMMessageManager() {
    }

    @Nullable
    public IMMessage getMessage(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long localMessageId) {
        final Message message = MessageDatabaseProvider.getInstance().getMessage(
                sessionUserId,
                conversationType,
                targetUserId,
                localMessageId);

        // TODO 如果是发送中的消息，查询发送进度？

        if (message != null) {
            return IMMessageFactory.create(message);
        }
        return null;
    }

}
