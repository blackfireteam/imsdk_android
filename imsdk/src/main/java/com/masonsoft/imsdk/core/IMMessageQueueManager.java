package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
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

    ///////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////
    // 处理服务器下发的消息
    private final MultiProcessor<SessionProtoByteMessageWrapper> mReceivedMessageProcessor = new MultiProcessor<>();
    private final TaskQueue mReceivedMessageQueue = new TaskQueue(1);
    ///////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////
    // 处理本地发送的消息
    // TODO
    private final TaskQueue mSendMessageQueue = new TaskQueue(1);
    ///////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////

    private IMMessageQueueManager() {
    }

    @NonNull
    public MultiProcessor<SessionProtoByteMessageWrapper> getReceivedMessageProcessor() {
        return mReceivedMessageProcessor;
    }

    /**
     * 收到服务器下发的消息
     */
    public void enqueueReceivedMessage(@NonNull SessionProtoByteMessageWrapper sessionProtoByteMessageWrapper) {
        mReceivedMessageQueue.enqueue(new ReceivedMessageTask(sessionProtoByteMessageWrapper));
    }

    private class ReceivedMessageTask implements Runnable {

        @NonNull
        private final SessionProtoByteMessageWrapper mSessionProtoByteMessageWrapper;

        private ReceivedMessageTask(@NonNull SessionProtoByteMessageWrapper sessionProtoByteMessageWrapper) {
            mSessionProtoByteMessageWrapper = sessionProtoByteMessageWrapper;
        }

        @Override
        public void run() {
            try {
                if (!mReceivedMessageProcessor.doProcess(mSessionProtoByteMessageWrapper)) {
                    throw new IllegalAccessError("ReceivedMessageTask SessionMessageWrapper do process fail");
                }
            } catch (Throwable e) {
                IMLog.v(e, "SessionMessageWrapper:%s", mSessionProtoByteMessageWrapper.toShortString());
            }
        }
    }

}
