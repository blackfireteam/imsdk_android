package com.masonsoft.imsdk.core;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.block.MessageBlock;
import com.masonsoft.imsdk.core.db.Conversation;
import com.masonsoft.imsdk.core.db.ConversationDatabaseProvider;
import com.masonsoft.imsdk.core.db.DatabaseHelper;
import com.masonsoft.imsdk.core.db.DatabaseProvider;
import com.masonsoft.imsdk.core.db.DatabaseSessionWriteLock;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.db.MessageFactory;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.NotNullTimeoutMessagePacket;
import com.masonsoft.imsdk.core.observable.FetchMessageHistoryObservable;
import com.masonsoft.imsdk.core.observable.MessagePacketStateObservable;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.lang.SafetyRunnable;
import com.masonsoft.imsdk.util.Objects;
import com.masonsoft.imsdk.util.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.TaskQueue;
import io.github.idonans.core.thread.Threads;

/**
 * 获取历史消息
 */
public class FetchMessageHistoryManager {

    private static final Singleton<FetchMessageHistoryManager> INSTANCE = new Singleton<FetchMessageHistoryManager>() {
        @Override
        protected FetchMessageHistoryManager create() {
            return new FetchMessageHistoryManager();
        }
    };

    public static FetchMessageHistoryManager getInstance() {
        return INSTANCE.get();
    }

    private final Map<Long, SessionWorker> mSessionWorkerMap = new HashMap<>();

    private FetchMessageHistoryManager() {
    }

    @NonNull
    private SessionWorker getSessionWorker(final long sessionUserId) {
        SessionWorker sessionWorker = mSessionWorkerMap.get(sessionUserId);
        if (sessionWorker != null) {
            return sessionWorker;
        }
        synchronized (mSessionWorkerMap) {
            sessionWorker = mSessionWorkerMap.get(sessionUserId);
            if (sessionWorker == null) {
                sessionWorker = new SessionWorker(sessionUserId);
                mSessionWorkerMap.put(sessionUserId, sessionWorker);
            }
            return sessionWorker;
        }
    }

    public void enqueueFetchMessageHistory(final long sessionUserId,
                                           final long sign,
                                           final int conversationType,
                                           final long targetUserId,
                                           final long blockId,
                                           final boolean history) {
        getSessionWorker(sessionUserId).enqueueFetchMessageHistory(sign, conversationType, targetUserId, blockId, history);
    }

