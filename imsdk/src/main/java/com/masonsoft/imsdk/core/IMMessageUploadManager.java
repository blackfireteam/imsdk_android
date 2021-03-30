package com.masonsoft.imsdk.core;

import android.graphics.Bitmap;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;
import com.masonsoft.imsdk.core.db.LocalSendingMessage;
import com.masonsoft.imsdk.core.db.LocalSendingMessageProvider;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.lang.SafetyRunnable;
import com.masonsoft.imsdk.util.Objects;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

        private void setError(int errorCode, String errorMessage) {
            this.mErrorCode = errorCode;
            this.mErrorMessage = errorMessage;
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
                    LocalSendingMessage localSendingMessage = mMessageUploadObjectWrapper.validateAbortIdMatch();
                    if (mMessageUploadObjectWrapper.mAbortIdNotMatch) {
                        return;
                    }
                    mMessageUploadObjectWrapper.moveSendStatus(IMConstants.SendStatus.SENDING);

                    final Message message = mMessageUploadObjectWrapper.mMessage;
                    if (message == null) {
                        mMessageUploadObjectWrapper.setError(MessageUploadObjectWrapper.ERROR_CODE_TARGET_NOT_FOUND, null);
                        return;
                    }

                    // TODO message to proto buf ?
                } catch (Throwable e) {
                    IMLog.e(e);
                    mMessageUploadObjectWrapper.setError(MessageUploadObjectWrapper.ERROR_CODE_UNKNOWN, null);
                }
            }

        }

        private class OneMessageUploadTask extends AbortRunnable {

            private boolean mMessageAbort;

            @NonNull
            private final ImSession mSession;
            private final long mMessageId;

            private Message mLocalMessage;
            private final Progress mProgress = new Progress() {

                private int mLastProgress = -1;

                @Override
                protected void onUpdate() {
                    super.onUpdate();

                    final Message localMessage = mLocalMessage;
                    if (localMessage == null) {
                        Timber.e("ignore progress update, local message is null");
                        return;
                    }

                    final int progress = getPercent();
                    if (mLastProgress == progress) {
                        Timber.v("ignore, progress not changed %s", progress);
                        return;
                    }

                    mLastProgress = progress;
                    LocalEventNoticeManager.getInstance().notifyMessageProgressChanged(
                            mSession.sessionUserId,
                            localMessage.conversationId,
                            localMessage.id,
                            progress
                    );
                }
            };

            private static final long TIME_OUT_MS = 15000; // 15s 超时
            private long mWaitForTcpResponseStartTime; // 等待 tcp protobuf 的消息发送确认开始时间，当 > 0 时有效。

            private OneMessageUploadTask(ImSession session, long messageId) {
                mSession = session;
                mMessageId = messageId;
            }

            /**
             * 设置中断上传
             */
            public void setMessageAbort() {
                mMessageAbort = true;
            }

            @Override
            public boolean isAbort() {
                return mMessageAbort || super.isAbort();
            }

            @Override
            public void onRun() {
                try {
                    throwIfAbort();

                    // 查询当前消息详细信息
                    mLocalMessage = ImMessageDatabaseProvider.getInstance().getTargetMessage(mSession.sessionUserId, mMessageId, null);
                    if (mLocalMessage == null) {
                        throw new IllegalArgumentException("message not found with message id:" + mMessageId);
                    }

                    if (mLocalMessage.sendStatus != ImConstant.MessageSendStatus.IDLE) {
                        throw new IllegalArgumentException("message status is not IDLE " + mLocalMessage.toShortString());
                    }

                    Timber.v("start send LocalMessage %s", mLocalMessage.toShortString());

                    mLocalMessage.sendStatus = ImConstant.MessageSendStatus.SENDING;
                    ImManager.getInstance().postSendMessageAction(new SaveDatabaseTask(mLocalMessage));

                    ProtoMessages.MsgData msgData;
                    switch (mLocalMessage.msgType) {
                        case ImConstant.MessageType.MESSAGE_TYPE_TEXT:
                            msgData = buildTextMessageOrThrow();
                            break;
                        case ImConstant.MessageType.MESSAGE_TYPE_IMAGE:
                            msgData = buildImageMessageOrThrow();
                            break;
                        case ImConstant.MessageType.MESSAGE_TYPE_VOICE:
                            msgData = buildVoiceMessageOrThrow();
                            break;
                        case ImConstant.MessageType.MESSAGE_TYPE_VIDEO:
                            msgData = buildVideoMessageOrThrow();
                            break;
                        case ImConstant.MessageType.MESSAGE_TYPE_LOCATION:
                            msgData = buildLocationMessageOrThrow();
                            break;
                        case ImConstant.MessageType.MESSAGE_TYPE_HEART:
                            msgData = buildHeartMessageOrThrow();
                            break;
                        case ImConstant.MessageType.MESSAGE_TYPE_GIFT:
                            msgData = buildGiftMessageOrThrow();
                            break;
                        default:
                            throw new IllegalArgumentException("unexpected message type " + mLocalMessage.msgType);
                    }

                    throwIfAbort();

                    ProtoMessages.ChatS chatS = ProtoMessages.ChatS.newBuilder()
                            .setSign(mMessageId)
                            .setToUid(mLocalMessage.toUserId)
                            .setType(mLocalMessage.msgType)
                            .setData(msgData)
                            .build();

                    resetWaitForTcpResponseStartTime();

                    if (DEBUG_DETAIL) {
                        Timber.v("[DETAIL] >>> TYPE_CHAT_S %s", chatS);
                    }

                    if (!MessageClientManager.getInstance().sendMessage(
                            new com.xmqvip.xiaomaiquan.common.im.core.Message()
                                    .setType(com.xmqvip.xiaomaiquan.common.im.core.Message.TYPE_CHAT_S)
                                    .setData(chatS.toByteArray())
                    )) {
                        // 消息发送失败，当前不满足消息发送条件。如没有连接或者没有登录成功
                        mLocalMessage.sendStatus = ImConstant.MessageSendStatus.FAIL;
                    }

                    waitForTcpResponseOrTimeout();

                    throwIfAbort();

                    final boolean insertLocalGiftFailTipMessage =
                            mLocalMessage.msgType == ImConstant.MessageType.MESSAGE_TYPE_GIFT
                                    && (mLocalMessage.sendStatus == ImConstant.MessageSendStatus.SENDING
                                    || mLocalMessage.sendStatus == ImConstant.MessageSendStatus.FAIL);
                    if (insertLocalGiftFailTipMessage) {
                        // 礼物消息发送失败
                        Message simpleMessage = new Message();
                        simpleMessage.msgType = ImConstant.MessageType.MESSAGE_TYPE_LOCAL_GIFT_FAIL_TIP;
                        simpleMessage.conversationId = mLocalMessage.conversationId;
                        simpleMessage.fromUserId = mLocalMessage.fromUserId;
                        simpleMessage.toUserId = mLocalMessage.toUserId;
                        simpleMessage.sendStatus = ImConstant.MessageSendStatus.SUCCESS;
                        simpleMessage.readStatus = ImConstant.MessageReadStatus.READ;
                        simpleMessage.revertStatus = ImConstant.MessageRevertStatus.NORMAL;
                        simpleMessage.msgLocalTime = System.currentTimeMillis();
                        simpleMessage.msgServerTime = 0;
                        MessageManager.getInstance().insert(mSession.sessionUserId, simpleMessage);
                    }

                    if (mLocalMessage.sendStatus == ImConstant.MessageSendStatus.FAIL) {
                        Timber.e("send message fail, local message id:%s", mLocalMessage.id);
                    } else if (mLocalMessage.sendStatus == ImConstant.MessageSendStatus.SUCCESS) {
                        Timber.v("send message onSuccess, local message id:%s, server msg id:%s", mLocalMessage.id, mLocalMessage.msgId);
                    } else {
                        Timber.e("send message timeout, local message id:%s", mLocalMessage.id);
                        mLocalMessage.sendStatus = ImConstant.MessageSendStatus.FAIL;
                    }
                    ImManager.getInstance().postSendMessageAction(new SaveDatabaseTask(mLocalMessage));
                } catch (Throwable e) {
                    if (isAbort()) {
                        return;
                    }

                    Timber.e(e);
                    if (mLocalMessage != null) {
                        if (mLocalMessage.sendStatus == ImConstant.MessageSendStatus.SENDING) {
                            // 发送中的消息发生异常，变更状态为发送失败
                            mLocalMessage.sendStatus = ImConstant.MessageSendStatus.FAIL;
                            ImManager.getInstance().postSendMessageAction(new SaveDatabaseTask(mLocalMessage));
                        }
                    }
                } finally {
                }
            }

            private boolean isTimeout() {
                return System.currentTimeMillis() - mWaitForTcpResponseStartTime > TIME_OUT_MS;
            }

            /**
             * 重置等待超时的开始时间
             */
            private void resetWaitForTcpResponseStartTime() {
                final long nowTime = System.currentTimeMillis();
                if (mWaitForTcpResponseStartTime != 0) {
                    Timber.v("found old mWaitForTcpResponseStartTime: %s, now:%s", mWaitForTcpResponseStartTime, nowTime);
                }
                mWaitForTcpResponseStartTime = nowTime;
            }

            /**
             * 等待服务器返回 或者 超时
             */
            private void waitForTcpResponseOrTimeout() {
                synchronized (OneMessageUploadTask.this) {
                    while (!isTimeout()
                            && mLocalMessage.sendStatus != ImConstant.MessageSendStatus.FAIL
                            && mLocalMessage.sendStatus != ImConstant.MessageSendStatus.SUCCESS) {
                        if (isAbort()) {
                            return;
                        }
                        try {
                            wait(2000);
                        } catch (Throwable e) {
                            // ignore
                        }
                    }
                }
            }

            private boolean notifyTcpResponse(ProtoMessages.RespondChat respondChat) {
                if (respondChat == null) {
                    Timber.v("respond chat is null");
                    return false;
                }

                if (mLocalMessage == null) {
                    Timber.v("ignore respond chat, mLocalMessage is null");
                    return false;
                }

                if (respondChat.getSign() != mLocalMessage.id) {
                    Timber.v("ignore respond chat, sign not match, sign:%s, local message id:%s", respondChat.getSign(), mLocalMessage.id);
                    return false;
                }

                if (isAbort()) {
                    Timber.v("ignore respond chat, already abort. local message id:%s", mLocalMessage.id);
                    return false;
                }

                if (mLocalMessage.sendStatus != ImConstant.MessageSendStatus.SENDING) {
                    Timber.e("ignore: respond chat got, but local message send status is not sending: %s", mLocalMessage.sendStatus);
                }

                final long code = respondChat.getCode();
                if (code != 0) {
                    // 消息发送失败
                    Timber.v("respond chat code error:%s, local message id:%s", code, mLocalMessage.id);
                    mLocalMessage.sendStatus = ImConstant.MessageSendStatus.FAIL;

                    if (code > 1000 && code < 2000) {
                        // code 在 (1000, 2000) 范围内, 给出提示
                        String messageText = respondChat.getMsg();
                        if (!TextUtils.isEmpty(messageText)) {
                            TipUtil.show(messageText);
                        }
                    } else if (code == 2012) {
                        // Ta在你的黑名单中
                        Message simpleMessage = new Message();
                        simpleMessage.msgType = ImConstant.MessageType.MESSAGE_TYPE_LOCAL_IN_MY_BLACK_LIST;
                        simpleMessage.conversationId = mLocalMessage.conversationId;
                        simpleMessage.fromUserId = mLocalMessage.fromUserId;
                        simpleMessage.toUserId = mLocalMessage.toUserId;
                        simpleMessage.sendStatus = ImConstant.MessageSendStatus.SUCCESS;
                        simpleMessage.readStatus = ImConstant.MessageReadStatus.READ;
                        simpleMessage.revertStatus = ImConstant.MessageRevertStatus.NORMAL;
                        simpleMessage.msgText = respondChat.getMsg();
                        simpleMessage.msgLocalTime = System.currentTimeMillis();
                        simpleMessage.msgServerTime = 0;
                        MessageManager.getInstance().insert(mSession.sessionUserId, simpleMessage);
                    } else if (code == 2013) {
                        // 对方拒绝接受你的消息
                        Message simpleMessage = new Message();
                        simpleMessage.msgType = ImConstant.MessageType.MESSAGE_TYPE_LOCAL_IN_OTHER_BLACK_LIST;
                        simpleMessage.conversationId = mLocalMessage.conversationId;
                        simpleMessage.fromUserId = mLocalMessage.fromUserId;
                        simpleMessage.toUserId = mLocalMessage.toUserId;
                        simpleMessage.sendStatus = ImConstant.MessageSendStatus.SUCCESS;
                        simpleMessage.readStatus = ImConstant.MessageReadStatus.READ;
                        simpleMessage.revertStatus = ImConstant.MessageRevertStatus.NORMAL;
                        simpleMessage.msgText = respondChat.getMsg();
                        simpleMessage.msgLocalTime = System.currentTimeMillis();
                        simpleMessage.msgServerTime = 0;
                        MessageManager.getInstance().insert(mSession.sessionUserId, simpleMessage);
                    }
                } else {
                    // 消息发送成功
                    Timber.v("respond chat code onSuccess:%s, local message id:%s, server msg id:%s", respondChat.getCode(), mLocalMessage.id, respondChat.getMsgId());
                    mLocalMessage.sendStatus = ImConstant.MessageSendStatus.SUCCESS;
                    mLocalMessage.msgId = respondChat.getMsgId();
                }
                synchronized (OneMessageUploadTask.this) {
                    notifyAll();
                }
                return true;
            }

            /**
             * 发送文本消息
             */
            private ProtoMessages.MsgData buildTextMessageOrThrow() {
                throwIfAbort();

                if (TextUtils.isEmpty(mLocalMessage.msgText)) {
                    throw new IllegalArgumentException("buildTextMessageOrThrow, msg text is empty " + mLocalMessage.toShortString());
                }

                ProtoMessages.MsgData msgData = ProtoMessages.MsgData.newBuilder()
                        .setMsg(mLocalMessage.msgText)
                        .build();

                return msgData;
            }

            /**
             * 发送图片消息
             */
            private ProtoMessages.MsgData buildImageMessageOrThrow() throws Throwable {
                throwIfAbort();

                if (TextUtils.isEmpty(mLocalMessage.msgImageLocalUrl)) {
                    throw new IllegalArgumentException("buildImageMessageOrThrow, msgImageLocalUrl is empty " + mLocalMessage.toShortString());
                }

                if (mLocalMessage.msgImageFileSize <= 0) {
                    throw new IllegalArgumentException("buildImageMessageOrThrow, invalid msgImageFileSize " + mLocalMessage.msgImageFileSize + mLocalMessage.toShortString());
                }

                if (mLocalMessage.msgImageWidth <= 0 || mLocalMessage.msgImageHeight <= 0) {
                    throw new IllegalArgumentException("buildImageMessageOrThrow, invalid msg image size, [" + mLocalMessage.msgImageWidth + ", " + mLocalMessage.msgImageHeight + "] " + mLocalMessage.toShortString());
                }

                final boolean needUploadImage = TextUtils.isEmpty(mLocalMessage.msgImageServerUrl);
                if (needUploadImage) {
                    // 上传图片
                    FileUploadHelper fileUploadHelper = new FileUploadHelper(input -> input.fileDirs.chat.images);
                    fileUploadHelper.setOnFileUploadProgressListener((id, localFilePath, progress)
                            -> mProgress.set(progress.getTotal(), progress.getCurrent()));
                    fileUploadHelper.setOnFileUploadListener(new FileUploadHelper.OnFileUploadListener() {
                        @Override
                        public void onUploadSuccess(String id, String localFilePath, String serverUrl) {
                            Timber.v("buildImageMessageOrThrow fileUploadHelper onUploadSuccess %s->%s", localFilePath, serverUrl);
                            mLocalMessage.msgImageServerUrl = serverUrl;
                        }

                        @Override
                        public void onUploadFail(String id, String localFilePath, Throwable e) {
                            Timber.v(e, "buildImageMessageOrThrow fileUploadHelper onUploadFail %s", localFilePath);
                            throw new IllegalStateException(e);
                        }
                    });
                    fileUploadHelper.blockingFileUpload(null, mLocalMessage.msgImageLocalUrl);
                }

                ProtoMessages.MsgData msgData = ProtoMessages.MsgData.newBuilder()
                        .setImage(ProtoMessages.Image.newBuilder()
                                .setFileSize(mLocalMessage.msgImageFileSize / 1000) // 将本地的 byte 转换为服务器的 kb
                                .setWidth(mLocalMessage.msgImageWidth)
                                .setHeight(mLocalMessage.msgImageHeight)
                                .setUrl(mLocalMessage.msgImageServerUrl))
                        .build();

                return msgData;
            }

            /**
             * 发送语音消息
             */
            private ProtoMessages.MsgData buildVoiceMessageOrThrow() throws Throwable {
                throwIfAbort();

                if (TextUtils.isEmpty(mLocalMessage.msgVoiceLocalUrl)) {
                    throw new IllegalArgumentException("buildVoiceMessageOrThrow, msgVoiceLocalUrl is empty " + mLocalMessage.toShortString());
                }

                if (mLocalMessage.msgVoiceFileSize <= 0) {
                    throw new IllegalArgumentException("buildVoiceMessageOrThrow, invalid msgVoiceFileSize " + mLocalMessage.msgVoiceFileSize + " " + mLocalMessage.toShortString());
                }

                if (mLocalMessage.msgVoiceDuration <= 0) {
                    throw new IllegalArgumentException("buildVoiceMessageOrThrow, invalid msgVoiceDuration " + mLocalMessage.msgVoiceDuration + " " + mLocalMessage.toShortString());
                }

                final boolean needUploadVoice = TextUtils.isEmpty(mLocalMessage.msgVoiceServerUrl);
                if (needUploadVoice) {
                    // 上传语音
                    FileUploadHelper fileUploadHelper = new FileUploadHelper(input -> input.fileDirs.chat.audio);
                    fileUploadHelper.setOnFileUploadProgressListener((id, localFilePath, progress)
                            -> mProgress.set(progress.getTotal(), progress.getCurrent()));
                    fileUploadHelper.setOnFileUploadListener(new FileUploadHelper.OnFileUploadListener() {
                        @Override
                        public void onUploadSuccess(String id, String localFilePath, String serverUrl) {
                            Timber.v("buildVoiceMessageOrThrow fileUploadHelper onUploadSuccess %s->%s", localFilePath, serverUrl);
                            mLocalMessage.msgVoiceServerUrl = serverUrl;
                        }

                        @Override
                        public void onUploadFail(String id, String localFilePath, Throwable e) {
                            Timber.v(e, "buildVoiceMessageOrThrow fileUploadHelper onUploadFail %s", localFilePath);
                            throw new IllegalStateException(e);
                        }
                    });
                    fileUploadHelper.blockingFileUpload(null, mLocalMessage.msgVoiceLocalUrl);
                }

                Preconditions.checkNotNull(mLocalMessage.msgVoiceServerUrl);

                ProtoMessages.MsgData msgData = ProtoMessages.MsgData.newBuilder()
                        .setVoice(ProtoMessages.Voice.newBuilder()
                                .setFileSize(mLocalMessage.msgVoiceFileSize / 1000) // 将本地的 byte 转换为服务器的 kb
                                .setUrl(mLocalMessage.msgVoiceServerUrl)
                                .setDuration(mLocalMessage.msgVoiceDuration / 1000)) // 将本地的 ms 转换为服务器的 s
                        .build();

                return msgData;
            }

            /**
             * 发送视频消息
             */
            private ProtoMessages.MsgData buildVideoMessageOrThrow() throws Throwable {
                throwIfAbort();

                if (TextUtils.isEmpty(mLocalMessage.msgVideoLocalUrl)) {
                    throw new IllegalArgumentException("buildVideoMessageOrThrow, msgVideoLocalUrl is empty " + mLocalMessage.toShortString());
                }

                if (mLocalMessage.msgVideoFileSize <= 0) {
                    throw new IllegalArgumentException("buildVideoMessageOrThrow, invalid msgVideoFileSize " + mLocalMessage.msgVideoFileSize + " " + mLocalMessage.toShortString());
                }

                if (mLocalMessage.msgVideoDuration <= 0) {
                    throw new IllegalArgumentException("buildVideoMessageOrThrow, invalid msgVideoDuration " + mLocalMessage.msgVideoDuration + " " + mLocalMessage.toShortString());
                }

                if (mLocalMessage.msgVideoWidth <= 0 || mLocalMessage.msgVideoHeight <= 0) {
                    throw new IllegalArgumentException("buildVideoMessageOrThrow, invalid msg video size, [" + mLocalMessage.msgVideoWidth + ", " + mLocalMessage.msgVideoHeight + "] " + mLocalMessage.toShortString());
                }

                final boolean needUploadVideo = TextUtils.isEmpty(mLocalMessage.msgVideoServerUrl);
                if (needUploadVideo) {
                    // 获取封面并上传
                    File thumbFile;
                    {
                        Bitmap bitmap = Glide.with(ContextUtil.getContext()).asBitmap().load(new File(mLocalMessage.msgVideoLocalUrl)).submit().get();
                        thumbFile = TmpFileManager.getInstance().createNewTmpFileQuietly("xmq_chat", ".jpg");
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(thumbFile);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                            fos.flush();
                        } finally {
                            IOUtil.closeQuietly(fos);
                        }
                        bitmap = null;
                    }
                    FileUploadHelper thumbFileUploadHelper = new FileUploadHelper(input -> input.fileDirs.chat.images);
                    thumbFileUploadHelper.setOnFileUploadListener(new FileUploadHelper.OnFileUploadListener() {
                        @Override
                        public void onUploadSuccess(String id, String localFilePath, String serverUrl) {
                            Timber.v("buildVideoMessageOrThrow thumbFileUploadHelper onUploadSuccess %s->%s", localFilePath, serverUrl);
                            mLocalMessage.msgVideoServerThumb = serverUrl;
                        }

                        @Override
                        public void onUploadFail(String id, String localFilePath, Throwable e) {
                            Timber.e(e, "buildVideoMessageOrThrow thumbFileUploadHelper onUploadFail %s", localFilePath);
                            throw new IllegalStateException(e);
                        }
                    });
                    thumbFileUploadHelper.blockingFileUpload(null, thumbFile.getAbsolutePath());

                    // 上传视频
                    FileUploadHelper videoFileUploadHelper = new FileUploadHelper(input -> input.fileDirs.chat.video);
                    videoFileUploadHelper.setOnFileUploadProgressListener((id, localFilePath, progress)
                            -> mProgress.set(progress.getTotal(), progress.getCurrent()));
                    videoFileUploadHelper.setOnFileUploadListener(new FileUploadHelper.OnFileUploadListener() {
                        @Override
                        public void onUploadSuccess(String id, String localFilePath, String serverUrl) {
                            Timber.v("buildVideoMessageOrThrow videoFileUploadHelper onUploadSuccess %s->%s", localFilePath, serverUrl);
                            mLocalMessage.msgVideoServerUrl = serverUrl;
                        }

                        @Override
                        public void onUploadFail(String id, String localFilePath, Throwable e) {
                            Timber.e(e, "buildVideoMessageOrThrow videoFileUploadHelper onUploadFail %s", localFilePath);
                            throw new IllegalStateException(e);
                        }
                    });
                    videoFileUploadHelper.blockingFileUpload(null, mLocalMessage.msgVideoLocalUrl);
                }

                Preconditions.checkNotNull(mLocalMessage.msgVideoServerThumb);
                Preconditions.checkNotNull(mLocalMessage.msgVideoServerUrl);

                ProtoMessages.MsgData msgData = ProtoMessages.MsgData.newBuilder()
                        .setVideo(ProtoMessages.Video.newBuilder()
                                .setFileSize(mLocalMessage.msgVideoFileSize / 1000) // 将本地的 byte 转换为服务器的 kb
                                .setThumb(mLocalMessage.msgVideoServerThumb)
                                .setUrl(mLocalMessage.msgVideoServerUrl)
                                .setDuration(mLocalMessage.msgVideoDuration / 1000) // 将本地的 ms 转换为服务器的 s
                                .setWidth(mLocalMessage.msgVideoWidth)
                                .setHeight(mLocalMessage.msgVideoHeight))
                        .build();

                return msgData;
            }

            /**
             * 发送定位消息
             */
            private ProtoMessages.MsgData buildLocationMessageOrThrow() {
                throwIfAbort();

                if (TextUtils.isEmpty(mLocalMessage.msgLocationTitle)) {
                    throw new IllegalArgumentException("buildLocationMessageOrThrow, msgLocationTitle is empty " + mLocalMessage.toShortString());
                }

                if (TextUtils.isEmpty(mLocalMessage.msgLocationAddress)) {
                    throw new IllegalArgumentException("buildLocationMessageOrThrow, msgLocationAddress is empty " + mLocalMessage.toShortString());
                }

                if (TextUtils.isEmpty(mLocalMessage.msgLocationLat)) {
                    throw new IllegalArgumentException("buildLocationMessageOrThrow, msgLocationLat is empty " + mLocalMessage.toShortString());
                }

                if (TextUtils.isEmpty(mLocalMessage.msgLocationLng)) {
                    throw new IllegalArgumentException("buildLocationMessageOrThrow, msgLocationLat is empty " + mLocalMessage.toShortString());
                }

                if (mLocalMessage.msgLocationZoom <= 0) {
                    throw new IllegalArgumentException("buildLocationMessageOrThrow, invalid msgLocationZoom " + mLocalMessage.msgLocationZoom + " " + mLocalMessage.toShortString());
                }

                ProtoMessages.MsgData msgData = ProtoMessages.MsgData.newBuilder()
                        .setLocation(ProtoMessages.Location.newBuilder()
                                .setTitle(mLocalMessage.msgLocationTitle)
                                .setAddress(mLocalMessage.msgLocationAddress)
                                .setLat(mLocalMessage.msgLocationLat)
                                .setLng(mLocalMessage.msgLocationLng)
                                .setZoom(mLocalMessage.msgLocationZoom))
                        .build();

                return msgData;
            }

            /**
             * 发送心跳消息
             */
            private ProtoMessages.MsgData buildHeartMessageOrThrow() {
                throwIfAbort();

                if (mLocalMessage.msgNumber > 1) {
                    // 长按的心跳消息，如果一直按着心跳按钮没有松手，则持续等待直到松手或者超时
                    final long waitTimeStart = System.currentTimeMillis();
                    final long maxWaitTimeDuration = TimeUnit.HOURS.toMillis(1);
                    synchronized (OneMessageUploadTask.this) {
                        long longPressedStartTime = SettingsManager.getInstance().getUserMemorySettings(mSession.sessionUserId).getDelaySendHeartIMMessageLongPressedStartTime();
                        while (longPressedStartTime > 0
                                && mLocalMessage.msgLocalTime > longPressedStartTime
                                && (System.currentTimeMillis() - waitTimeStart < maxWaitTimeDuration)) {
                            throwIfAbort();

                            try {
                                wait(200);
                            } catch (Throwable e) {
                                // ignore
                            }

                            longPressedStartTime = SettingsManager.getInstance().getUserMemorySettings(mSession.sessionUserId).getDelaySendHeartIMMessageLongPressedStartTime();
                        }
                    }
                }

                throwIfAbort();

                ProtoMessages.MsgData msgData = ProtoMessages.MsgData.newBuilder()
                        .setNumber(mLocalMessage.msgNumber)
                        .build();

                return msgData;
            }

            /**
             * 发送礼物消息
             */
            private ProtoMessages.MsgData buildGiftMessageOrThrow() {
                throwIfAbort();

                if (mLocalMessage.msgGiftId <= 0) {
                    throw new IllegalArgumentException("buildGiftMessageOrThrow, invalid msgGiftId " + mLocalMessage.msgGiftId + " " + mLocalMessage.toShortString());
                }

                if (TextUtils.isEmpty(mLocalMessage.msgGiftName)) {
                    throw new IllegalArgumentException("buildGiftMessageOrThrow, msgGiftName is empty " + mLocalMessage.toShortString());
                }

                if (TextUtils.isEmpty(mLocalMessage.msgGiftDesc)) {
                    throw new IllegalArgumentException("buildGiftMessageOrThrow, msgGiftDesc is empty " + mLocalMessage.toShortString());
                }

                if (mLocalMessage.msgGiftKPrice < 0) {
                    throw new IllegalArgumentException("buildGiftMessageOrThrow, invalid msgGiftKPrice " + mLocalMessage.msgGiftKPrice + " " + mLocalMessage.toShortString());
                }

                if (TextUtils.isEmpty(mLocalMessage.msgGiftCover)) {
                    throw new IllegalArgumentException("buildGiftMessageOrThrow, msgGiftCover is empty " + mLocalMessage.toShortString());
                }

                if (TextUtils.isEmpty(mLocalMessage.msgGiftAnim)) {
                    throw new IllegalArgumentException("buildGiftMessageOrThrow, msgGiftAnim is empty " + mLocalMessage.toShortString());
                }

                ProtoMessages.MsgData msgData = ProtoMessages.MsgData.newBuilder()
                        .setGiftInfo(ProtoMessages.Gift.newBuilder()
                                .setGiftId(mLocalMessage.msgGiftId)
                                .setGiftName(mLocalMessage.msgGiftName)
                                .setIntro(mLocalMessage.msgGiftDesc)
                                .setKprice(mLocalMessage.msgGiftKPrice)
                                .setAnimation(ProtoMessages.Image.newBuilder()
                                        .setThumb(mLocalMessage.msgGiftCover)
                                        .setUrl(mLocalMessage.msgGiftAnim)
                                        .build())
                        )
                        .build();

                return msgData;
            }
        }

    }

}
