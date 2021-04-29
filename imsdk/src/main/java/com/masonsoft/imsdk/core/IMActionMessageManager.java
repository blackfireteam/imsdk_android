package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMActionMessage;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.NotNullTimeoutMessagePacket;
import com.masonsoft.imsdk.core.observable.ActionMessageObservable;
import com.masonsoft.imsdk.core.observable.MessagePacketStateObservable;
import com.masonsoft.imsdk.core.processor.TinyChatRProcessor;
import com.masonsoft.imsdk.core.processor.TinyLastReadMsgProcessor;
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
            final long sign,
            @NonNull final IMActionMessage actionMessage) {
        getSessionWorker(actionMessage.getSessionUserId()).enqueueActionMessage(sign, actionMessage);
    }

    public boolean dispatchTcpResponse(
            final long sign,
            @NonNull final SessionProtoByteMessageWrapper wrapper) {
        return getSessionWorker(wrapper.getSessionUserId()).dispatchTcpResponse(sign, wrapper);
    }

    private static class SessionWorker implements DebugManager.DebugInfoProvider {

        private final long mSessionUserId;
        private final List<ActionMessageObjectWrapperTask> mAllRunningTasks = new ArrayList<>();
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
                final ActionMessageObjectWrapperTask task = new ActionMessageObjectWrapperTask(actionMessageObjectWrapper) {
                    @Override
                    public void run() {
                        try {
                            super.run();
                        } catch (Throwable e) {
                            IMLog.e(e, "unexpected");
                        }
                        actionMessageObjectWrapper.onTaskEnd();
                        synchronized (mAllRunningTasks) {
                            final ActionMessageObjectWrapperTask existsTask = removeTask(sign);
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

        private boolean dispatchTcpResponse(
                final long sign,
                @NonNull final SessionProtoByteMessageWrapper wrapper) {
            synchronized (mAllRunningTasks) {
                final ActionMessageObjectWrapperTask task = getTask(sign);
                if (task == null) {
                    return false;
                }
                if (task.mActionMessageObjectWrapper.dispatchTcpResponse(sign, wrapper)) {
                    return true;
                }
            }
            return false;
        }

        @Nullable
        private ActionMessageObjectWrapperTask getTask(final long sign) {
            synchronized (mAllRunningTasks) {
                for (ActionMessageObjectWrapperTask task : mAllRunningTasks) {
                    if (task.mActionMessageObjectWrapper.mSign == sign) {
                        return task;
                    }
                }
            }
            return null;
        }

        @Nullable
        private ActionMessageObjectWrapperTask removeTask(final long sign) {
            synchronized (mAllRunningTasks) {
                for (int i = 0; i < mAllRunningTasks.size(); i++) {
                    final ActionMessageObjectWrapperTask task = mAllRunningTasks.get(i);
                    if (sign == task.mActionMessageObjectWrapper.mSign) {
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

            public int mErrorCode;
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
                            setError(GeneralErrorCode.ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT);
                        }
                        notifySendStatus(IMConstants.SendStatus.FAIL);
                    } else if (newState == MessagePacket.STATE_SUCCESS) {
                        // 消息发送成功
                        notify = true;
                        notifySendStatus(IMConstants.SendStatus.SUCCESS);
                    }

                    if (notify) {
                        // @see ActionMessageObjectWrapperTask#run -> "// wait message packet result"
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

                if (actionType == IMActionMessage.ACTION_TYPE_MARK_AS_READ) {
                    // 回执消息已读
                    final IMMessage message = (IMMessage) mActionMessage.getActionObject();
                    final Message dbMessage = MessageDatabaseProvider.getInstance().getClosestLessThanTargetFromRemoteMessageIdWithSeq(
                            mSessionUserId,
                            message._conversationType.get(),
                            message._targetUserId.get(),
                            message.seq.get()
                    );
                    if (dbMessage == null) {
                        // 本地没有对方发送的消息
                        setError(GeneralErrorCode.ERROR_CODE_TARGET_MESSAGE_NOT_FOUND);
                        return null;
                    } else {
                        final ProtoMessage.MsgRead msgRead = ProtoMessage.MsgRead.newBuilder()
                                .setSign(mSign)
                                .setToUid(dbMessage._targetUserId.get())
                                .setMsgId(dbMessage.remoteMessageId.get())
                                .build();
                        final ProtoByteMessage protoByteMessage = ProtoByteMessage.Type.encode(msgRead);
                        final MarkAsReadActionMessagePacket markAsReadActionMessagePacket = new MarkAsReadActionMessagePacket(
                                protoByteMessage,
                                mSign
                        );
                        mActionMessagePacket = markAsReadActionMessagePacket;
                        mActionMessagePacket.getMessagePacketStateObservable().registerObserver(mActionMessagePacketStateObserver);
                        return markAsReadActionMessagePacket;
                    }
                }

                final Throwable e = new IllegalAccessError("unknown action type:" + actionType + " " + mActionMessage.getActionObject());
                IMLog.e(e);
                return null;
            }

            private boolean dispatchTcpResponse(final long sign,
                                                @NonNull final SessionProtoByteMessageWrapper target) {
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
                final boolean result = actionMessagePacket.doProcess(target);
                IMLog.v(Objects.defaultObjectTag(this) + " dispatchTcpResponse actionMessagePacket.doProcess result:%s", result);
                return result;
            }

            private static abstract class ActionMessagePacket extends NotNullTimeoutMessagePacket {
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
                protected boolean doNotNullProcess(@NonNull SessionProtoByteMessageWrapper target) {
                    Threads.mustNotUi();

                    final Object protoMessageObject = target.getProtoByteMessageWrapper().getProtoMessageObject();
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
                                    setErrorCode((int) result.getCode());
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

                                if (!doNotNullProcessChatRInternal(target)) {
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

                private boolean doNotNullProcessChatRInternal(@NonNull SessionProtoByteMessageWrapper target) {
                    final TinyChatRProcessor proxy = new TinyChatRProcessor();
                    return proxy.doProcess(target);
                }
            }

            /**
             * 消息已读回执
             */
            private class MarkAsReadActionMessagePacket extends ActionMessagePacket {

                public MarkAsReadActionMessagePacket(ProtoByteMessage protoByteMessage, long sign) {
                    super(protoByteMessage, sign);
                }

                @Override
                protected boolean doNotNullProcess(@NonNull SessionProtoByteMessageWrapper target) {
                    Threads.mustNotUi();

                    final Object protoMessageObject = target.getProtoByteMessageWrapper().getProtoMessageObject();
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
                                    setErrorCode((int) result.getCode());
                                    setErrorMessage(result.getMsg());
                                    IMLog.e(Objects.defaultObjectTag(this) +
                                            " unexpected. errorCode:%s, errorMessage:%s", result.getCode(), result.getMsg());
                                    moveToState(STATE_FAIL);
                                } else {
                                    // 没有已读消息变动
                                    moveToState(STATE_SUCCESS);
                                }
                            }
                            return true;
                        }
                    }

                    // 接收 LastReadMsg 消息
                    if (protoMessageObject instanceof ProtoMessage.LastReadMsg) {
                        final ProtoMessage.LastReadMsg lastReadMsg = (ProtoMessage.LastReadMsg) protoMessageObject;

                        // 校验 sign 是否相等
                        if (lastReadMsg.getSign() == getSign()) {
                            synchronized (getStateLock()) {
                                final int state = getState();
                                if (state != STATE_WAIT_RESULT) {
                                    IMLog.e(Objects.defaultObjectTag(this)
                                            + " unexpected. accept with same sign:%s and invalid state:%s", getSign(), stateToString(state));
                                    return false;
                                }

                                if (!doNotNullProcessLastReadMsgInternal(lastReadMsg)) {
                                    final Throwable e = new IllegalAccessError("unexpected. doNotNullProcessLastReadMsgInternal return false. sign:" + getSign());
                                    IMLog.e(e);
                                }
                                moveToState(STATE_SUCCESS);
                            }
                            return true;
                        }
                    }

                    return false;
                }

                private boolean doNotNullProcessLastReadMsgInternal(@NonNull ProtoMessage.LastReadMsg lastReadMsg) {
                    final TinyLastReadMsgProcessor proxy = new TinyLastReadMsgProcessor(mSessionUserId);
                    return proxy.doProcess(lastReadMsg);
                }

            }

        }

        private class ActionMessageObjectWrapperTask implements Runnable {

            @NonNull
            private final ActionMessageObjectWrapper mActionMessageObjectWrapper;

            private ActionMessageObjectWrapperTask(@NonNull ActionMessageObjectWrapper actionMessageObjectWrapper) {
                mActionMessageObjectWrapper = actionMessageObjectWrapper;
            }

            @Nullable
            private SessionTcpClient waitTcpClientConnected() {
                final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxyWithBlockOrTimeout();
                if (mActionMessageObjectWrapper.hasError()) {
                    return null;
                }

                if (proxy == null) {
                    mActionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL);
                    return null;
                }

                if (IMSessionManager.getInstance().getSessionUserId() != mSessionUserId) {
                    mActionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID);
                    return null;
                }

                if (!proxy.isOnline()) {
                    mActionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR);
                    return null;
                }

                final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
                if (sessionTcpClient == null) {
                    mActionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN);
                    return null;
                }

                if (mActionMessageObjectWrapper.hasError()) {
                    return null;
                }
                return sessionTcpClient;
            }

            @Override
            public void run() {
                try {
                    if (mActionMessageObjectWrapper.hasError()) {
                        return;
                    }

                    mActionMessageObjectWrapper.notifySendStatus(IMConstants.SendStatus.SENDING);

                    {
                        // wait tcp client connected
                        final SessionTcpClient sessionTcpClient = this.waitTcpClientConnected();
                        if (sessionTcpClient == null) {
                            return;
                        }
                    }

                    final MessagePacket messagePacket = mActionMessageObjectWrapper.buildMessagePacket();
                    if (messagePacket == null) {
                        mActionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL);
                        return;
                    }

                    // 通过长连接发送 proto buf
                    final SessionTcpClient sessionTcpClient = this.waitTcpClientConnected();
                    if (sessionTcpClient == null) {
                        return;
                    }

                    sessionTcpClient.sendMessagePacketQuietly(messagePacket);
                    if (messagePacket.getState() != MessagePacket.STATE_WAIT_RESULT) {
                        mActionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_MESSAGE_PACKET_SEND_FAIL);
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
                        mActionMessageObjectWrapper.setError(((GeneralErrorCodeException) e).errorCode);
                    } else if (mActionMessageObjectWrapper.mErrorCode == 0) {
                        mActionMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_UNKNOWN);
                    }
                }
            }
        }
    }

}
