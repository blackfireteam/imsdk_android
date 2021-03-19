package com.masonsoft.imsdk.core;

import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;

/**
 * 消息上传队列. 从 IdleSendMessage 表中读取需要发送的内容依次处理, 并处理对应的消息响应。
 *
 * @see com.masonsoft.imsdk.core.db.IdleSendingMessage
 * @see com.masonsoft.imsdk.core.db.IdleSendingMessageProvider
 * @since 1.0
 */
public class IMMessageUploadManager {

    private static final Singleton<IMMessageUploadManager> INSTANCE = new Singleton<IMMessageUploadManager>() {
        @Override
        protected IMMessageUploadManager create() {
            return new IMMessageUploadManager();
        }
    };

    public static IMMessageUploadManager getInstance() {
        return INSTANCE.get();
    }

    // TODO
    private final TaskQueue mUploadQueue = new TaskQueue(2);

    private IMMessageUploadManager() {
        // TODO
    }

}
