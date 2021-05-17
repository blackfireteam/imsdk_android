package com.masonsoft.imsdk.core;

import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import com.masonsoft.imsdk.core.block.MessageBlock;
import com.masonsoft.imsdk.core.db.LocalSendingMessage;
import com.masonsoft.imsdk.core.db.LocalSendingMessageProvider;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.NotNullTimeoutMessagePacket;
import com.masonsoft.imsdk.core.observable.MessageObservable;
import com.masonsoft.imsdk.core.observable.MessagePacketStateObservable;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.lang.GeneralErrorCode;
import com.masonsoft.imsdk.lang.GeneralErrorCodeException;
import com.masonsoft.imsdk.lang.SafetyRunnable;
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.idonans.core.Progress;
import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.TaskQueue;
import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.Preconditions;

/**
 * 会话消息上传队列. 从 LocalSendingMessage 表中读取需要发送的内容依次处理, 并处理对应的消息响应。
 *
 * @see LocalSendingMessage
 * @see LocalSendingMessageProvider
 * @since 1.0
 */
public class IMSessionMessageUploadManager {

    private static final Singleton<IMSessionMessageUploadManager> INSTANCE = new Singleton<IMSessionMessageUploadManager>() {
        @Override
        protected IMSessionMessageUploadManager create() {
            return new IMSessionMessageUploadManager();
        }
    };

    public static IMSessionMessageUploadManager getInstance() {
        return INSTANCE.get();
    }

    private final Map<Long, SessionWorker> mSessionUploaderMap = new HashMap<>();

    private IMSessionMessageUploadManager() {
    }

    @NonNull
    private SessionWorker getSessionUploader(final long sessionUserId) {
        SessionWorker sessionWorker = mSessionUploaderMap.get(sessionUserId);
        if (sessionWorker != null) {
            return sessionWorker;
        }
        synchronized (mSessionUploaderMap) {
            sessionWorker = mSessionUploaderMap.get(sessionUserId);
            if (sessionWorker == null) {
                sessionWorker = new SessionWorker(sessionUserId);
                mSessionUploaderMap.put(sessionUserId, sessionWorker);
            }
            return sessionWorker;
        }
    }

    /**
     * 通知同步 LocalSendingMessage 表内容，可能添加了新的消息上传发送任务。
     */
    public void notifySyncLocalSendingMessage(final long sessionUserId) {
        getSessionUploader(sessionUserId).dispatchCheckIdleMessage();
    }

    public boolean dispatchTcpResponse(final long sign, @NonNull final SessionProtoByteMessageWrapper wrapper) {
        return getSessionUploader(wrapper.getSessionUserId()).dispatchTcpResponse(sign, wrapper);
    }

    public void touch(final long sessionUserId) {
        getSessionUploader(sessionUserId);
    }

    public float getUploadProgress(final long sessionUserId, final long localSendingMessageLocalId) {
        return getSessionUploader(sessionUserId).getUploadProgress(localSendingMessageLocalId);
    }

    private class SessionWorker implements DebugManager.DebugInfoProvider {

        private class SessionMessageObjectWrapper {
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
                            setError(GeneralErrorCode.ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT);
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

            public int mErrorCode;
            public String mErrorMessage;

            //////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////
            //////////////////////////////////////////////////////////////////////

            public SessionMessageObjectWrapper(long sessionUserId, long sign, long abortId, @NonNull LocalSendingMessage localSendingMessage) {
                this.mSessionUserId = sessionUserId;
                this.mSign = sign;
                this.mAbortId = abortId;
                this.mLocalSendingMessage = localSendingMessage;
            }

            private void setSendProgress(float sendProgress) {
                mSendProgress = sendProgress;
                mUnsafeProgress.put(mLocalSendingMessage.localId.get(), sendProgress);

                notifyMessageChanged();
            }

            private void notifyMessageChanged() {
                final Message message = mMessage;
                if (message == null) {
                    return;
                }

                MessageObservable.DEFAULT.notifyMessageChanged(
                        message._sessionUserId.get(),
                        message._conversationType.get(),
                        message._targetUserId.get(),
                        message.localId.get()
                );
            }

            private boolean hasErrorOrAbort() {
                return this.mErrorCode != 0 || this.mAbortIdNotMatch;
            }

            private void setError(int errorCode) {
                this.setError(errorCode, null);
            }

