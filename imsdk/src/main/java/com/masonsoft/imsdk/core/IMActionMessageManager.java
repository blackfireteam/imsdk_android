package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMActionMessage;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.NotNullTimeoutMessagePacket;
import com.masonsoft.imsdk.core.observable.ActionMessageObservable;
import com.masonsoft.imsdk.core.observable.MessagePacketStateObservable;
import com.masonsoft.imsdk.core.processor.TinyChatRProcessor;
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
 * 指令消息发送队列，并处理对应的消息响应。
 */
public class IMActionMessageManager {

    private static final Singleton<IMActionMessageManager> INSTANCE = new Singleton<IMActionMessageManager>() {
        @Override
        protected IMActionMessageManager create() {
            return new IMActionMessageManager();
        }
    };

    public static IMActionMessageManager getInstance() {
        return INSTANCE.get();
    }

    private final Map<Long, SessionWorker> mSessionWorkerMap = new HashMap<>();

    private IMActionMessageManager() {
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

    public void enqueueActionMessage(
            final long sessionUserId,
            final long sign,
            @NonNull final IMActionMessage actionMessage) {
        getSessionWorker(sessionUserId).enqueueActionMessage(sign, actionMessage);
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
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL, "ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL, "ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID, "ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR, "ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN, "ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_MESSAGE_PACKET_SEND_FAIL, "ERROR_CODE_MESSAGE_PACKET_SEND_FAIL");
            DEFAULT_ERROR_MESSAGE_MAP.put(ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT, "ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT");

            Preconditions.checkArgument(DEFAULT_ERROR_MESSAGE_MAP.size() == sNextErrorCode - FIRST_LOCAL_ERROR_CODE);
        }
        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////
    }

    private static class SessionWorker implements DebugManager.DebugInfoProvider {

        private final long mSessionUserId;
        private final List<FetchMessageObjectWrapperTask> mAllRunningTasks = new ArrayList<>();
        private final TaskQueue mActionQueue = new TaskQueue(1);
        private final TaskQueue mQueue = new TaskQueue(1);

        private SessionWorker(long sessionUserId) {
            mSessionUserId = sessionUserId;

            DebugManager.getInstance().addDebugInfoProvider(this);
        }

        @Override
        public void fetchDebugInfo(@NonNull StringBuilder builder) {
            final String tag = Objects.defaultObjectTag(this);
            builder.append(tag).append(" --:\n");
            builder.append("mSessionUserId:").append(this.mSessionUserId).append("\n");
            builder.append("mAllRunningTasks size:").append(this.mAllRunningTasks.size()).append("\n");
            builder.append("mActionQueue --:").append("\n");
            mActionQueue.printDetail(builder);
            builder.append("mActionQueue -- end").append("\n");
            builder.append("mQueue --:").append("\n");
            mQueue.printDetail(builder);
            builder.append("mQueue -- end").append("\n");
            builder.append(tag).append(" -- end\n");
        }

