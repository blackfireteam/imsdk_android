package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.EnqueueCallback;
import com.masonsoft.imsdk.EnqueueCallbackAdapter;
import com.masonsoft.imsdk.IMActionMessage;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMMessageFactory;
import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.processor.InternalReceivedProtoMessageProtoTypeProcessor;
import com.masonsoft.imsdk.core.processor.InternalSendSessionMessageTypeValidateProcessor;
import com.masonsoft.imsdk.core.processor.ReceivedProtoMessageConversationListProcessor;
import com.masonsoft.imsdk.core.processor.ReceivedProtoMessageOtherMessageResponseProcessor;
import com.masonsoft.imsdk.core.processor.ReceivedProtoMessageResultIgnoreProcessor;
import com.masonsoft.imsdk.core.processor.ReceivedProtoMessageSessionMessageResponseProcessor;
import com.masonsoft.imsdk.core.processor.ReceivedProtoMessageSessionProcessor;
import com.masonsoft.imsdk.core.processor.SendActionTypeRevokeValidateProcessor;
import com.masonsoft.imsdk.core.processor.SendSessionMessageRecoveryProcessor;
import com.masonsoft.imsdk.core.processor.SendSessionMessageWriteDatabaseProcessor;
import com.masonsoft.imsdk.lang.MultiProcessor;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.TaskQueue;
import io.github.idonans.core.thread.Threads;

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
    // 处理本地发送的会话消息, 消息入库之后交由 IMSessionMessageUploadManager 处理
    private final MultiProcessor<IMSessionMessage> mSendSessionMessageProcessor = new MultiProcessor<>();
    private final TaskQueue mSendSessionMessageQueue = new TaskQueue(1);
    ///////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////
    // 处理本地发送的指令消息, 消息入对之后交由 IMActionMessageManager 处理
    private final MultiProcessor<IMActionMessage> mSendActionMessageProcessor = new MultiProcessor<>();
    private final TaskQueue mSendActionMessageQueue = new TaskQueue(1);
    ///////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////

    private IMMessageQueueManager() {
        mReceivedMessageProcessor.addFirstProcessor(new ReceivedProtoMessageSessionProcessor());
        mReceivedMessageProcessor.addLastProcessor(new ReceivedProtoMessageResultIgnoreProcessor());
        mReceivedMessageProcessor.addLastProcessor(new ReceivedProtoMessageConversationListProcessor());
        mReceivedMessageProcessor.addLastProcessor(new InternalReceivedProtoMessageProtoTypeProcessor());
        mReceivedMessageProcessor.addLastProcessor(new ReceivedProtoMessageSessionMessageResponseProcessor());
        mReceivedMessageProcessor.addLastProcessor(new ReceivedProtoMessageOtherMessageResponseProcessor());

        mSendSessionMessageProcessor.addFirstProcessor(new SendSessionMessageRecoveryProcessor());
        mSendSessionMessageProcessor.addLastProcessor(new InternalSendSessionMessageTypeValidateProcessor());
        mSendSessionMessageProcessor.addLastProcessor(new SendSessionMessageWriteDatabaseProcessor());

        mSendActionMessageProcessor.addLastProcessor(new SendActionTypeRevokeValidateProcessor());

        Threads.postBackground(() -> {
            IMManager.getInstance().attach();
        });
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
    public MultiProcessor<IMSessionMessage> getSendSessionMessageProcessor() {
        return mSendSessionMessageProcessor;
    }

    /**
     * 本地重发一个失败的消息
     */
    public void enqueueResendSessionMessage(@NonNull IMMessage imMessage) {
        this.enqueueResendSessionMessage(imMessage, new EnqueueCallbackAdapter<>());
    }

    /**
     * 本地重发一个失败的消息
     */
    public void enqueueResendSessionMessage(@NonNull IMMessage imMessage, @NonNull EnqueueCallback<IMSessionMessage> enqueueCallback) {
        this.enqueueSendSessionMessage(imMessage, 0, true, enqueueCallback);
    }

    /**
     * 本地发送新消息
     */
    public void enqueueSendSessionMessage(@NonNull IMMessage imMessage, long toUserId, @NonNull EnqueueCallback<IMSessionMessage> enqueueCallback) {
        this.enqueueSendSessionMessage(imMessage, toUserId, false, enqueueCallback);
    }

    private void enqueueSendSessionMessage(@NonNull IMMessage imMessage, long toUserId, boolean resend, @NonNull EnqueueCallback<IMSessionMessage> enqueueCallback) {
        // sessionUserId 可能是无效值
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        mSendSessionMessageQueue.enqueue(
                new SendSessionMessageTask(
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

    private class SendSessionMessageTask implements Runnable {

        @NonNull
        private final IMSessionMessage mIMSessionMessage;

        private SendSessionMessageTask(@NonNull IMSessionMessage imSessionMessage) {
            mIMSessionMessage = imSessionMessage;
        }

        @Override
        public void run() {
            try {
                if (!mSendSessionMessageProcessor.doProcess(mIMSessionMessage)) {
                    Throwable e = new IllegalAccessError("SendMessageTask IMSessionMessage do process fail");
                    IMLog.v(e);

                    mIMSessionMessage.getEnqueueCallback().onEnqueueFail(
                            mIMSessionMessage,
                            EnqueueCallback.ERROR_CODE_UNKNOWN,
                            I18nResources.getString(R.string.msimsdk_enqueue_callback_error_unknown)
                    );
                }
            } catch (Throwable e) {
                IMLog.e(e, "IMSessionMessage:%s", mIMSessionMessage.toShortString());
                RuntimeMode.throwIfDebug(e);

                mIMSessionMessage.getEnqueueCallback().onEnqueueFail(
                        mIMSessionMessage,
                        EnqueueCallback.ERROR_CODE_UNKNOWN,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_unknown)
                );
            }
        }
    }

    public MultiProcessor<IMActionMessage> getSendActionMessageProcessor() {
        return mSendActionMessageProcessor;
    }

    /**
     * 撤回指定会话消息(聊天消息)
     */
    public void enqueueRevokeActionMessage(@NonNull IMMessage message) {
        this.enqueueSendActionMessage(IMActionMessage.ACTION_TYPE_REVOKE, message);
    }

    /**
     * 发送指令消息
     */
    public void enqueueSendActionMessage(int actionType, Object actionObject) {
        this.enqueueSendActionMessage(actionType, actionObject, new EnqueueCallbackAdapter<>());
    }

    /**
     * 发送指令消息
     */
    public void enqueueSendActionMessage(int actionType, Object actionObject, @NonNull EnqueueCallback<IMActionMessage> enqueueCallback) {
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        mSendActionMessageQueue.enqueue(new SendActionMessageTask(
                new IMActionMessage(
                        sessionUserId,
                        actionType,
                        actionObject,
                        enqueueCallback
                )
        ));
    }

    private class SendActionMessageTask implements Runnable {

        @NonNull
        private final IMActionMessage mActionMessage;

        private SendActionMessageTask(@NonNull IMActionMessage actionMessage) {
            mActionMessage = actionMessage;
        }

        @Override
        public void run() {
            try {
                if (!mSendActionMessageProcessor.doProcess(mActionMessage)) {
                    Throwable e = new IllegalAccessError("SendActionMessageTask mActionMessage do process fail");
                    IMLog.v(e);

                    mActionMessage.getEnqueueCallback().onEnqueueFail(
                            mActionMessage,
                            EnqueueCallback.ERROR_CODE_UNKNOWN,
                            I18nResources.getString(R.string.msimsdk_enqueue_callback_error_unknown)
                    );
                }
            } catch (Throwable e) {
                IMLog.e(e, "mActionMessage:%s", mActionMessage.toShortString());
                RuntimeMode.throwIfDebug(e);

                mActionMessage.getEnqueueCallback().onEnqueueFail(
                        mActionMessage,
                        EnqueueCallback.ERROR_CODE_UNKNOWN,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_unknown)
                );
            }
        }
    }

}
