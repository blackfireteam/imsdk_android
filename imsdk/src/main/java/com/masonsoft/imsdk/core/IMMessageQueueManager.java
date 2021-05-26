package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.processor.InternalReceivedProtoMessageProtoTypeProcessor;
import com.masonsoft.imsdk.core.processor.InternalSendSessionMessageTypeValidateProcessor;
import com.masonsoft.imsdk.core.processor.ReceivedProtoMessageResultIgnoreProcessor;
import com.masonsoft.imsdk.core.processor.ReceivedProtoMessageSessionProcessor;
import com.masonsoft.imsdk.core.processor.SendActionTypeDeleteConversationValidateProcessor;
import com.masonsoft.imsdk.core.processor.SendActionTypeMarkAsReadValidateProcessor;
import com.masonsoft.imsdk.core.processor.SendActionTypeRevokeValidateProcessor;
import com.masonsoft.imsdk.core.processor.SendSessionMessageRecoveryProcessor;
import com.masonsoft.imsdk.core.processor.SendSessionMessageWriteDatabaseProcessor;
import com.masonsoft.imsdk.lang.GeneralResult;
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
        mReceivedMessageProcessor.addLastProcessor(new InternalReceivedProtoMessageProtoTypeProcessor());

        mSendSessionMessageProcessor.addFirstProcessor(new SendSessionMessageRecoveryProcessor());
        mSendSessionMessageProcessor.addLastProcessor(new InternalSendSessionMessageTypeValidateProcessor());
        mSendSessionMessageProcessor.addLastProcessor(new SendSessionMessageWriteDatabaseProcessor());

        mSendActionMessageProcessor.addLastProcessor(new SendActionTypeRevokeValidateProcessor());
        mSendActionMessageProcessor.addLastProcessor(new SendActionTypeMarkAsReadValidateProcessor());
        mSendActionMessageProcessor.addLastProcessor(new SendActionTypeDeleteConversationValidateProcessor());

        Threads.postBackground(() -> IMManager.getInstance().start());
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
                RuntimeMode.fixme(e);
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
    public void enqueueResendSessionMessage(@NonNull IMMessage message) {
        this.enqueueResendSessionMessage(message, null);
    }

    /**
     * 本地重发一个失败的消息
     */
    public void enqueueResendSessionMessage(@NonNull IMMessage message, @Nullable IMCallback<GeneralResult> enqueueCallback) {
        this.enqueueSendSessionMessage(message, 0, true, enqueueCallback);
    }

    /**
     * 本地发送新消息
     */
    public void enqueueSendSessionMessage(@NonNull IMMessage message, long toUserId, @Nullable IMCallback<GeneralResult> enqueueCallback) {
        this.enqueueSendSessionMessage(message, toUserId, false, enqueueCallback);
    }

    private void enqueueSendSessionMessage(@NonNull IMMessage message, long toUserId, boolean resend, @Nullable IMCallback<GeneralResult> enqueueCallback) {
        // sessionUserId 可能是无效值
        final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
        mSendSessionMessageQueue.enqueue(
                new SendSessionMessageTask(
                        new IMSessionMessage(
                                sessionUserId,
                                toUserId,
                                resend,
                                IMMessageFactory.copy(message),
                                enqueueCallback
                        )
                )
        );
    }

    private class SendSessionMessageTask implements Runnable {

        @NonNull
        private final IMSessionMessage mSessionMessage;

        private SendSessionMessageTask(@NonNull IMSessionMessage imSessionMessage) {
            mSessionMessage = imSessionMessage;
        }

        @Override
        public void run() {
            try {
                if (!mSendSessionMessageProcessor.doProcess(mSessionMessage)) {
                    Throwable e = new IllegalAccessError("SendSessionMessageTask IMSessionMessage do process fail");
                    IMLog.v(e);

                    mSessionMessage.getEnqueueCallback().onCallback(
                            GeneralResult.valueOf(GeneralResult.ERROR_CODE_UNKNOWN)
                                    .withPayload(mSessionMessage)
                    );
                }
            } catch (Throwable e) {
                IMLog.e(e, "IMSessionMessage:%s", mSessionMessage.toShortString());
                RuntimeMode.fixme(e);

                mSessionMessage.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_UNKNOWN)
                                .withPayload(mSessionMessage)
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
    public void enqueueRevokeActionMessage(@NonNull IMMessage message, @Nullable IMCallback<GeneralResult> enqueueCallback) {
        this.enqueueSendActionMessage(IMActionMessage.ACTION_TYPE_REVOKE, message, enqueueCallback);
    }

    /**
     * 回执消息已读
     */
    public void enqueueMarkAsReadActionMessage(long targetUserId, @Nullable IMCallback<GeneralResult> enqueueCallback) {
        this.enqueueSendActionMessage(IMActionMessage.ACTION_TYPE_MARK_AS_READ, targetUserId, enqueueCallback);
    }

    /**
     * 删除会话
     */
    public void enqueueDeleteConversationActionMessage(@NonNull IMConversation conversation, @Nullable IMCallback<GeneralResult> enqueueCallback) {
        this.enqueueSendActionMessage(IMActionMessage.ACTION_TYPE_DELETE_CONVERSATION, conversation, enqueueCallback);
    }

    /**
     * 发送指令消息
     */
    public void enqueueSendActionMessage(int actionType, Object actionObject) {
        this.enqueueSendActionMessage(actionType, actionObject, null);
    }

    /**
     * 发送指令消息
     */
    public void enqueueSendActionMessage(int actionType, Object actionObject, @Nullable IMCallback<GeneralResult> enqueueCallback) {
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

                    mActionMessage.getEnqueueCallback().onCallback(
                            GeneralResult.valueOf(GeneralResult.ERROR_CODE_UNKNOWN)
                                    .withPayload(mActionMessage)
                    );
                }
            } catch (Throwable e) {
                IMLog.e(e, "mActionMessage:%s", mActionMessage.toShortString());
                RuntimeMode.fixme(e);

                mActionMessage.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_UNKNOWN)
                                .withPayload(mActionMessage)
                );
            }
        }
    }

}