        public void enqueueActionMessage(final long sign, @NonNull final IMActionMessage actionMessage) {
            mActionQueue.enqueue(new SafetyRunnable(() -> {
                IMLog.v("SessionWorker mActionQueue size:%s, mQueue size:%s",
                        mActionQueue.getCurrentCount(),
                        mQueue.getCurrentCount());

                final ActionMessageObjectWrapper actionMessageObjectWrapper = new ActionMessageObjectWrapper(
                        mSessionUserId,
                        sign,
                        actionMessage
                );
                final FetchMessageObjectWrapperTask task = new FetchMessageObjectWrapperTask(actionMessageObjectWrapper) {
                    @Override
                    public void run() {
                        try {
                            super.run();
                        } catch (Throwable e) {
                            IMLog.e(e, "unexpected");
                        }
                        actionMessageObjectWrapper.onTaskEnd();
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

        private static class ActionMessageObjectWrapper {

            private final long mSessionUserId;
            private final long mSign;
            @NonNull
            private final IMActionMessage mActionMessage;

            public long mErrorCode;
            public String mErrorMessage;

            private final AtomicBoolean mBuildActionMessagePacket = new AtomicBoolean(false);
            @Nullable
            private ActionMessagePacket mActionMessagePacket;

            @NonNull
            private final MessagePacketStateObservable.MessagePacketStateObserver mActionMessagePacketStateObserver = new MessagePacketStateObservable.MessagePacketStateObserver() {
                @Override
                public void onStateChanged(MessagePacket packet, int oldState, int newState) {
                    if (packet != mActionMessagePacket) {
                        final Throwable e = new IllegalAccessError("invalid packet:" + Objects.defaultObjectTag(packet)
                                + ", mActionMessagePacket:" + Objects.defaultObjectTag(mActionMessagePacket));
                        IMLog.e(e);
                        return;
                    }

                    boolean notify = false;
                    final ActionMessagePacket actionMessagePacket = (ActionMessagePacket) packet;
                    if (newState == MessagePacket.STATE_FAIL) {
                        // 消息发送失败
                        notify = true;
                        IMLog.v("onStateChanged STATE_FAIL actionMessagePacket errorCode:%s, errorMessage:%s, timeout:%s",
                                actionMessagePacket.getErrorCode(),
                                actionMessagePacket.getErrorMessage(),
                                actionMessagePacket.isTimeoutTriggered());
                        if (actionMessagePacket.getErrorCode() != 0) {
                            setError(actionMessagePacket.getErrorCode(), actionMessagePacket.getErrorMessage());
                        } else if (actionMessagePacket.isTimeoutTriggered()) {
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
                        synchronized (actionMessagePacket) {
                            actionMessagePacket.notify();
                        }
                    }
                }
            };

            private ActionMessageObjectWrapper(long sessionUserId, long sign, @NonNull IMActionMessage actionMessage) {
                mSessionUserId = sessionUserId;
                mSign = sign;
                mActionMessage = actionMessage;
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
                        ActionMessageObservable.DEFAULT.notifyActionMessageLoading(mSign);
                        break;
                    case IMConstants.SendStatus.SUCCESS:
                        ActionMessageObservable.DEFAULT.notifyActionMessageSuccess(mSign);
                        break;
                    case IMConstants.SendStatus.FAIL:
                        ActionMessageObservable.DEFAULT.notifyActionMessageError(mSign, mErrorCode, mErrorMessage);
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
                final ActionMessagePacket actionMessagePacket = mActionMessagePacket;
                if (actionMessagePacket == null || actionMessagePacket.getState() != MessagePacket.STATE_SUCCESS) {
                    notifySendStatus(IMConstants.SendStatus.FAIL);
                } else {
                    notifySendStatus(IMConstants.SendStatus.SUCCESS);
                }
            }

            @Nullable
            private MessagePacket buildMessagePacket() {
                if (!mBuildActionMessagePacket.weakCompareAndSet(false, true)) {
                    throw new IllegalAccessError("buildMessagePacket only support called once");
                }

                final int actionType = mActionMessage.getActionType();

                if (actionType == IMActionMessage.ACTION_TYPE_REVOKE) {
                    // 撤回一条会话消息(聊天消息)
                    final IMMessage message = (IMMessage) mActionMessage.getActionObject();
                    final Message dbMessage = MessageDatabaseProvider.getInstance().getMessage(
                            message._sessionUserId.get(),
                            message._conversationType.get(),
                            message._targetUserId.get(),
                            message.id.get());

                    final ProtoMessage.Revoke revoke = ProtoMessage.Revoke.newBuilder()
                            .setSign(mSign)
                            .setToUid(dbMessage._targetUserId.get())
                            .setMsgId(dbMessage.remoteMessageId.get())
                            .build();
                    final ProtoByteMessage protoByteMessage = ProtoByteMessage.Type.encode(revoke);
                    final RevokeActionMessagePacket revokeActionMessagePacket = new RevokeActionMessagePacket(
                            protoByteMessage,
                            mSign
                    );
                    mActionMessagePacket = revokeActionMessagePacket;
                    mActionMessagePacket.getMessagePacketStateObservable().registerObserver(mActionMessagePacketStateObserver);
                    return revokeActionMessagePacket;
                }

                final Throwable e = new IllegalAccessError("unknown action type:" + actionType + " " + mActionMessage.getActionObject());
                IMLog.e(e);
                return null;
            }

            private boolean dispatchTcpResponse(final long sign, @NonNull final ProtoByteMessageWrapper wrapper) {
                final ActionMessagePacket actionMessagePacket = mActionMessagePacket;
                if (actionMessagePacket == null) {
                    final Throwable e = new IllegalAccessError(Objects.defaultObjectTag(this) + " unexpected mActionMessagePacket is null");
                    IMLog.e(e);
                    return false;
                }
                if (mSign != sign) {
                    final Throwable e = new IllegalAccessError(Objects.defaultObjectTag(this) + " unexpected sign not match mSign:" + mSign + ", sign:" + sign);
                    IMLog.e(e);
                    return false;
                }
                final boolean result = actionMessagePacket.doProcess(wrapper);
                IMLog.v(Objects.defaultObjectTag(this) + " dispatchTcpResponse actionMessagePacket.doProcess result:%s", result);
                return result;
            }

            private abstract class ActionMessagePacket extends NotNullTimeoutMessagePacket {
                public ActionMessagePacket(ProtoByteMessage protoByteMessage, long sign) {
                    super(protoByteMessage, sign);
                }
            }

            /**
             * 撤回指定消息
             */
            private class RevokeActionMessagePacket extends ActionMessagePacket {

                public RevokeActionMessagePacket(ProtoByteMessage protoByteMessage, long sign) {
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

                    // 接收 ChatR 消息
                    if (protoMessageObject instanceof ProtoMessage.ChatR) {
                        final ProtoMessage.ChatR chatR = (ProtoMessage.ChatR) protoMessageObject;

                        // 校验 sign 是否相等
                        if (chatR.getSign() == getSign()) {
                            synchronized (getStateLock()) {
                                final int state = getState();
                                if (state != STATE_WAIT_RESULT) {
                                    IMLog.e(Objects.defaultObjectTag(this)
                                            + " unexpected. accept with same sign:%s and invalid state:%s", getSign(), stateToString(state));
                                    return false;
                                }

                                if (!doNotNullProcessChatRInternal(chatR)) {
                                    final Throwable e = new IllegalAccessError("unexpected. doNotNullProcessChatRInternal return false. sign:" + getSign());
                                    IMLog.e(e);
                                }
                                moveToState(STATE_SUCCESS);
                            }
                            return true;
                        }
                    }

                    return false;
                }

                private boolean doNotNullProcessChatRInternal(@NonNull ProtoMessage.ChatR chatR) {
                    final TinyChatRProcessor proxy = new TinyChatRProcessor(mSessionUserId);
                    return proxy.doProcess(chatR);
                }
            }
        }

        private class FetchMessageObjectWrapperTask implements Runnable {

            @NonNull
            private final ActionMessageObjectWrapper mFetchMessageObjectWrapper;

            private FetchMessageObjectWrapperTask(@NonNull ActionMessageObjectWrapper fetchMessageObjectWrapper) {
                mFetchMessageObjectWrapper = fetchMessageObjectWrapper;
            }

            @Nullable
            private SessionTcpClient waitTcpClientConnected() {
                final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxyWithBlockOrTimeout();
                if (mFetchMessageObjectWrapper.hasError()) {
                    return null;
                }

                if (proxy == null) {
                    mFetchMessageObjectWrapper.setError(LocalErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL);
                    return null;
                }

                if (IMSessionManager.getInstance().getSessionUserId() != mSessionUserId) {
                    mFetchMessageObjectWrapper.setError(LocalErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID);
                    return null;
                }

                if (!proxy.isOnline()) {
                    mFetchMessageObjectWrapper.setError(LocalErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR);
                    return null;
                }

                final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
                if (sessionTcpClient == null) {
                    mFetchMessageObjectWrapper.setError(LocalErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN);
                    return null;
                }

                if (mFetchMessageObjectWrapper.hasError()) {
                    return null;
                }
                return sessionTcpClient;
            }

            @Override
            public void run() {
                try {
                    if (mFetchMessageObjectWrapper.hasError()) {
                        return;
                    }

                    mFetchMessageObjectWrapper.notifySendStatus(IMConstants.SendStatus.SENDING);

                    {
                        // wait tcp client connected
                        final SessionTcpClient sessionTcpClient = this.waitTcpClientConnected();
                        if (sessionTcpClient == null) {
                            return;
                        }
                    }

                    final MessagePacket messagePacket = mFetchMessageObjectWrapper.buildMessagePacket();
                    if (messagePacket == null) {
                        mFetchMessageObjectWrapper.setError(LocalErrorCode.ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL);
                        return;
                    }

                    // 通过长连接发送 proto buf
                    final SessionTcpClient sessionTcpClient = this.waitTcpClientConnected();
                    if (sessionTcpClient == null) {
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