    public boolean dispatchTcpResponse(final long sessionUserId, final long sign, @NonNull final ProtoByteMessageWrapper wrapper) {
        return getSessionWorker(sessionUserId).dispatchTcpResponse(sign, wrapper);
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
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_FILE_UPLOAD_FAIL, "ERROR_CODE_FILE_UPLOAD_FAIL");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_MESSAGE_PACKET_SEND_FAIL, "ERROR_CODE_MESSAGE_PACKET_SEND_FAIL");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT, "ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT");

            Preconditions.checkArgument(DEFAULT_ERROR_MESSAGE_MAP.size() == ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT - FIRST_LOCAL_ERROR_CODE + 1);
        }
        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
    }

    private static class SessionWorker {

        private final long mSessionUserId;
        private final List<FetchMessageObjectWrapperTask> mAllRunningTasks = new ArrayList<>();
        private final TaskQueue mActionQueue = new TaskQueue(1);
        private final TaskQueue mQueue = new TaskQueue(1);

        private SessionWorker(long sessionUserId) {
            mSessionUserId = sessionUserId;
        }

        public void enqueueFetchMessageHistory(final long sign, final int conversationType, final long targetUserId, final long blockId, final boolean history) {
            mActionQueue.enqueue(new SafetyRunnable(() -> {
                IMLog.v("SessionWorker mActionQueue size:%s, mQueue size:%s",
                        mActionQueue.getCurrentCount(),
                        mQueue.getCurrentCount());

                final FetchMessageObjectWrapper fetchMessageObjectWrapper = new FetchMessageObjectWrapper(
                        mSessionUserId,
                        sign,
                        conversationType,
                        targetUserId,
                        blockId,
                        history
                );
                final FetchMessageObjectWrapperTask task = new FetchMessageObjectWrapperTask(fetchMessageObjectWrapper) {
                    @Override
                    public void run() {
                        try {
                            super.run();
                        } catch (Throwable e) {
                            IMLog.e(e, "unexpected");
                        }
                        fetchMessageObjectWrapper.onTaskEnd();
                        synchronized (mAllRunningTasks) {
                            final FetchMessageObjectWrapperTask existsTask = removeTask(sign);
                            if (existsTask == null) {
                                IMLog.e("unexpected removeTask return null sign:%s", sign);
                            } else if (existsTask != this) {
                                IMLog.e("unexpected removeTask return another value sign:%s", sign);
                            } else {
                                IMLog.v("success remove task sign:%s", sign);
                            }
                        }
                    }
                };

                synchronized (mAllRunningTasks) {
                    mAllRunningTasks.add(task);
                    mQueue.enqueue(task);
                }
            }));
        }

        private boolean dispatchTcpResponse(final long sign, @NonNull final ProtoByteMessageWrapper wrapper) {
            synchronized (mAllRunningTasks) {
                final FetchMessageObjectWrapperTask task = getTask(sign);
                if (task == null) {
                    return false;
                }
                if (task.mFetchMessageObjectWrapper.dispatchTcpResponse(sign, wrapper)) {
                    return true;
                }
            }
            return false;
        }

        @Nullable
        private FetchMessageObjectWrapperTask getTask(final long sign) {
            synchronized (mAllRunningTasks) {
                for (FetchMessageObjectWrapperTask task : mAllRunningTasks) {
                    if (task.mFetchMessageObjectWrapper.mSign == sign) {
                        return task;
                    }
                }
            }
            return null;
        }

        @Nullable
        private FetchMessageObjectWrapperTask removeTask(final long sign) {
            synchronized (mAllRunningTasks) {
                for (int i = 0; i < mAllRunningTasks.size(); i++) {
                    final FetchMessageObjectWrapperTask task = mAllRunningTasks.get(i);
                    if (sign == task.mFetchMessageObjectWrapper.mSign) {
                        return mAllRunningTasks.remove(i);
                    }
                }
            }
            return null;
        }

        private static class FetchMessageObjectWrapper {

            // 自动获取历史消息时(block id 为 0), 是否允许获取到该会话的第一条消息(否则获取到 remoteShowMessageId 即可)
            private static final boolean AUTO_FETCH_HISTORY_TO_START = false;

            private final long mSessionUserId;
            private final long mSign;
            private final int mConversationType;
            private final long mTargetUserId;
            private final long mBlockId;
            private final boolean mHistory;

            // 本地数据库中存在的与 mBlockId 处于同一个 block 的 min remote message id
            private long mLocalMinRemoteMessageIdWithSameBlockId;
            // 本地数据库中存在的与 mBlockId 处于同一个 block 的 max remote message id
            private long mLocalMaxRemoteMessageIdWithSameBlockId;

            private long mRemoteMessageStart;
            private long mRemoteMessageEnd;

            public long mErrorCode;
            public String mErrorMessage;

            private final AtomicBoolean mBuildFetchMessageHistoryMessagePacket = new AtomicBoolean(false);
            @Nullable
            private FetchMessageHistoryMessagePacket mFetchMessageHistoryMessagePacket;

            @NonNull
            private final MessagePacketStateObservable.MessagePacketStateObserver mFetchMessageHistoryMessagePacketStateObserver = new MessagePacketStateObservable.MessagePacketStateObserver() {
                @Override
                public void onStateChanged(MessagePacket packet, int oldState, int newState) {
                    if (packet != mFetchMessageHistoryMessagePacket) {
                        final Throwable e = new IllegalAccessError("invalid packet:" + Objects.defaultObjectTag(packet)
                                + ", mFetchMessageHistoryMessagePacket:" + Objects.defaultObjectTag(mFetchMessageHistoryMessagePacket));
                        IMLog.e(e);
                        return;
                    }

                    boolean notify = false;
                    final FetchMessageHistoryMessagePacket fetchMessageHistoryMessagePacket = (FetchMessageHistoryMessagePacket) packet;
                    if (newState == MessagePacket.STATE_FAIL) {
                        // 消息发送失败
                        notify = true;
                        IMLog.v("onStateChanged STATE_FAIL fetchMessageHistoryMessagePacket errorCode:%s, errorMessage:%s, timeout:%s",
                                fetchMessageHistoryMessagePacket.getErrorCode(),
                                fetchMessageHistoryMessagePacket.getErrorMessage(),
                                fetchMessageHistoryMessagePacket.isTimeoutTriggered());
                        if (fetchMessageHistoryMessagePacket.getErrorCode() != 0) {
                            setError(fetchMessageHistoryMessagePacket.getErrorCode(), fetchMessageHistoryMessagePacket.getErrorMessage());
                        } else if (fetchMessageHistoryMessagePacket.isTimeoutTriggered()) {
                            setError(LocalErrorCode.ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT);
                        }
                        notifySendStatus(IMConstants.SendStatus.FAIL);
                    } else if (newState == MessagePacket.STATE_SUCCESS) {
                        // 消息发送成功
                        notify = true;
                        notifySendStatus(IMConstants.SendStatus.SUCCESS);
                    }

                    if (notify) {
                        // @see FetchMessageObjectWrapperTask#run -> "// wait message packet result"
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (fetchMessageHistoryMessagePacket) {
                            fetchMessageHistoryMessagePacket.notify();
                        }
                    }
                }
            };

            private FetchMessageObjectWrapper(long sessionUserId, long sign, int conversationType, long targetUserId, long blockId, boolean history) {
                mSessionUserId = sessionUserId;
                mSign = sign;
                mConversationType = conversationType;
                mTargetUserId = targetUserId;
                mBlockId = blockId;
                mHistory = history;
            }

            private boolean hasError() {
                return this.mErrorCode != 0;
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

            private void notifySendStatus(int sendStatus) {
                switch (sendStatus) {
                    case IMConstants.SendStatus.IDLE:
                    case IMConstants.SendStatus.SENDING:
                        FetchMessageHistoryObservable.DEFAULT.notifyMessageHistoryFetchedLoading(mSign);
                        break;
                    case IMConstants.SendStatus.SUCCESS:
                        FetchMessageHistoryObservable.DEFAULT.notifyMessageHistoryFetchedSuccess(mSign);
                        break;
                    case IMConstants.SendStatus.FAIL:
                        FetchMessageHistoryObservable.DEFAULT.notifyMessageHistoryFetchedError(mSign, mErrorCode, mErrorMessage);
                        break;
                    default:
                        final Throwable e = new IllegalStateException("unexpected send status:" + sendStatus);
                        IMLog.e(e);
                }
            }

            /**
             * 任务执行结束
             */
            private void onTaskEnd() {
                final FetchMessageHistoryMessagePacket fetchMessageHistoryMessagePacket = mFetchMessageHistoryMessagePacket;
                if (fetchMessageHistoryMessagePacket == null || fetchMessageHistoryMessagePacket.getState() != IMConstants.SendStatus.SUCCESS) {
                    notifySendStatus(IMConstants.SendStatus.FAIL);
                } else {
                    notifySendStatus(IMConstants.SendStatus.SUCCESS);
                }
            }

            @Nullable
            private MessagePacket buildMessagePacket() {
                if (!mBuildFetchMessageHistoryMessagePacket.weakCompareAndSet(false, true)) {
                    throw new IllegalAccessError("buildMessagePacket only support called once");
                }

                final Conversation conversation = ConversationDatabaseProvider.getInstance().getConversationByTargetUserId(
                        mSessionUserId,
                        mConversationType,
                        mTargetUserId
                );
                if (conversation == null) {
                    IMLog.e(Objects.defaultObjectTag(this) + " unexpected. conversation is null");
                    return null;
                }

                if (mBlockId <= 0) {
                    // 读取最新一页消息
                    final long remoteMessageEnd = conversation.remoteMessageEnd.get();
                    if (remoteMessageEnd <= 0) {
                        // 没有消息
                        IMLog.v(Objects.defaultObjectTag(this) + " ignore. remoteMessageEnd:" + remoteMessageEnd);
                        return null;
                    }
                    final Message message = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                            mSessionUserId,
                            mConversationType,
                            mTargetUserId,
                            remoteMessageEnd
                    );
                    if (message == null) {
                        // 消息不在本地，从最新的开始获取
                        mRemoteMessageEnd = 0;

                        final Message maxMessage = MessageDatabaseProvider.getInstance().getMaxRemoteMessageId(
                                mSessionUserId,
                                mConversationType,
                                mTargetUserId
                        );
                        if (maxMessage != null) {
                            mRemoteMessageStart = maxMessage.remoteMessageId.get();
                        } else {
                            mRemoteMessageStart = 0;
                        }
                    } else {
                        // 消息在本地
                        // 获取该消息在本地所属 block 的最小 remote message id
                        final long blockId = message.localBlockId.get();
                        Preconditions.checkArgument(blockId > 0);
                        final Message minMessage = MessageDatabaseProvider.getInstance().getMinRemoteMessageIdWithBlockId(
                                mSessionUserId,
                                mConversationType,
                                mTargetUserId,
                                blockId
                        );
                        Preconditions.checkNotNull(minMessage);
                        mRemoteMessageEnd = minMessage.remoteMessageId.get();

                        final long remoteMessageStart = conversation.remoteMessageStart.get();
                        if (mRemoteMessageEnd <= remoteMessageStart + 1) {
                            // 所有消息已经获取完整
                            IMLog.v("ignore. all message are loaded. sessionUserId:%s, conversationType:%s, targetUserId:%s",
                                    mSessionUserId, mConversationType, mTargetUserId);
                            return null;
                        }

                        if (!AUTO_FETCH_HISTORY_TO_START) {
                            final long remoteShowMessageId = conversation.remoteShowMessageId.get();
                            if (remoteShowMessageId > 0) {
                                // 获取到 remoteShowMessageId 即止
                                final Message exists = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                                        mSessionUserId,
                                        mConversationType,
                                        mTargetUserId,
                                        remoteShowMessageId
                                );
                                if (exists != null) {
                                    IMLog.v("ignore. already load to remote show message id. sessionUserId:%s, conversationType:%s, targetUserId:%s, remoteShowMessageId:%s",
                                            mSessionUserId, mConversationType, mTargetUserId, remoteShowMessageId);
                                    return null;
                                }

                                // 紧挨着 remoteShowMessageId 的前一条消息(比 remoteShowMessageId 小的)
                                final Message closestLessThanRemoteShowMessageId = MessageDatabaseProvider.getInstance().getClosestLessThanRemoteMessageIdWithRemoteMessageId(
                                        mSessionUserId,
                                        mConversationType,
                                        mTargetUserId,
                                        remoteShowMessageId
                                );
                                if (closestLessThanRemoteShowMessageId != null) {
                                    mRemoteMessageStart = closestLessThanRemoteShowMessageId.remoteMessageId.get();
                                }
                            }
                        }

                        if (mRemoteMessageStart <= 0) {
                            final Message closestLessThanRemoteMessage = MessageDatabaseProvider.getInstance().getClosestLessThanRemoteMessageIdWithRemoteMessageId(
                                    mSessionUserId,
                                    mConversationType,
                                    mTargetUserId,
                                    mRemoteMessageEnd
                            );
                            if (closestLessThanRemoteMessage != null) {
                                // 获取到这一个为止
                                mRemoteMessageStart = closestLessThanRemoteMessage.remoteMessageId.get();
                            } else {
                                // 获取到会话的最开始
                                mRemoteMessageStart = 0;
                            }
                        }
                    }
                } else {
                    final Message minRemoteMessage = MessageDatabaseProvider.getInstance().getMinRemoteMessageIdWithBlockId(
                            mSessionUserId,
                            mConversationType,
                            mTargetUserId,
                            mBlockId
                    );
                    if (minRemoteMessage != null) {
                        mLocalMinRemoteMessageIdWithSameBlockId = minRemoteMessage.remoteMessageId.get();
                    }
                    final Message maxRemoteMessage = MessageDatabaseProvider.getInstance().getMaxRemoteMessageIdWithBlockId(
                            mSessionUserId,
                            mConversationType,
                            mTargetUserId,
                            mBlockId
                    );
                    if (maxRemoteMessage != null) {
                        mLocalMaxRemoteMessageIdWithSameBlockId = maxRemoteMessage.remoteMessageId.get();
                    }

                    if (mHistory) {
                        if (mLocalMinRemoteMessageIdWithSameBlockId > 0) {
                            mRemoteMessageEnd = mLocalMinRemoteMessageIdWithSameBlockId;
                            final long remoteMessageStart = conversation.remoteMessageStart.get();
                            if (mRemoteMessageEnd <= remoteMessageStart + 1) {
                                // 所有消息已经获取完整
                                IMLog.v("ignore. all message are loaded. sessionUserId:%s, conversationType:%s, targetUserId:%s, history:%s",
                                        mSessionUserId, mConversationType, mTargetUserId, mHistory);
                                return null;
                            }

                            final Message closestLessThanRemoteMessage = MessageDatabaseProvider.getInstance().getClosestLessThanRemoteMessageIdWithRemoteMessageId(
                                    mSessionUserId,
                                    mConversationType,
                                    mTargetUserId,
                                    mLocalMinRemoteMessageIdWithSameBlockId
                            );
                            if (closestLessThanRemoteMessage != null) {
                                mRemoteMessageStart = closestLessThanRemoteMessage.remoteMessageId.get();
                            }
                        }
                    } else {
                        if (mLocalMaxRemoteMessageIdWithSameBlockId > 0) {
                            mRemoteMessageStart = mLocalMaxRemoteMessageIdWithSameBlockId;
                            final long remoteMessageEnd = conversation.remoteMessageEnd.get();
                            if (mRemoteMessageStart >= remoteMessageEnd) {
                                // 所有消息已经获取完整
                                IMLog.v("ignore. all message are loaded. sessionUserId:%s, conversationType:%s, targetUserId:%s, history:%s",
                                        mSessionUserId, mConversationType, mTargetUserId, mHistory);
                                return null;
                            }
                            final Message closestGreaterThanRemoteMessage = MessageDatabaseProvider.getInstance().getClosestGreaterThanRemoteMessageIdWithRemoteMessageId(
                                    mSessionUserId,
                                    mConversationType,
                                    mTargetUserId,
                                    mLocalMaxRemoteMessageIdWithSameBlockId
                            );
                            if (closestGreaterThanRemoteMessage != null) {
                                mRemoteMessageEnd = closestGreaterThanRemoteMessage.remoteMessageId.get();
                            } else {
                                mRemoteMessageStart = 0;
                                mRemoteMessageEnd = 0;
                            }
                        }
                    }
                }

                IMLog.v("ProtoMessage.GetHistory sign:%s, targetUserId:%s, blockId:%s," +
                                " remoteMessageStart:%s, remoteMessageEnd:%s," +
                                " localMinRemoteMessageIdWithSameBlockId:%s, localMaxRemoteMessageIdWithSameBlockId:%s," +
                                " history:%s",
                        mSign, mTargetUserId, mBlockId,
                        mRemoteMessageStart, mRemoteMessageEnd,
                        mLocalMinRemoteMessageIdWithSameBlockId, mLocalMaxRemoteMessageIdWithSameBlockId,
                        mHistory);

                final ProtoMessage.GetHistory getHistory = ProtoMessage.GetHistory.newBuilder()
                        .setSign(mSign)
                        .setToUid(mTargetUserId)
                        .setMsgStart(mRemoteMessageStart)
                        .setMsgEnd(mRemoteMessageEnd)
                        .build();
                final ProtoByteMessage protoByteMessage = ProtoByteMessage.Type.encode(getHistory);
                final FetchMessageHistoryMessagePacket fetchMessageHistoryMessagePacket = new FetchMessageHistoryMessagePacket(
                        protoByteMessage,
                        mSign);
                mFetchMessageHistoryMessagePacket = fetchMessageHistoryMessagePacket;
                mFetchMessageHistoryMessagePacket.getMessagePacketStateObservable().registerObserver(mFetchMessageHistoryMessagePacketStateObserver);
                return fetchMessageHistoryMessagePacket;
            }

            private boolean dispatchTcpResponse(final long sign, @NonNull final ProtoByteMessageWrapper wrapper) {
                final FetchMessageHistoryMessagePacket fetchMessageHistoryMessagePacket = mFetchMessageHistoryMessagePacket;
                if (fetchMessageHistoryMessagePacket == null) {
                    final Throwable e = new IllegalAccessError(Objects.defaultObjectTag(this) + " unexpected mFetchMessageHistoryMessagePacket is null");
                    IMLog.e(e);
                    return false;
                }
                if (mSign != sign) {
                    final Throwable e = new IllegalAccessError(Objects.defaultObjectTag(this) + " unexpected sign not match mSign:" + mSign + ", sign:" + sign);
                    IMLog.e(e);
                    return false;
                }
                final boolean result = fetchMessageHistoryMessagePacket.doProcess(wrapper);
                IMLog.v(Objects.defaultObjectTag(this) + " dispatchTcpResponse fetchMessageHistoryMessagePacket.doProcess result:%s", result);
                return result;
            }

            private class FetchMessageHistoryMessagePacket extends NotNullTimeoutMessagePacket {

                public FetchMessageHistoryMessagePacket(ProtoByteMessage protoByteMessage, long sign) {
                    super(protoByteMessage, sign);
                }

                @Override
                protected boolean doNotNullProcess(@NonNull ProtoByteMessageWrapper target) {
                    Threads.mustNotUi();

                    final Object protoMessageObject = target.getProtoMessageObject();
                    if (protoMessageObject == null) {
                        return false;
                    }

                    // 接收 Result 消息
                    if (protoMessageObject instanceof ProtoMessage.Result) {
                        final ProtoMessage.Result result = (ProtoMessage.Result) protoMessageObject;

                        // 校验 sign 是否相等
                        if (result.getSign() == getSign()) {
                            synchronized (getStateLock()) {
                                final int state = getState();
                                if (state != STATE_WAIT_RESULT) {
                                    IMLog.e(Objects.defaultObjectTag(this)
                                            + " unexpected. accept with same sign:%s and invalid state:%s", getSign(), stateToString(state));
                                    return false;
                                }

                                if (result.getCode() != 0) {
                                    setErrorCode(result.getCode());
                                    setErrorMessage(result.getMsg());
                                    IMLog.e(Objects.defaultObjectTag(this) +
                                            " unexpected. errorCode:%s, errorMessage:%s", result.getCode(), result.getMsg());
                                } else {
                                    final Throwable e = new IllegalArgumentException(Objects.defaultObjectTag(this) + " unexpected. result code is 0.");
                                    IMLog.e(e);
                                }
                                moveToState(STATE_FAIL);
                            }
                            return true;
                        }
                    }

                    // 接收 ChatRBatch 消息
                    if (protoMessageObject instanceof ProtoMessage.ChatRBatch) {
                        final ProtoMessage.ChatRBatch chatRBatch = (ProtoMessage.ChatRBatch) protoMessageObject;

                        // 校验 sign 是否相等
                        if (chatRBatch.getSign() == getSign()) {
                            synchronized (getStateLock()) {
                                final int state = getState();
                                if (state != STATE_WAIT_RESULT) {
                                    IMLog.e(Objects.defaultObjectTag(this)
                                            + " unexpected. accept with same sign:%s and invalid state:%s", getSign(), stateToString(state));
                                    return false;
                                }

                                if (!doNotNullProcessChatRBatchInternal(chatRBatch)) {
                                    final Throwable e = new IllegalAccessError("unexpected. doNotNullProcessChatRBatchInternal return false. sign:" + getSign());
                                    IMLog.e(e);
                                }
                                moveToState(STATE_SUCCESS);
                            }
                            return true;
                        }
                    }

                    return false;
                }

                private boolean doNotNullProcessChatRBatchInternal(
                        @NonNull ProtoMessage.ChatRBatch chatRBatch) {
                    final List<Message> messageList = new ArrayList<>();
                    final List<ProtoMessage.ChatR> chatRList = chatRBatch.getMsgsList();
                    if (chatRList != null) {
                        for (ProtoMessage.ChatR chatR : chatRList) {
                            if (chatR != null) {
                                messageList.add(MessageFactory.create(chatR));
                            }
                        }
                    }

                    if (messageList.isEmpty()) {
                        IMLog.w(new IllegalArgumentException("unexpected ChatRBatch is empty"));

                        // mRemoteMessageStart 与 mRemoteMessageEnd 直接连通
                        if (mRemoteMessageStart > 0 && mRemoteMessageEnd > 0) {
                            long blockId = 0L;
                            {
                                final Message message = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                                        mSessionUserId,
                                        mConversationType,
                                        mTargetUserId,
                                        mRemoteMessageEnd
                                );
                                if (message == null) {
                                    IMLog.e(new IllegalArgumentException("unexpected message[mRemoteMessageEnd:" + mRemoteMessageEnd + "] not found"));
                                } else {
                                    blockId = message.localBlockId.get();
                                }
                            }
                            if (blockId > 0) {
                                final Message message = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                                        mSessionUserId,
                                        mConversationType,
                                        mTargetUserId,
                                        mRemoteMessageStart
                                );
                                if (message == null) {
                                    IMLog.e(new IllegalArgumentException("unexpected message[mRemoteMessageStart:" + mRemoteMessageStart + "] not found"));
                                } else {
                                    final long preBlockId = message.localBlockId.get();
                                    if (preBlockId == blockId) {
                                        IMLog.e(new IllegalArgumentException("unexpected message[mRemoteMessageStart:" + mRemoteMessageStart
                                                + ", mRemoteMessageEnd:" + mRemoteMessageEnd + "] already has same block id:" + preBlockId));
                                    } else {
                                        IMLog.v("start update block id %s -> %s. mRemoteMessageStart:%s, mRemoteMessageEnd:%s",
                                                preBlockId, blockId, mRemoteMessageStart, mRemoteMessageEnd);
                                        if (!MessageDatabaseProvider.getInstance().updateBlockId(
                                                mSessionUserId,
                                                mConversationType,
                                                mTargetUserId,
                                                preBlockId,
                                                blockId)) {
                                            IMLog.e(Objects.defaultObjectTag(this) + " unexpected. updateBlockId return false");
                                        }
                                    }
                                }
                            }
                        }
                        return true;
                    }

                    // messageList 中的所有消息是连续的，并且是有序的(按照 msg id 有序)

                    // server message id 最大和最小的那一条(对比第一条和最后一条)
                    final Message minMessage, maxMessage;
                    {
                        final Message firstMessage = messageList.get(0);
                        final Message lastMessage = messageList.get(messageList.size() - 1);
                        if (firstMessage.remoteMessageId.get() < lastMessage.remoteMessageId.get()) {
                            minMessage = firstMessage;
                            maxMessage = lastMessage;
                        } else {
                            minMessage = lastMessage;
                            maxMessage = firstMessage;
                        }
                    }

                    final long sessionUserId = mSessionUserId;
                    final long fromUserId = maxMessage.fromUserId.get();
                    final long toUserId = maxMessage.toUserId.get();
                    if (fromUserId != sessionUserId && toUserId != sessionUserId) {
                        IMLog.e("unexpected. sessionUserId:%s invalid fromUserId and toUserId %s", sessionUserId, maxMessage);
                        return false;
                    }

                    final boolean received = fromUserId != sessionUserId;
                    final long targetUserId = received ? fromUserId : toUserId;
                    final int conversationType = IMConstants.ConversationType.C2C;

                    for (Message message : messageList) {
                        message.applyLogicField(sessionUserId, conversationType, targetUserId);

                        final int messageType = message.messageType.get();
                        if (messageType == IMConstants.MessageType.REVOKE_MESSAGE) {
                            // 撤回了一条消息，如果目标消息在本地，则需要将目标消息的类型修改为已撤回
                            final long targetMessageId = Long.parseLong(message.body.get().trim());
                            final Message dbMessage = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                                    sessionUserId,
                                    conversationType,
                                    targetUserId,
                                    targetMessageId);
                            if (dbMessage != null) {
                                if (dbMessage.messageType.get() != IMConstants.MessageType.REVOKED) {
                                    // 将本地目标消息修改为已撤回
                                    final DatabaseHelper databaseHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
                                    synchronized (DatabaseSessionWriteLock.getInstance().getSessionWriteLock(databaseHelper)) {
                                        final Message messageUpdate = new Message();
                                        messageUpdate.localId.apply(dbMessage.localId);
                                        messageUpdate.messageType.set(IMConstants.MessageType.REVOKED);
                                        MessageDatabaseProvider.getInstance().updateMessage(
                                                sessionUserId,
                                                conversationType,
                                                targetUserId,
                                                messageUpdate);
                                    }
                                }
                            }
                        }
                    }

                    long bestBlockId = 0L;
                    if (mRemoteMessageEnd > 0) {
                        // 使用与 mRemoteMessageEnd 相同的 blockId
                        final Message message = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                                mSessionUserId,
                                mConversationType,
                                mTargetUserId,
                                mRemoteMessageEnd
                        );
                        if (message == null) {
                            IMLog.e("unexpected. message is null, mRemoteMessageEnd:%s", mRemoteMessageEnd);
                        } else {
                            bestBlockId = message.localBlockId.get();
                        }
                    }

                    final long blockId;
                    if (bestBlockId > 0) {
                        blockId = bestBlockId;
                    } else {
                        blockId = MessageBlock.generateBlockId(
                                sessionUserId,
                                conversationType,
                                targetUserId,
                                maxMessage.remoteMessageId.get()
                        );
                    }
                    Preconditions.checkArgument(blockId > 0);

                    final DatabaseHelper databaseHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
                    synchronized (DatabaseSessionWriteLock.getInstance().getSessionWriteLock(databaseHelper)) {
                        final SQLiteDatabase database = databaseHelper.getDBHelper().getWritableDatabase();
                        database.beginTransaction();
                        try {
                            for (Message message : messageList) {
                                final long remoteMessageId = message.remoteMessageId.get();
                                // 设置 block id
                                message.localBlockId.set(blockId);

                                final Message dbMessage = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                                        sessionUserId,
                                        conversationType,
                                        targetUserId,
                                        remoteMessageId);
                                if (dbMessage == null) {
                                    // 如果消息在本地不存在，则入库
                                    if (!MessageDatabaseProvider.getInstance().insertMessage(
                                            sessionUserId,
                                            conversationType,
                                            targetUserId,
                                            message)) {
                                        final Throwable e = new IllegalAccessError("unexpected insertMessage return false " + message);
                                        IMLog.e(e);
                                    }
                                } else {
                                    // 消息已经存在
                                    // 回写 localId
                                    message.localId.set(dbMessage.localId.get());

                                    // 更新必要字段
                                    boolean requireUpdate = false;
                                    final Message messageUpdate = new Message();
                                    messageUpdate.applyLogicField(sessionUserId, conversationType, targetUserId);
                                    messageUpdate.localId.set(dbMessage.localId.get());

                                    {
                                        // 校验是否需要更新 message type
                                        final int dbMessageType = dbMessage.messageType.get();
                                        final int newMessageType = message.messageType.get();
                                        if (dbMessageType != newMessageType) {
                                            requireUpdate = true;
                                            messageUpdate.messageType.set(newMessageType);
                                        }
                                    }
                                    {
                                        // 校验是否需要更新 block id
                                        final long dbMessageBlockId = dbMessage.localBlockId.get();
                                        final long newMessageBlockId = message.localBlockId.get();
                                        Preconditions.checkArgument(newMessageBlockId > 0);
                                        if (dbMessageBlockId != newMessageBlockId) {
                                            requireUpdate = true;
                                            messageUpdate.localBlockId.set(newMessageBlockId);
                                        }
                                    }

                                    if (requireUpdate) {
                                        if (!MessageDatabaseProvider.getInstance().updateMessage(
                                                sessionUserId, conversationType, targetUserId, messageUpdate)) {
                                            final Throwable e = new IllegalAccessError("unexpected updateMessage return false " + messageUpdate);
                                            IMLog.e(e);
                                        }
                                    }
                                }
                            }

                            // expand block id
                            MessageBlock.expandBlockId(sessionUserId, conversationType, targetUserId, minMessage.remoteMessageId.get());

                            database.setTransactionSuccessful();

                            try {
                                // 更新对应会话的最后一条关联消息
                                IMConversationManager.getInstance().updateConversationLastMessage(
                                        sessionUserId,
                                        conversationType,
                                        targetUserId,
                                        maxMessage.localId.get()
                                );
                            } catch (Throwable e) {
                                IMLog.e(e);
                            }
                        } finally {
                            database.endTransaction();
                        }
                    }

                    return true;
                }
            }
        }

        private class FetchMessageObjectWrapperTask implements Runnable {

            @NonNull
            private final FetchMessageObjectWrapper mFetchMessageObjectWrapper;

            private FetchMessageObjectWrapperTask(@NonNull FetchMessageObjectWrapper fetchMessageObjectWrapper) {
                mFetchMessageObjectWrapper = fetchMessageObjectWrapper;
            }

            @Override
            public void run() {
                try {
                    if (mFetchMessageObjectWrapper.hasError()) {
                        return;
                    }

                    mFetchMessageObjectWrapper.notifySendStatus(IMConstants.SendStatus.SENDING);

                    final MessagePacket messagePacket = mFetchMessageObjectWrapper.buildMessagePacket();
                    if (messagePacket == null) {
                        mFetchMessageObjectWrapper.setError(LocalErrorCode.ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL);
                        return;
                    }

                    // 通过长连接发送 proto buf
                    final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxy();
                    if (proxy == null) {
                        mFetchMessageObjectWrapper.setError(LocalErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL);
                        return;
                    }

                    if (IMSessionManager.getInstance().getSessionUserId() != mSessionUserId) {
                        mFetchMessageObjectWrapper.setError(LocalErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID);
                        return;
                    }

                    if (!proxy.isOnline()) {
                        mFetchMessageObjectWrapper.setError(LocalErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR);
                        return;
                    }

                    final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
                    if (sessionTcpClient == null) {
                        mFetchMessageObjectWrapper.setError(LocalErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN);
                        return;
                    }

                    if (mFetchMessageObjectWrapper.hasError()) {
                        return;
                    }

                    sessionTcpClient.sendMessagePacketQuietly(messagePacket);
                    if (messagePacket.getState() != MessagePacket.STATE_WAIT_RESULT) {
                        mFetchMessageObjectWrapper.setError(LocalErrorCode.ERROR_CODE_MESSAGE_PACKET_SEND_FAIL);
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
                        mFetchMessageObjectWrapper.setError(((LocalErrorCodeException) e).mErrorCode);
                    } else if (mFetchMessageObjectWrapper.mErrorCode == 0) {
                        mFetchMessageObjectWrapper.setError(LocalErrorCode.ERROR_CODE_UNKNOWN);
                    }
                }
            }
        }
    }

}
