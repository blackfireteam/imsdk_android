package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;
import com.masonsoft.imsdk.core.message.SessionMessageWrapper;
import com.masonsoft.imsdk.lang.MultiProcessor;

/**
 * 消息收发队列
 *
 * @since 1.0
 */
public class IMMessageQueueManager {

    private static final Singleton<IMMessageQueueManager> INSTANCE = new Singleton<IMMessageQueueManager>() {
        @Override
        protected IMMessageQueueManager create() {
            return new IMMessageQueueManager();
        }
    };

    @NonNull
    public static IMMessageQueueManager getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    // 处理服务器下发的消息
    private final MultiProcessor<SessionMessageWrapper> mReceivedMessageProcessor = new MultiProcessor<>();
    private final TaskQueue mReceivedMessageQueue = new TaskQueue(1);

    private IMMessageQueueManager() {
    }

    @NonNull
    public MultiProcessor<SessionMessageWrapper> getReceivedMessageProcessor() {
        return mReceivedMessageProcessor;
    }

    /**
     * 收到服务器下发的消息
     */
    public void enqueueReceivedMessage(@NonNull SessionMessageWrapper sessionMessageWrapper) {
        mReceivedMessageQueue.enqueue(new ReceivedMessageTask(sessionMessageWrapper));
    }

    private class ReceivedMessageTask implements Runnable {

        @NonNull
        private final SessionMessageWrapper mSessionMessageWrapper;

        private ReceivedMessageTask(@NonNull SessionMessageWrapper sessionMessageWrapper) {
            mSessionMessageWrapper = sessionMessageWrapper;
        }

        @Override
        public void run() {
            try {
                if (!mReceivedMessageProcessor.doProcess(mSessionMessageWrapper)) {
                    throw new IllegalAccessError("ReceivedMessageTask SessionMessageWrapper do process fail");
                }
            } catch (Throwable e) {
                IMLog.v(e, "SessionMessageWrapper:%s", mSessionMessageWrapper.toShortString());
            }
        }
    }

}