            private void setError(int errorCode, String errorMessage) {
                if (errorMessage == null) {
                    errorMessage = GeneralErrorCode.findDefaultErrorMessage(errorCode);
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
                    IMLog.e(Objects.defaultObjectTag(this) + " unexpected. mMessage is null");
                    return null;
                }

                // 将消息预处理之后构建为 proto buf
                final int messageType = message.messageType.get();
                if (messageType == IMConstants.MessageType.TEXT) {
                    // 文本消息
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
                    // 图片消息
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

                        // 更新至数据库
                        final Message messageUpdate = new Message();
                        messageUpdate.localId.apply(message.localId);
                        messageUpdate.body.apply(message.body);
                        messageUpdate.localBodyOrigin.apply(message.localBodyOrigin);
                        if (!MessageDatabaseProvider.getInstance().updateMessage(
                                mSessionUserId,
                                mLocalSendingMessage.conversationType.get(),
                                mLocalSendingMessage.targetUserId.get(),
                                messageUpdate)) {
                            IMLog.e(Objects.defaultObjectTag(this)
                                    + " unexpected. updateMessage return false, sign:%s, messageUpdate:%s", mSign, messageUpdate);
                            return null;
                        }
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

                if (messageType == IMConstants.MessageType.AUDIO) {
                    // 语音消息
                    final String audioUrl = message.body.get();
                    if (!URLUtil.isNetworkUrl(audioUrl)) {
                        final String accessUrl = uploadFile(audioUrl, new Progress() {
                            @Override
                            protected void onUpdate() {
                                super.onUpdate();
                                setSendProgress(getPercent() / 100f);
                            }
                        });
                        // 备份原始地址
                        message.localBodyOrigin.set(audioUrl);
                        // 设置上传成功后的网络地址
                        message.body.set(accessUrl);

                        // 更新至数据库
                        final Message messageUpdate = new Message();
                        messageUpdate.localId.apply(message.localId);
                        messageUpdate.body.apply(message.body);
                        messageUpdate.localBodyOrigin.apply(message.localBodyOrigin);
                        if (!MessageDatabaseProvider.getInstance().updateMessage(
                                mSessionUserId,
                                mLocalSendingMessage.conversationType.get(),
                                mLocalSendingMessage.targetUserId.get(),
                                messageUpdate)) {
                            IMLog.e(Objects.defaultObjectTag(this)
                                    + " unexpected. updateMessage return false, sign:%s, messageUpdate:%s", mSign, messageUpdate);
                            return null;
                        }
                    }

                    final ProtoMessage.ChatS chatS = ProtoMessage.ChatS.newBuilder()
                            .setSign(mSign)
                            .setType(messageType)
                            .setToUid(message.toUserId.get())
                            .setBody(message.body.get())
                            // 将时长的毫秒转换为秒(四舍五入)
                            .setDuration((message.durationMs.get() + 500) / 1000)
                            .build();
                    final ProtoByteMessage protoByteMessage = ProtoByteMessage.Type.encode(chatS);
                    final ChatSMessagePacket chatSMessagePacket = new ChatSMessagePacket(protoByteMessage, mSign);
                    mChatSMessagePacket = chatSMessagePacket;
                    mChatSMessagePacket.getMessagePacketStateObservable().registerObserver(mChatSMessagePacketStateObserver);
                    return chatSMessagePacket;
                }

                if (messageType == IMConstants.MessageType.VIDEO) {
                    // 视频消息
                    {
                        // 上传视频内容
                        final String videoUrl = message.body.get();
                        if (!URLUtil.isNetworkUrl(videoUrl)) {
                            final String accessUrl = uploadFile(videoUrl, new Progress() {
                                @Override
                                protected void onUpdate() {
                                    super.onUpdate();
                                    setSendProgress((getPercent() / 100f) * 0.8f/*上传视频内容占整体上传进度的 80% */);
                                }
                            });
                            // 备份原始地址
                            message.localBodyOrigin.set(videoUrl);
                            // 设置上传成功后的网络地址
                            message.body.set(accessUrl);

                            // 更新至数据库
                            final Message messageUpdate = new Message();
                            messageUpdate.localId.apply(message.localId);
                            messageUpdate.body.apply(message.body);
                            messageUpdate.localBodyOrigin.apply(message.localBodyOrigin);
                            if (!MessageDatabaseProvider.getInstance().updateMessage(
                                    mSessionUserId,
                                    mLocalSendingMessage.conversationType.get(),
                                    mLocalSendingMessage.targetUserId.get(),
                                    messageUpdate)) {
                                IMLog.e(Objects.defaultObjectTag(this)
                                        + " unexpected. updateMessage return false, sign:%s, messageUpdate:%s", mSign, messageUpdate);
                                return null;
                            }
                        }
                    }
                    {
                        // 上传封面内容
                        final String thumbUrl = message.thumb.get();
                        if (!URLUtil.isNetworkUrl(thumbUrl)) {
                            final String accessUrl = uploadFile(thumbUrl, new Progress() {
                                @Override
                                protected void onUpdate() {
                                    super.onUpdate();
                                    setSendProgress(0.8f + (getPercent() / 100f) * 0.2f/*上传封面内容占整体上传进度的 20% */);
                                }
                            });
                            // 备份原始地址
                            message.localThumbOrigin.set(thumbUrl);
                            // 设置上传成功后的网络地址
                            message.thumb.set(accessUrl);

                            // 更新至数据库
                            final Message messageUpdate = new Message();
                            messageUpdate.localId.apply(message.localId);
                            messageUpdate.thumb.apply(message.thumb);
                            messageUpdate.localThumbOrigin.apply(message.localThumbOrigin);
                            if (!MessageDatabaseProvider.getInstance().updateMessage(
                                    mSessionUserId,
                                    mLocalSendingMessage.conversationType.get(),
                                    mLocalSendingMessage.targetUserId.get(),
                                    messageUpdate)) {
                                IMLog.e(Objects.defaultObjectTag(this)
                                        + " unexpected. updateMessage return false, sign:%s, messageUpdate:%s", mSign, messageUpdate);
                                return null;
                            }
                        }
                    }

                    final ProtoMessage.ChatS chatS = ProtoMessage.ChatS.newBuilder()
                            .setSign(mSign)
                            .setType(messageType)
                            .setToUid(message.toUserId.get())
                            .setBody(message.body.get())
                            // 将时长的毫秒转换为秒(四舍五入)
                            .setDuration((message.durationMs.get() + 500) / 1000)
                            .setWidth(message.width.get())
                            .setHeight(message.height.get())
                            .setThumb(message.thumb.get())
                            .build();
                    final ProtoByteMessage protoByteMessage = ProtoByteMessage.Type.encode(chatS);
                    final ChatSMessagePacket chatSMessagePacket = new ChatSMessagePacket(protoByteMessage, mSign);
                    mChatSMessagePacket = chatSMessagePacket;
                    mChatSMessagePacket.getMessagePacketStateObservable().registerObserver(mChatSMessagePacketStateObserver);
                    return chatSMessagePacket;
                }

                if (messageType == IMConstants.MessageType.WINK) {
                    // wink 消息
                    final ProtoMessage.ChatS chatS = ProtoMessage.ChatS.newBuilder()
                            .setSign(mSign)
                            .setType(messageType)
                            .setToUid(message.toUserId.get())
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

            private String uploadFile(final String fileUri, @NonNull final Progress progress) {
                try {
                    final String accessUrl = FileUploadManager.getInstance().getFileUploadProvider()
                            .uploadFile(fileUri, progress);
                    IMLog.v("uploadFile success %s -> %s", fileUri, accessUrl);
                    return accessUrl;
                } catch (Throwable e) {
                    IMLog.e(e);
                    throw new GeneralErrorCodeException(GeneralErrorCode.ERROR_CODE_FILE_UPLOAD_FAIL);
                }
            }

            private boolean dispatchTcpResponse(final long sign, @NonNull final SessionProtoByteMessageWrapper wrapper) {
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
                protected boolean doNotNullProcess(@NonNull SessionProtoByteMessageWrapper target) {
                    // check thread state
                    Threads.mustNotUi();

                    final Object protoMessageObject = target.getProtoByteMessageWrapper().getProtoMessageObject();
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
                                    setErrorCode((int) result.getCode());
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
                            setError(GeneralErrorCode.ERROR_CODE_TARGET_NOT_FOUND);
                        }
                    } else {
                        setError(GeneralErrorCode.ERROR_CODE_BIND_ABORT_ID_FAIL);
                    }
                } catch (Throwable e) {
                    IMLog.e(e);
                    setError(GeneralErrorCode.ERROR_CODE_UNKNOWN);
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
                    setError(GeneralErrorCode.ERROR_CODE_TARGET_NOT_FOUND);
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
                            setError(GeneralErrorCode.ERROR_CODE_TARGET_NOT_FOUND);
                        }
                    } else {
                        setError(GeneralErrorCode.ERROR_CODE_UPDATE_SEND_STATUS_FAIL);
                    }
                } catch (Throwable e) {
                    IMLog.e(e);
                    setError(GeneralErrorCode.ERROR_CODE_UNKNOWN);
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
        private final List<SessionMessageObjectWrapperTask> mAllRunningTasks = new ArrayList<>();

        private final TaskQueue mCheckIdleActionQueue = new TaskQueue(1);

        // 需要预处理上传数据的任务队列，如视频，音频，图片。通常是先用 http 协议上传预处理数据，再通过 tcp proto buf 发送消息
        private final TaskQueue mLongTimeTaskQueue = new TaskQueue(2);

        // 不需要预处理的任务队列。通常可以直接通过 tcp proto buf 发送消息
        private final TaskQueue mShortTimeTaskQueue = new TaskQueue(1);

        // 记录消息的发送进度
        private final LruCache<Long, Float> mUnsafeProgress = new LruCache<>(MAX_RUNNING_SIZE * 10);

        private SessionWorker(long sessionUserId) {
            mSessionUserId = sessionUserId;
            LocalSendingMessageProvider.getInstance().updateMessageToFailIfNotSuccess(mSessionUserId);
            LocalSendingMessageProvider.getInstance().removeAllSuccessMessage(mSessionUserId);

            DebugManager.getInstance().addDebugInfoProvider(this);
        }

        @Override
        public void fetchDebugInfo(@NonNull StringBuilder builder) {
            final String tag = Objects.defaultObjectTag(this);
            builder.append(tag).append(" --:\n");
            builder.append("mSessionUserId:").append(this.mSessionUserId).append("\n");
            builder.append("MAX_RUNNING_SIZE:").append(MAX_RUNNING_SIZE).append("\n");
            builder.append("mAllRunningTasks size:").append(this.mAllRunningTasks.size()).append("\n");
            builder.append("mCheckIdleActionQueue --:").append("\n");
            mCheckIdleActionQueue.printDetail(builder);
            builder.append("mCheckIdleActionQueue -- end").append("\n");
            builder.append("mLongTimeTaskQueue --:").append("\n");
            mLongTimeTaskQueue.printDetail(builder);
            builder.append("mLongTimeTaskQueue -- end").append("\n");
            builder.append("mShortTimeTaskQueue --:").append("\n");
            mShortTimeTaskQueue.printDetail(builder);
            builder.append("mShortTimeTaskQueue -- end").append("\n");
            builder.append("mUnsafeProgress size:").append(mUnsafeProgress.size()).append("\n");
            builder.append(tag).append(" -- end\n");
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
        private SessionMessageObjectWrapperTask getTask(final long sign) {
            synchronized (mAllRunningTasks) {
                for (SessionMessageObjectWrapperTask task : mAllRunningTasks) {
                    if (task.mSessionMessageObjectWrapper.mSign == sign) {
                        return task;
                    }
                }
            }
            return null;
        }

        @Nullable
        private SessionMessageObjectWrapperTask getTask(@NonNull LocalSendingMessage localSendingMessage) {
            synchronized (mAllRunningTasks) {
                final long localId = localSendingMessage.localId.get();
                for (SessionMessageObjectWrapperTask task : mAllRunningTasks) {
                    if (localId == task.mSessionMessageObjectWrapper.mLocalSendingMessage.localId.get()) {
                        return task;
                    }
                }
            }
            return null;
        }

        @Nullable
        private SessionMessageObjectWrapperTask removeTask(@NonNull LocalSendingMessage localSendingMessage) {
            synchronized (mAllRunningTasks) {
                final long localId = localSendingMessage.localId.get();
                for (int i = 0; i < mAllRunningTasks.size(); i++) {
                    final SessionMessageObjectWrapperTask task = mAllRunningTasks.get(i);
                    if (localId == task.mSessionMessageObjectWrapper.mLocalSendingMessage.localId.get()) {
                        return mAllRunningTasks.remove(i);
                    }
                }
            }
            return null;
        }

        private boolean dispatchTcpResponse(final long sign, @NonNull final SessionProtoByteMessageWrapper wrapper) {
            synchronized (mAllRunningTasks) {
                final SessionMessageObjectWrapperTask task = getTask(sign);
                if (task == null) {
                    return false;
                }
                if (task.mSessionMessageObjectWrapper.dispatchTcpResponse(sign, wrapper)) {
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
                        final SessionMessageObjectWrapperTask oldTask = getTask(localSendingMessage);
                        if (oldTask != null) {
                            final Throwable e = new IllegalAccessError("unexpected localSendingMessage already exists in task. "
                                    + localSendingMessage + ", oldTask:" + Objects.defaultObjectTag(oldTask));
                            IMLog.e(e);
                            continue;
                        }

                        IMLog.v("found new idle localSendingMessage %s", localSendingMessage);
                        final long sign = SignGenerator.next();
                        final SessionMessageObjectWrapper wrapper = new SessionMessageObjectWrapper(
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

                        final SessionMessageObjectWrapperTask task = new SessionMessageObjectWrapperTask(wrapper) {
                            @Override
                            public void run() {
                                try {
                                    super.run();
                                } catch (Throwable e) {
                                    IMLog.e(e, "unexpected");
                                }
                                wrapper.onTaskEnd();
                                synchronized (mAllRunningTasks) {
                                    final SessionMessageObjectWrapperTask existsTask = removeTask(localSendingMessage);
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

        private class SessionMessageObjectWrapperTask implements Runnable {

            @NonNull
            private final SessionMessageObjectWrapper mSessionMessageObjectWrapper;

            private SessionMessageObjectWrapperTask(@NonNull SessionMessageObjectWrapper sessionMessageObjectWrapper) {
                mSessionMessageObjectWrapper = sessionMessageObjectWrapper;
            }

            @Nullable
            private SessionTcpClient waitTcpClientConnected() {
                final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxyWithBlockOrTimeout();
                if (mSessionMessageObjectWrapper.hasErrorOrAbort()) {
                    return null;
                }

                if (proxy == null) {
                    mSessionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL);
                    return null;
                }

                if (IMSessionManager.getInstance().getSessionUserId() != mSessionUserId) {
                    mSessionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID);
                    return null;
                }

                if (!proxy.isOnline()) {
                    mSessionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR);
                    return null;
                }

                final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
                if (sessionTcpClient == null) {
                    mSessionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN);
                    return null;
                }

                if (mSessionMessageObjectWrapper.hasErrorOrAbort()) {
                    return null;
                }
                return sessionTcpClient;
            }

            @Override
            public void run() {
                try {
                    if (mSessionMessageObjectWrapper.hasErrorOrAbort()) {
                        return;
                    }

                    mSessionMessageObjectWrapper.moveSendStatus(IMConstants.SendStatus.SENDING);
                    if (mSessionMessageObjectWrapper.hasErrorOrAbort()) {
                        return;
                    }

                    final Message message = mSessionMessageObjectWrapper.mMessage;
                    if (message == null) {
                        mSessionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_TARGET_NOT_FOUND);
                        return;
                    }

                    {
                        // wait tcp client connected
                        final SessionTcpClient sessionTcpClient = this.waitTcpClientConnected();
                        if (sessionTcpClient == null) {
                            return;
                        }
                    }

                    final MessagePacket messagePacket = mSessionMessageObjectWrapper.buildMessagePacket();
                    if (messagePacket == null) {
                        mSessionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL);
                        return;
                    }

                    // 通过长连接发送 proto buf
                    final SessionTcpClient sessionTcpClient = this.waitTcpClientConnected();
                    if (sessionTcpClient == null) {
                        return;
                    }

                    if (mSessionMessageObjectWrapper.hasErrorOrAbort()) {
                        return;
                    }

                    sessionTcpClient.sendMessagePacketQuietly(messagePacket);
                    if (messagePacket.getState() != MessagePacket.STATE_WAIT_RESULT) {
                        mSessionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_MESSAGE_PACKET_SEND_FAIL);
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
                    if (e instanceof GeneralErrorCodeException) {
                        mSessionMessageObjectWrapper.setError(((GeneralErrorCodeException) e).errorCode);
                    } else if (mSessionMessageObjectWrapper.mErrorCode == 0) {
                        mSessionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_UNKNOWN);
                    }
                }
            }
        }
    }
}
