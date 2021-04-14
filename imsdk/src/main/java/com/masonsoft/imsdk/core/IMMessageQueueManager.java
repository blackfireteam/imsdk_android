package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMMessageFactory;
import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.processor.InternalReceivedProtoMessageProtoTypeProcessor;
import com.masonsoft.imsdk.core.processor.InternalSendSessionMessageTypeValidateProcessor;
import com.masonsoft.imsdk.core.processor.ReceivedProtoMessageConversationListProcessor;
import com.masonsoft.imsdk.core.processor.ReceivedProtoMessageResultIgnoreProcessor;
import com.masonsoft.imsdk.core.processor.ReceivedProtoMessageSessionProcessor;
import com.masonsoft.imsdk.core.processor.ReceivedProtoMessageUploadResponseProcessor;
import com.masonsoft.imsdk.core.processor.SendSessionMessageRecoveryProcessor;
import com.masonsoft.imsdk.core.processor.SendSessionMessageWriteDatabaseProcessor;
import com.masonsoft.imsdk.lang.MultiProcessor;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.TaskQueue;

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
    // 处理本地发送的消息, 消息入库之后交由消息上传队列处理
    private final MultiProcessor<IMSessionMessage> mSendMessageProcessor = new MultiProcessor<>();
    private final TaskQueue mSendMessageQueue = new TaskQueue(1);
    ///////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////

    private IMMessageQueueManager() {
        mReceivedMessageProcessor.addFirstProcessor(new ReceivedProtoMessageSessionProcessor());
        mReceivedMessageProcessor.addLastProcessor(new ReceivedProtoMessageResultIgnoreProcessor());
        mReceivedMessageProcessor.addLastProcessor(new ReceivedProtoMessageConversationListProcessor());
        mReceivedMessageProcessor.addLastProcessor(new InternalReceivedProtoMessageProtoTypeProcessor());
        mReceivedMessageProcessor.addLastProcessor(new ReceivedProtoMessageUploadResponseProcessor());

        mSendMessageProcessor.addFirstProcessor(new SendSessionMessageRecoveryProcessor());
        mSendMessageProcessor.addLastProcessor(new InternalSendSessionMessageTypeValidateProcessor());
        mSendMessageProcessor.addLastProcessor(new SendSessionMessageWriteDatabaseProcessor());
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
                    Throwable e = new IllegalAccessError("ReceivedMessageTask SessionProtoByteMessageWrapper do process fail " + mSessionProtoByteMessageWrapper.toShortString());
                    IMLog.v(e);
                }
            } catch (Throwable e) {
                IMLog.e(e, "SessionProtoByteMessageWrapper:%s", mSessionProtoByteMessageWrapper.toShortString());
                RuntimeMode.throwIfDebug(e);
            }
        }
    }

    @NonNull
    public MultiProcessor<IMSessionMessage> getSendMessageProcessor() {
        return mSendMessageProcessor;
    }

    /**
     * 本地重发一个失败的消息
     */
    public void enqueueResendMessage(@NonNull IMMessage imMessage) {
        this.enqueueResendMessage(imMessage, new IMSessionMessage.EnqueueCallbackAdapter());
    }

    /**
     * 本地重发一个失败的消息
     */
    public void enqueueResendMessage(@NonNull IMMessage imMessage, @NonNull IMSessionMessage.EnqueueCallback enqueueCallback) {
        this.enqueueSendMessage(imMessage, 0, true, enqueueCallback);
    }

    /**
     * 本地发送新消息
     */
    public void enqueueSendMessage(@NonNull IMMessage imMessage, long toUserId, @NonNull IMSessionMessage.EnqueueCallback enqueueCallback) {
        this.enqueueSendMessage(imMessage, toUserId, false, enqueueCallback);
    }

    private void enqueueSendMessage(@NonNull IMMessage imMessage, long toUserId, boolean resend, @NonNull IMSessionMessage.EnqueueCallback enqueueCallback) {
        // sessionUserId 可能是无效值
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        mSendMessageQueue.enqueue(
                new SendMessageTask(
                        new IMSessionMessage(
                                sessionUserId,
                                toUserId,
                                resend,
                                IMMessageFactory.copy(imMessage),
                                enqueueCallback
                        )
                )
        );
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
                    Throwable e = new IllegalAccessError("SendMessageTask IMSessionMessage do process fail");
                    IMLog.v(e);

                    mIMSessionMessage.getEnqueueCallback().onEnqueueFail(
                            mIMSessionMessage,
                            IMSessionMessage.EnqueueCallback.ERROR_CODE_UNKNOWN,
                            I18nResources.getString(R.string.msimsdk_enqueue_callback_error_unknown)
                    );
                }
            } catch (Throwable e) {
                IMLog.e(e, "IMSessionMessage:%s", mIMSessionMessage.toShortString());
                RuntimeMode.throwIfDebug(e);

                mIMSessionMessage.getEnqueueCallback().onEnqueueFail(
                        mIMSessionMessage,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_UNKNOWN,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_unknown)
                );
            }
        }
    }

}
