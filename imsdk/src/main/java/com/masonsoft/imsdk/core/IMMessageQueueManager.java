package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMSessionMessage;
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
    private final MultiProcessor<IMSessionMessage> mSendMessageProcessor = new MultiProcessor<>();
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
                    throw new IllegalAccessError("ReceivedMessageTask SessionProtoByteMessageWrapper do process fail");
                }
            } catch (Throwable e) {
                IMLog.v(e, "SessionProtoByteMessageWrapper:%s", mSessionProtoByteMessageWrapper.toShortString());
                if (RuntimeMode.isDebug()) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @NonNull
    public MultiProcessor<IMSessionMessage> getSendMessageProcessor() {
        return mSendMessageProcessor;
    }

    /**
     * 本地发送新消息或重发一个失败的消息
     */
    public void enqueueSendMessage(@NonNull IMMessage imMessage) {
        // sessionUserId 可能是无效值
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        mSendMessageQueue.enqueue(new SendMessageTask(new IMSessionMessage(sessionUserId, imMessage)));
    }

    private class SendMessageTask implements Runnable {

        @NonNull
        private final IMSessionMessage mIMSessionMessage;

        private SendMessageTask(@NonNull IMSessionMessage imSessionMessage) {
            mIMSessionMessage = imSessionMessage;
        }

        @Override
        public void run() {
            try {
                if (!mSendMessageProcessor.doProcess(mIMSessionMessage)) {
                    throw new IllegalAccessError("SendMessageTask IMSessionMessage do process fail");
                }
            } catch (Throwable e) {
                IMLog.v(e, "IMSessionMessage:%s", mIMSessionMessage.toShortString());
                if (RuntimeMode.isDebug()) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
