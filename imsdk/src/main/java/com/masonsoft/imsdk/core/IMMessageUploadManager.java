package com.masonsoft.imsdk.core;

import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import com.idonans.core.Progress;
import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;
import com.idonans.core.thread.Threads;
import com.idonans.core.util.FileUtil;
import com.masonsoft.imsdk.core.block.MessageBlock;
import com.masonsoft.imsdk.core.db.LocalSendingMessage;
import com.masonsoft.imsdk.core.db.LocalSendingMessageProvider;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.NotNullTimeoutMessagePacket;
import com.masonsoft.imsdk.core.observable.MessagePacketStateObservable;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.lang.SafetyRunnable;
import com.masonsoft.imsdk.util.Objects;
import com.masonsoft.imsdk.util.Preconditions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public boolean dispatchTcpResponse(final long sessionUserId, final long sign, @NonNull final ProtoByteMessageWrapper wrapper) {
        return getSessionUploader(sessionUserId).dispatchTcpResponse(sign, wrapper);
    }

    public void touch(final long sessionUserId) {
        getSessionUploader(sessionUserId);
    }

    public float getUploadProgress(final long sessionUserId, final long localSendingMessageLocalId) {
        return getSessionUploader(sessionUserId).getUploadProgress(localSendingMessageLocalId);
    }

    private static class LocalErrorCodeException extends RuntimeException {
        private final int mErrorCode;

        private LocalErrorCodeException(int errorCode) {
            mErrorCode = errorCode;
        }
    }

    private static class LocalErrorCode {
        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
        private static final int FIRST_LOCAL_ERROR_CODE = Integer.MIN_VALUE / 2;
        private static int sNextErrorCode = FIRST_LOCAL_ERROR_CODE;
        /**
         * 未知错误
         */
        private static final int ERROR_CODE_UNKNOWN = sNextErrorCode++;
        /**
         * 期望的目标对象没有找到
         */
        private static final int ERROR_CODE_TARGET_NOT_FOUND = sNextErrorCode++;
        /**
         * 绑定 abort id 失败
         */
        private static final int ERROR_CODE_BIND_ABORT_ID_FAIL = sNextErrorCode++;
        /**
         * 更新 sendStatus 失败
         */
        private static final int ERROR_CODE_UPDATE_SEND_STATUS_FAIL = sNextErrorCode++;
        /**
         * 构建 protoByteMessage 失败
         */
        private static final int ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL = sNextErrorCode++;
        /**
         * sessionTcpClientProxy 为 null
         */
        private static final int ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL = sNextErrorCode++;
        /**
         * sessionTcpClientProxy session 无效
         */
        private static final int ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID = sNextErrorCode++;
        /**
         * sessionTcpClientProxy 链接错误
         */
        private static final int ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR = sNextErrorCode++;
        /**
         * sessionTcpClientProxy 未知错误
         */
        private static final int ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN = sNextErrorCode++;
        /**
         * 文件不存在
         */
        private static final int ERROR_CODE_FILE_NOT_EXISTS = sNextErrorCode++;
        /**
         * 文件为空
         */
        private static final int ERROR_CODE_FILE_EMPTY = sNextErrorCode++;
        /**
         * 文件上传失败
         */
        private static final int ERROR_CODE_FILE_UPLOAD_FAIL = sNextErrorCode++;
        /**
         * messagePacket 发送失败
         */
        private static final int ERROR_CODE_MESSAGE_PACKET_SEND_FAIL = sNextErrorCode++;
        /**
         * messagePacket 发送超时
         */
        private static final int ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT = sNextErrorCode++;

        private static final Map<Integer, String> DEFAULT_ERROR_MESSAGE_MAP = new HashMap<>();

        static {
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_UNKNOWN, "ERROR_CODE_UNKNOWN");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_TARGET_NOT_FOUND, "ERROR_CODE_TARGET_NOT_FOUND");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_BIND_ABORT_ID_FAIL, "ERROR_CODE_BIND_ABORT_ID_FAIL");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_UPDATE_SEND_STATUS_FAIL, "ERROR_CODE_UPDATE_SEND_STATUS_FAIL");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL, "ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL, "ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID, "ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR, "ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN, "ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_FILE_NOT_EXISTS, "ERROR_CODE_FILE_NOT_EXISTS");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_FILE_EMPTY, "ERROR_CODE_FILE_EMPTY");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_FILE_UPLOAD_FAIL, "ERROR_CODE_FILE_UPLOAD_FAIL");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_MESSAGE_PACKET_SEND_FAIL, "ERROR_CODE_MESSAGE_PACKET_SEND_FAIL");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT, "ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT");

            Preconditions.checkArgument(DEFAULT_ERROR_MESSAGE_MAP.size() == ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT - FIRST_LOCAL_ERROR_CODE + 1);
        }
        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
    }

    private class SessionUploader {

        private class MessageUploadObjectWrapper {
            private final long mSessionUserId;
            private final long mSign;
            private final long mAbortId;
            @NonNull
            private LocalSendingMessage mLocalSendingMessage;
            @Nullable
            private Message mMessage;

            private final AtomicBoolean mBuildChatSMessagePacket = new AtomicBoolean(false);
            @Nullable
            private ChatSMessagePacket mChatSMessagePacket;
            @NonNull
            private final MessagePacketStateObservable.MessagePacketStateObserver mChatSMessagePacketStateObserver = new MessagePacketStateObservable.MessagePacketStateObserver() {
                @Override
                public void onStateChanged(MessagePacket packet, int oldState, int newState) {
                    if (packet != mChatSMessagePacket) {
                        final Throwable e = new IllegalAccessError("invalid packet:" + Objects.defaultObjectTag(packet)
                                + ", mChatSMessagePacket:" + Objects.defaultObjectTag(mChatSMessagePacket));
                        IMLog.e(e);
                        return;
                    }

                    boolean notify = false;
                    final ChatSMessagePacket chatSMessagePacket = (ChatSMessagePacket) packet;
                    if (newState == MessagePacket.STATE_FAIL) {
                        // 消息发送失败
                        notify = true;
                        IMLog.v("onStateChanged STATE_FAIL chatSMessagePacket errorCode:%s, errorMessage:%s, timeout:%s",
                                chatSMessagePacket.getErrorCode(), chatSMessagePacket.getErrorMessage(), chatSMessagePacket.isTimeoutTriggered());
                        if (chatSMessagePacket.getErrorCode() != 0) {
                            setError(chatSMessagePacket.getErrorCode(), chatSMessagePacket.getErrorMessage());
                        } else if (chatSMessagePacket.isTimeoutTriggered()) {
                            setError(LocalErrorCode.ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT);
                        }
                        moveSendStatus(IMConstants.SendStatus.FAIL);
                    } else if (newState == MessagePacket.STATE_SUCCESS) {
                        // 消息发送成功
                        notify = true;
                        // 设置发送进度为 100%
                        setSendProgress(1f);
                        moveSendStatus(IMConstants.SendStatus.SUCCESS);
                    }

                    if (notify) {
                        // @see MessageUploadObjectWrapperTask#run -> "// wait message packet result"
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (chatSMessagePacket) {
                            chatSMessagePacket.notify();
                        }
                    }
                }
            };

            /**
             * abort id 与数据库中对应记录不相等.
             */
            private boolean mAbortIdNotMatch;

            /**
             * 消息的发送进度
             */
            public float mSendProgress;

            public long mErrorCode;
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

            private void setSendProgress(float sendProgress) {
                mSendProgress = sendProgress;
                mUnsafeProgress.put(mLocalSendingMessage.localId.get(), sendProgress);
            }

            private boolean hasErrorOrAbort() {
                return this.mErrorCode != 0 || this.mAbortIdNotMatch;
            }

            private void setError(long errorCode) {
                this.setError(errorCode, null);
            }

            private void setError(long errorCode, String errorMessage) {
                if (errorMessage == null) {
                    errorMessage = LocalErrorCode.DEFAULT_ERROR_MESSAGE_MAP.get((int) errorCode);
                }
                this.mErrorCode = errorCode;
                this.mErrorMessage = errorMessage;
            }

            @Nullable
            private MessagePacket buildMessagePacket() {
                if (!mBuildChatSMessagePacket.weakCompareAndSet(false, true)) {
                    throw new IllegalAccessError("buildMessagePacket only support called once");
                }

                final Message message = this.mMessage;
                if (message == null) {
                    return null;
                }

                // 将消息预处理之后构建为 proto buf
                final int messageType = message.messageType.get();
                if (messageType == IMConstants.MessageType.TEXT) {
                    final ProtoMessage.ChatS chatS = ProtoMessage.ChatS.newBuilder()
                            .setSign(mSign)
                            .setType(messageType)
                            .setToUid(message.toUserId.get())
                            .setBody(message.body.get())
                            .build();
                    final ProtoByteMessage protoByteMessage = ProtoByteMessage.Type.encode(chatS);
                    final ChatSMessagePacket chatSMessagePacket = new ChatSMessagePacket(protoByteMessage, mSign);
                    mChatSMessagePacket = chatSMessagePacket;
                    mChatSMessagePacket.getMessagePacketStateObservable().registerObserver(mChatSMessagePacketStateObserver);
                    return chatSMessagePacket;
                }

                if (messageType == IMConstants.MessageType.IMAGE) {
                    final String imageUrl = message.body.get();
                    if (!URLUtil.isNetworkUrl(imageUrl)) {
                        final String accessUrl = uploadFile(imageUrl, new Progress() {
                            @Override
                            protected void onUpdate() {
                                super.onUpdate();
                                setSendProgress(getPercent() / 100f);
                            }
                        });
                        // 备份原始地址
                        message.localBodyOrigin.set(imageUrl);
                        // 设置上传成功后的网络地址
                        message.body.set(accessUrl);
                    }

                    final ProtoMessage.ChatS chatS = ProtoMessage.ChatS.newBuilder()
                            .setSign(mSign)
                            .setType(messageType)
                            .setToUid(message.toUserId.get())
                            .setBody(message.body.get())
                            .setWidth(message.width.get())
                            .setHeight(message.height.get())
                            .build();
                    final ProtoByteMessage protoByteMessage = ProtoByteMessage.Type.encode(chatS);
                    final ChatSMessagePacket chatSMessagePacket = new ChatSMessagePacket(protoByteMessage, mSign);
                    mChatSMessagePacket = chatSMessagePacket;
                    mChatSMessagePacket.getMessagePacketStateObservable().registerObserver(mChatSMessagePacketStateObserver);
                    return chatSMessagePacket;
                }

                final Throwable e = new IllegalAccessError("unknown message type:" + messageType + " " + message);
                IMLog.e(e);
                return null;
            }

            private String uploadFile(final String filePath, @NonNull final Progress progress) {
                final File file = new File(filePath);
                if (!FileUtil.isFile(file)) {
                    throw new LocalErrorCodeException(LocalErrorCode.ERROR_CODE_FILE_NOT_EXISTS);
                }
                final long fileLength = file.length();
                if (fileLength <= 0) {
                    throw new LocalErrorCodeException(LocalErrorCode.ERROR_CODE_FILE_EMPTY);
                }
                progress.set(fileLength, 0);

                try {
                    final String accessUrl = FileUploadManager.getInstance().getFileUploadProvider()
                            .uploadFile(filePath, progress);
                    IMLog.v("uploadFile success %s -> %s", filePath, accessUrl);
                    return accessUrl;
                } catch (Throwable e) {
                    IMLog.e(e);
                    throw new LocalErrorCodeException(LocalErrorCode.ERROR_CODE_FILE_UPLOAD_FAIL);
                }
            }

            private boolean dispatchTcpResponse(final long sign, @NonNull final ProtoByteMessageWrapper wrapper) {
                final ChatSMessagePacket chatSMessagePacket = mChatSMessagePacket;
                if (chatSMessagePacket == null) {
                    final Throwable e = new IllegalAccessError(Objects.defaultObjectTag(this) + " unexpected mChatSMessagePacket is null");
                    IMLog.e(e);
                    return false;
                }
                if (mSign != sign) {
                    final Throwable e = new IllegalAccessError(Objects.defaultObjectTag(this) + " unexpected sign not match mSign:" + mSign + ", sign:" + sign);
                    IMLog.e(e);
                    return false;
                }
                final boolean result = chatSMessagePacket.doProcess(wrapper);
                IMLog.v(Objects.defaultObjectTag(this) + " dispatchTcpResponse chatSMessagePacket.doProcess result:%s", result);
                return result;
            }

            private class ChatSMessagePacket extends NotNullTimeoutMessagePacket {

                public ChatSMessagePacket(ProtoByteMessage protoByteMessage, long sign) {
                    super(protoByteMessage, sign);
                }

                @Override
                protected boolean doNotNullProcess(@NonNull ProtoByteMessageWrapper target) {
                    // check thread state
                    Threads.mustNotUi();

                    final Object protoMessageObject = target.getProtoMessageObject();
                    if (protoMessageObject == null) {
                        return false;
                    }

                    if (protoMessageObject instanceof ProtoMessage.Result) {
                        // 接收 Result 消息
                        final ProtoMessage.Result result = (ProtoMessage.Result) protoMessageObject;
                        if (result.getSign() == getSign()) {
                            // 校验 sign 是否相等

                            synchronized (getStateLock()) {
                                final int state = getState();
                                if (state != STATE_WAIT_RESULT) {
                                    IMLog.e(Objects.defaultObjectTag(ChatSMessagePacket.this)
                                            + " unexpected. accept with same sign:%s and invalid state:%s", getSign(), stateToString(state));
                                    return false;
                                }

                                if (result.getCode() != 0) {
                                    setErrorCode(result.getCode());
                                    setErrorMessage(result.getMsg());
                                    IMLog.e(Objects.defaultObjectTag(ChatSMessagePacket.this) +
                                            " unexpected. errorCode:%s, errorMessage:%s", result.getCode(), result.getMsg());
                                    moveToState(STATE_FAIL);
                                } else {
                                    IMLog.e(Objects.defaultObjectTag(ChatSMessagePacket.this) +
                                            " unexpected. accept with same sign:%s and invalid Result code:%s", getSign(), result.getCode());
                                    return false;
                                }
                            }

                            return true;
                        }
                    } else if (protoMessageObject instanceof ProtoMessage.ChatSR) {
                        // 接收 ChatSR 消息
                        final ProtoMessage.ChatSR chatSR = (ProtoMessage.ChatSR) protoMessageObject;
                        if (chatSR.getSign() == getSign()) {
                            // 校验 sign 是否相等

                            synchronized (getStateLock()) {
                                final int state = getState();
                                if (state != STATE_WAIT_RESULT) {
                                    IMLog.e(Objects.defaultObjectTag(ChatSMessagePacket.this)
                                            + " unexpected. accept with same sign:%s and invalid state:%s", getSign(), stateToString(state));
                                    return false;
                                }

                                final long msgId = chatSR.getMsgId();
                                if (msgId <= 0) {
                                    IMLog.e(Objects.defaultObjectTag(ChatSMessagePacket.this)
                                            + " unexpected. invalid msgId:%s, sign:%s", msgId, getSign());
                                    moveToState(STATE_FAIL);
                                    return true;
                                }
                                // 微秒
                                final long msgTime = chatSR.getMsgTime();
                                if (msgTime <= 0) {
                                    IMLog.e(Objects.defaultObjectTag(ChatSMessagePacket.this)
                                            + " unexpected. invalid msgTime:%s, sign:%s", msgTime, getSign());
                                    moveToState(STATE_FAIL);
                                    return true;
                                }

                                final Message message = mMessage;
                                if (message == null) {
                                    IMLog.e(Objects.defaultObjectTag(ChatSMessagePacket.this)
                                            + " unexpected. message is null, sign:%s", msgTime, getSign());
                                    moveToState(STATE_FAIL);
                                    return true;
                                }

                                final Message messageUpdate = new Message();
                                messageUpdate.localId.set(message.localId.get());
                                messageUpdate.remoteMessageId.set(msgId);
                                messageUpdate.remoteMessageTime.set(msgTime);
                                // 设置 block id
                                final long blockId = MessageBlock.generateBlockId(
                                        mSessionUserId,
                                        mLocalSendingMessage.conversationType.get(),
                                        mLocalSendingMessage.targetUserId.get(),
                                        msgId
                                );
                                Preconditions.checkArgument(blockId > 0);
                                messageUpdate.localBlockId.set(blockId);

                                if (!MessageDatabaseProvider.getInstance().updateMessage(
                                        mSessionUserId,
                                        mLocalSendingMessage.conversationType.get(),
                                        mLocalSendingMessage.targetUserId.get(),
                                        messageUpdate)) {
                                    IMLog.e(Objects.defaultObjectTag(ChatSMessagePacket.this)
                                            + " unexpected. updateMessage return false, sign:%s, messageUpdate:%s", getSign(), messageUpdate);
                                    moveToState(STATE_FAIL);
                                    return true;
                                }

                                final Message readMessage = MessageDatabaseProvider.getInstance().getMessage(
                                        mSessionUserId,
                                        mLocalSendingMessage.conversationType.get(),
                                        mLocalSendingMessage.targetUserId.get(),
                                        message.localId.get()
                                );
                                if (readMessage == null) {
                                    IMLog.e(Objects.defaultObjectTag(ChatSMessagePacket.this)
                                                    + " unexpected. getMessage return null, sign:%s, sessionUserId:%s, conversationType:%s, targetUserId:%s, localId:%s",
                                            getSign(),
                                            mSessionUserId,
                                            mLocalSendingMessage.conversationType.get(),
                                            mLocalSendingMessage.targetUserId.get(),
                                            message.localId.get());
                                    moveToState(STATE_FAIL);
                                    return true;
                                }

                                mMessage = readMessage;
                                moveToState(STATE_SUCCESS);
                                return true;
                            }
                        }
                    }

                    return false;
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
                            setError(LocalErrorCode.ERROR_CODE_TARGET_NOT_FOUND);
                        }
                    } else {
                        setError(LocalErrorCode.ERROR_CODE_BIND_ABORT_ID_FAIL);
                    }
                } catch (Throwable e) {
                    IMLog.e(e);
                    setError(LocalErrorCode.ERROR_CODE_UNKNOWN);
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
                    setError(LocalErrorCode.ERROR_CODE_TARGET_NOT_FOUND);
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
                    // 保存错误信息
                    localSendingMessageUpdate.errorCode.set(mErrorCode);
                    localSendingMessageUpdate.errorMessage.set(mErrorMessage);
                    if (LocalSendingMessageProvider.getInstance().updateLocalSendingMessage(this.mSessionUserId, localSendingMessageUpdate)) {
                        localSendingMessage = LocalSendingMessageProvider.getInstance().getLocalSendingMessage(
                                this.mSessionUserId,
                                this.mLocalSendingMessage.localId.get()
                        );
                        if (localSendingMessage != null) {
                            this.mLocalSendingMessage = localSendingMessage;
                        } else {
                            setError(LocalErrorCode.ERROR_CODE_TARGET_NOT_FOUND);
                        }
                    } else {
                        setError(LocalErrorCode.ERROR_CODE_UPDATE_SEND_STATUS_FAIL);
                    }
                } catch (Throwable e) {
                    IMLog.e(e);
                    setError(LocalErrorCode.ERROR_CODE_UNKNOWN);
                }
            }
        }

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

        // 记录消息的发送进度
        private final LruCache<Long, Float> mUnsafeProgress = new LruCache<>(MAX_RUNNING_SIZE * 10);

        private SessionUploader(long sessionUserId) {
            mSessionUserId = sessionUserId;
            LocalSendingMessageProvider.getInstance().updateMessageToFailIfNotSuccess(mSessionUserId);
            LocalSendingMessageProvider.getInstance().removeAllSuccessMessage(mSessionUserId);
        }

        private float getUploadProgress(final long localSendingMessageLocalId) {
            // 注意查询性能
            final Float progress = mUnsafeProgress.get(localSendingMessageLocalId);
            if (progress != null) {
                return progress;
            }
            // 默认 0%
            return 0f;
        }

        @Nullable
        private MessageUploadObjectWrapperTask getTask(final long sign) {
            for (MessageUploadObjectWrapperTask task : mAllRunningTasks) {
                if (task.mMessageUploadObjectWrapper.mSign == sign) {
                    return task;
                }
            }
            return null;
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

        private boolean dispatchTcpResponse(final long sign, @NonNull final ProtoByteMessageWrapper wrapper) {
            synchronized (mAllRunningTasks) {
                final MessageUploadObjectWrapperTask task = getTask(sign);
                if (task == null) {
                    return false;
                }
                if (task.mMessageUploadObjectWrapper.dispatchTcpResponse(sign, wrapper)) {
                    return true;
                }
            }
            return false;
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

            private MessageUploadObjectWrapperTask(@NonNull MessageUploadObjectWrapper messageUploadObjectWrapper) {
                mMessageUploadObjectWrapper = messageUploadObjectWrapper;
            }

            @Override
            public void run() {
                try {
                    if (mMessageUploadObjectWrapper.hasErrorOrAbort()) {
                        return;
                    }

                    mMessageUploadObjectWrapper.moveSendStatus(IMConstants.SendStatus.SENDING);
                    if (mMessageUploadObjectWrapper.hasErrorOrAbort()) {
                        return;
                    }

                    final Message message = mMessageUploadObjectWrapper.mMessage;
                    if (message == null) {
                        mMessageUploadObjectWrapper.setError(LocalErrorCode.ERROR_CODE_TARGET_NOT_FOUND);
                        return;
                    }

                    final MessagePacket messagePacket = mMessageUploadObjectWrapper.buildMessagePacket();
                    if (messagePacket == null) {
                        mMessageUploadObjectWrapper.setError(LocalErrorCode.ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL);
                        return;
                    }

                    // 通过长连接发送 proto buf
                    final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxy();
                    if (proxy == null) {
                        mMessageUploadObjectWrapper.setError(LocalErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL);
                        return;
                    }

                    if (IMSessionManager.getInstance().getSessionUserId() != mSessionUserId) {
                        mMessageUploadObjectWrapper.setError(LocalErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID);
                        return;
                    }

                    if (!proxy.isOnline()) {
                        mMessageUploadObjectWrapper.setError(LocalErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR);
                        return;
                    }

                    final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
                    if (sessionTcpClient == null) {
                        mMessageUploadObjectWrapper.setError(LocalErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN);
                        return;
                    }

                    if (mMessageUploadObjectWrapper.hasErrorOrAbort()) {
                        return;
                    }

                    sessionTcpClient.sendMessagePacketQuietly(messagePacket);
                    if (messagePacket.getState() != MessagePacket.STATE_WAIT_RESULT) {
                        mMessageUploadObjectWrapper.setError(LocalErrorCode.ERROR_CODE_MESSAGE_PACKET_SEND_FAIL);
                        return;
                    }

                    // wait message packet result
                    while (messagePacket.getState() == MessagePacket.STATE_WAIT_RESULT) {
                        IMLog.v(Objects.defaultObjectTag(this) + " wait message packet result %s", messagePacket);
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (messagePacket) {
                            try {
                                messagePacket.wait(2000L);
                            } catch (InterruptedException e) {
                                IMLog.v("messagePacket wait interrupted %s", messagePacket);
                            }
                        }
                    }
                    IMLog.v(Objects.defaultObjectTag(this) + " body run end. %s", messagePacket);
                } catch (Throwable e) {
                    IMLog.e(e);
                    if (e instanceof LocalErrorCodeException) {
                        mMessageUploadObjectWrapper.setError(((LocalErrorCodeException) e).mErrorCode);
                    } else if (mMessageUploadObjectWrapper.mErrorCode == 0) {
                        mMessageUploadObjectWrapper.setError(LocalErrorCode.ERROR_CODE_UNKNOWN);
                    }
                }
            }
        }
    }
}
