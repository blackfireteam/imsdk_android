package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;
import com.masonsoft.imsdk.core.db.LocalSendingMessage;
import com.masonsoft.imsdk.core.db.LocalSendingMessageProvider;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.lang.SafetyRunnable;
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息上传队列. 从 LocalSendingMessage 表中读取需要发送的内容依次处理, 并处理对应的消息响应。
 *
 * @see LocalSendingMessage
 * @see LocalSendingMessageProvider
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

    private final Map<Long, SessionUploader> mSessionUploaderMap = new HashMap<>();

    private IMMessageUploadManager() {
    }

    @NonNull
    private SessionUploader getSessionUploader(final long sessionUserId) {
        SessionUploader sessionUploader = mSessionUploaderMap.get(sessionUserId);
        if (sessionUploader != null) {
            return sessionUploader;
        }
        synchronized (mSessionUploaderMap) {
            sessionUploader = mSessionUploaderMap.get(sessionUserId);
            if (sessionUploader == null) {
                sessionUploader = new SessionUploader(sessionUserId);
                mSessionUploaderMap.put(sessionUserId, sessionUploader);
            }
            return sessionUploader;
        }
    }

    /**
     * 通知同步 LocalSendingMessage 表内容，可能添加了新的消息上传发送任务。
     */
    public void notifySyncLocalSendingMessage(final long sessionUserId) {
        getSessionUploader(sessionUserId).dispatchCheckIdleMessage();
    }

    private static class MessageUploadObjectWrapper {
        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
        private static final int FIRST_LOCAL_ERROR_CODE = Integer.MIN_VALUE / 2;
        /**
         * 未知错误
         */
        private static final int ERROR_CODE_UNKNOWN = FIRST_LOCAL_ERROR_CODE;
        private static final int ERROR_CODE_TARGET_NOT_FOUND = FIRST_LOCAL_ERROR_CODE + 1;
        /**
         * 绑定 abort id 失败
         */
        private static final int ERROR_CODE_BIND_ABORT_ID_FAIL = FIRST_LOCAL_ERROR_CODE + 2;
        /**
         * 错误的 send status 状态
         */
        private static final int ERROR_CODE_UNEXPECTED_SEND_STATUS = FIRST_LOCAL_ERROR_CODE + 3;
        /**
         * 更新 sendStatus 失败
         */
        private static final int ERROR_CODE_UPDATE_SEND_STATUS_FAIL = FIRST_LOCAL_ERROR_CODE + 4;
        /**
         * 构建 protoByteMessage 失败
         */
        private static final int ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL = FIRST_LOCAL_ERROR_CODE + 5;
        /**
         * sessionTcpClientProxy 为 null
         */
        private static final int ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL = FIRST_LOCAL_ERROR_CODE + 6;
        /**
         * sessionTcpClientProxy session 无效
         */
        private static final int ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID = FIRST_LOCAL_ERROR_CODE + 7;
        /**
         * sessionTcpClientProxy 链接错误
         */
        private static final int ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR = FIRST_LOCAL_ERROR_CODE + 9;
        /**
         * sessionTcpClientProxy 未知错误
         */
        private static final int ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN = FIRST_LOCAL_ERROR_CODE + 10;
        /**
         * messagePacket 发送失败
         */
        private static final int ERROR_CODE_MESSAGE_PACKET_SEND_FAIL = FIRST_LOCAL_ERROR_CODE + 11;

        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
        private final long mSessionUserId;
        private final long mSign;
        private final long mAbortId;
        @NonNull
        private LocalSendingMessage mLocalSendingMessage;
        @Nullable
        private Message mMessage;

        /**
         * abort id 与数据库中对应记录不相等.
         */
        private boolean mAbortIdNotMatch;

        /**
         * 消息的发送进度
         */
        public float mSendProgress = 0f;

        public int mErrorCode;
        public String mErrorMessage;

        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////

        public MessageUploadObjectWrapper(long sessionUserId, long sign, long abortId, @NonNull LocalSendingMessage localSendingMessage) {
            this.mSessionUserId = sessionUserId;
            this.mSign = sign;
            this.mAbortId = abortId;
            this.mLocalSendingMessage = localSendingMessage;
        }

        private boolean canContinue() {
            return this.mErrorCode == 0 && !this.mAbortIdNotMatch;
        }

        private void setError(int errorCode, String errorMessage) {
            this.mErrorCode = errorCode;
            this.mErrorMessage = errorMessage;
        }

        @Nullable
        private MessagePacket buildMessagePacket() {
            final Message message = this.mMessage;
            if (message == null) {
                return null;
            }

            // 将消息预处理之后构建为 proto buf
            final int messageType = message.messageType.get();
            if (messageType == IMConstants.MessageType.TEXT) {
                final ProtoMessage.ChatS chatS = ProtoMessage.ChatS.newBuilder()
                        .setBody(message.body.get())
                        .build();
                final ProtoByteMessage protoByteMessage = ProtoByteMessage.Type.encode(chatS);
                // TODO protoByteMessage -> MessagePacket ?
                return null;
            } else {
                final Throwable e = new IllegalAccessError("unknown message type:" + messageType + " " + message);
                IMLog.e(e);
                return null;
            }
        }

        private boolean isFastMessage() {
            final Message message = mMessage;
            if (message == null) {
                return false;
            }
            if (message.messageType.isUnset()) {
                return false;
            }
            final int messageType = message.messageType.get();
            if (messageType == IMConstants.MessageType.TEXT) {
                return true;
            }
            return false;
        }

        /**
         * 上传任务执行结束。如果任务执行成功，则从上传表中的删除。否则设置为上传失败。
         */
        private void onTaskEnd() {
            final LocalSendingMessage localSendingMessage = mLocalSendingMessage;
            if (localSendingMessage.localSendStatus.get() != IMConstants.SendStatus.SUCCESS) {
                moveSendStatus(IMConstants.SendStatus.FAIL);
            } else {
                LocalSendingMessageProvider.getInstance().removeLocalSendingMessage(
                        mSessionUserId,
                        localSendingMessage.localId.get());
            }
        }

        /**
         * 绑定消息上传任务的 abort id
         */
        private void bindAbortId() {
            try {
                final LocalSendingMessage localSendingMessageUpdate = new LocalSendingMessage();
                localSendingMessageUpdate.localId.set(this.mLocalSendingMessage.localId.get());
                localSendingMessageUpdate.localAbortId.set(this.mAbortId);

                if (LocalSendingMessageProvider.getInstance().updateLocalSendingMessage(
                        this.mSessionUserId,
                        localSendingMessageUpdate)) {
                    final LocalSendingMessage localSendingMessage = LocalSendingMessageProvider.getInstance().getLocalSendingMessage(
                            this.mSessionUserId,
                            this.mLocalSendingMessage.localId.get()
                    );
                    if (localSendingMessage != null) {
                        this.mLocalSendingMessage = localSendingMessage;
                    } else {
                        setError(ERROR_CODE_TARGET_NOT_FOUND, null);
                    }
                } else {
                    setError(ERROR_CODE_BIND_ABORT_ID_FAIL, null);
                }
            } catch (Throwable e) {
                IMLog.e(e);
                setError(ERROR_CODE_UNKNOWN, null);
            }
        }

        @Nullable
        private LocalSendingMessage validateAbortIdMatch() {
            if (this.mAbortIdNotMatch) {
                return null;
            }
            LocalSendingMessage localSendingMessage = LocalSendingMessageProvider.getInstance().getLocalSendingMessage(
                    this.mSessionUserId,
                    this.mLocalSendingMessage.localId.get()
            );
            if (localSendingMessage == null) {
                setError(ERROR_CODE_TARGET_NOT_FOUND, null);
                this.mAbortIdNotMatch = true;
                return null;
            }
            this.mLocalSendingMessage = localSendingMessage;

            if (localSendingMessage.localAbortId.get() != mAbortId) {
                IMLog.e("abort id changed. %s : %s", localSendingMessage.localAbortId.get(), mAbortId);
                this.mAbortIdNotMatch = true;
                return localSendingMessage;
            }
            this.mAbortIdNotMatch = false;
            return localSendingMessage;
        }

        private void moveSendStatus(int sendStatus) {
            try {
                LocalSendingMessage localSendingMessage = validateAbortIdMatch();
                if (this.mAbortIdNotMatch) {
                    return;
                }

                final LocalSendingMessage localSendingMessageUpdate = new LocalSendingMessage();
                localSendingMessageUpdate.localId.set(this.mLocalSendingMessage.localId.get());
                localSendingMessageUpdate.localSendStatus.set(sendStatus);
                if (LocalSendingMessageProvider.getInstance().updateLocalSendingMessage(this.mSessionUserId, localSendingMessageUpdate)) {
                    localSendingMessage = LocalSendingMessageProvider.getInstance().getLocalSendingMessage(
                            this.mSessionUserId,
                            this.mLocalSendingMessage.localId.get()
                    );
                    if (localSendingMessage != null) {
                        this.mLocalSendingMessage = localSendingMessage;
                    } else {
                        setError(ERROR_CODE_TARGET_NOT_FOUND, null);
                    }
                } else {
                    setError(ERROR_CODE_UPDATE_SEND_STATUS_FAIL, null);
                }
            } catch (Throwable e) {
                IMLog.e(e);
                setError(ERROR_CODE_UNKNOWN, null);
            }
        }
    }

    private class SessionUploader {

        private final long mSessionUserId;

        /**
         * 所有正在执行的消息任务数量上限
         */
        private static final int MAX_RUNNING_SIZE = 10;
        /**
         * 所有正在执行的消息任务
         */
        private final List<MessageUploadObjectWrapperTask> mAllRunningTasks = new ArrayList<>();

        private final TaskQueue mCheckIdleActionQueue = new TaskQueue(1);

        // 需要预处理上传数据的任务队列，如视频，音频，图片。通常是先用 http 协议上传预处理数据，再通过 tcp proto buf 发送消息
        private final TaskQueue mLongTimeTaskQueue = new TaskQueue(2);

        // 不需要预处理的任务队列。通常可以直接通过 tcp proto buf 发送消息
        private final TaskQueue mShortTimeTaskQueue = new TaskQueue(1);

        private SessionUploader(long sessionUserId) {
            mSessionUserId = sessionUserId;
            LocalSendingMessageProvider.getInstance().updateMessageToFailIfNotSuccess(mSessionUserId);
            LocalSendingMessageProvider.getInstance().removeAllSuccessMessage(mSessionUserId);
        }

        @Nullable
        private MessageUploadObjectWrapperTask getTask(@NonNull LocalSendingMessage localSendingMessage) {
            final long localId = localSendingMessage.localId.get();
            for (MessageUploadObjectWrapperTask task : mAllRunningTasks) {
                if (localId == task.mMessageUploadObjectWrapper.mLocalSendingMessage.localId.get()) {
                    return task;
                }
            }
            return null;
        }

        @Nullable
        private MessageUploadObjectWrapperTask removeTask(@NonNull LocalSendingMessage localSendingMessage) {
            final long localId = localSendingMessage.localId.get();
            for (int i = 0; i < mAllRunningTasks.size(); i++) {
                final MessageUploadObjectWrapperTask task = mAllRunningTasks.get(i);
                if (localId == task.mMessageUploadObjectWrapper.mLocalSendingMessage.localId.get()) {
                    return mAllRunningTasks.remove(i);
                }
            }
            return null;
        }

        private void dispatchCheckIdleMessage() {
            if (mCheckIdleActionQueue.getWaitCount() > 5) {
                // 不直接判断 > 0, 做一些冗余，此处不做锁。
                return;
            }
            mCheckIdleActionQueue.skipQueue();
            mCheckIdleActionQueue.enqueue(new SafetyRunnable(() -> {
                if (mAllRunningTasks.size() >= MAX_RUNNING_SIZE) {
                    IMLog.v("ignore, already has many running task size:%s", mAllRunningTasks.size());
                    return;
                }

                final List<LocalSendingMessage> localSendingMessageList =
                        LocalSendingMessageProvider
                                .getInstance()
                                .getIdleMessageList(mSessionUserId, MAX_RUNNING_SIZE);

                synchronized (mAllRunningTasks) {
                    for (LocalSendingMessage localSendingMessage : localSendingMessageList) {
                        final MessageUploadObjectWrapperTask oldTask = getTask(localSendingMessage);
                        if (oldTask != null) {
                            final Throwable e = new IllegalAccessError("unexpected localSendingMessage already exists in task. "
                                    + localSendingMessage + ", oldTask:" + Objects.defaultObjectTag(oldTask));
                            IMLog.e(e);
                            continue;
                        }

                        IMLog.v("found new idle localSendingMessage %s", localSendingMessage);
                        final long sign = SignGenerator.next();
                        final MessageUploadObjectWrapper wrapper = new MessageUploadObjectWrapper(
                                mSessionUserId,
                                sign,
                                sign,
                                localSendingMessage
                        );
                        wrapper.mMessage = MessageDatabaseProvider.getInstance().getMessage(
                                mSessionUserId,
                                localSendingMessage.conversationType.get(),
                                localSendingMessage.targetUserId.get(),
                                localSendingMessage.messageLocalId.get()
                        );
                        wrapper.bindAbortId();

                        final MessageUploadObjectWrapperTask task = new MessageUploadObjectWrapperTask(wrapper) {
                            @Override
                            public void run() {
                                try {
                                    super.run();
                                } catch (Throwable e) {
                                    IMLog.e(e, "unexpected");
                                }
                                wrapper.onTaskEnd();
                                synchronized (mAllRunningTasks) {
                                    final MessageUploadObjectWrapperTask existsTask = removeTask(localSendingMessage);
                                    if (existsTask == null) {
                                        IMLog.e("unexpected removeTask return null %s", localSendingMessage);
                                    } else if (existsTask != this) {
                                        IMLog.e("unexpected removeTask return another value %s", localSendingMessage);
                                    } else {
                                        IMLog.v("success remove task %s", localSendingMessage);
                                    }
                                }
                                dispatchCheckIdleMessage();
                            }
                        };
                        mAllRunningTasks.add(task);
                        if (wrapper.isFastMessage()) {
                            mShortTimeTaskQueue.enqueue(task);
                        } else {
                            mLongTimeTaskQueue.enqueue(task);
                        }
                    }
                }

                IMLog.v("checkIdleActionQueue run localSendingMessageList size:%s, allRunningTasks size:%s", localSendingMessageList.size(), mAllRunningTasks.size());
            }));
        }

        private class MessageUploadObjectWrapperTask implements Runnable {

            @NonNull
            private final MessageUploadObjectWrapper mMessageUploadObjectWrapper;

            private MessageUploadObjectWrapperTask(MessageUploadObjectWrapper messageUploadObjectWrapper) {
                mMessageUploadObjectWrapper = messageUploadObjectWrapper;
            }

            @Override
            public void run() {
                try {
                    if (!mMessageUploadObjectWrapper.canContinue()) {
                        return;
                    }

                    mMessageUploadObjectWrapper.moveSendStatus(IMConstants.SendStatus.SENDING);
                    if (!mMessageUploadObjectWrapper.canContinue()) {
                        return;
                    }

                    final Message message = mMessageUploadObjectWrapper.mMessage;
                    if (message == null) {
                        mMessageUploadObjectWrapper.setError(MessageUploadObjectWrapper.ERROR_CODE_TARGET_NOT_FOUND, null);
                        return;
                    }

                    final MessagePacket messagePacket = mMessageUploadObjectWrapper.buildMessagePacket();
                    if (messagePacket == null) {
                        mMessageUploadObjectWrapper.setError(MessageUploadObjectWrapper.ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL, null);
                        return;
                    }

                    // 通过长连接发送 proto buf
                    final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxy();
                    if (proxy == null) {
                        mMessageUploadObjectWrapper.setError(MessageUploadObjectWrapper.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL, null);
                        return;
                    }

                    if (IMSessionManager.getInstance().getSessionUserId() != mSessionUserId) {
                        mMessageUploadObjectWrapper.setError(MessageUploadObjectWrapper.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID, null);
                        return;
                    }

                    if (!proxy.isOnline()) {
                        mMessageUploadObjectWrapper.setError(MessageUploadObjectWrapper.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR, null);
                        return;
                    }

                    final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
                    if (sessionTcpClient == null) {
                        mMessageUploadObjectWrapper.setError(MessageUploadObjectWrapper.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN, null);
                        return;
                    }

                    if (!mMessageUploadObjectWrapper.canContinue()) {
                        return;
                    }

                    sessionTcpClient.sendMessagePacketQuietly(messagePacket);
                    if (messagePacket.getState() != MessagePacket.STATE_WAIT_RESULT) {
                        mMessageUploadObjectWrapper.setError(MessageUploadObjectWrapper.ERROR_CODE_MESSAGE_PACKET_SEND_FAIL, null);
                    }
                } catch (Throwable e) {
                    IMLog.e(e);
                    mMessageUploadObjectWrapper.setError(MessageUploadObjectWrapper.ERROR_CODE_UNKNOWN, null);
                }
            }
        }
    }
}
