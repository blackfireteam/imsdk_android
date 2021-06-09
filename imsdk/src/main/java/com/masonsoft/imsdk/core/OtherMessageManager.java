package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.TimeoutMessagePacket;
import com.masonsoft.imsdk.core.observable.MessagePacketStateObservable;
import com.masonsoft.imsdk.core.observable.OtherMessageObservable;
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

/**
 * 没有预置的所有其它消息的发送队列，并处理对应的消息响应。
 */
public class OtherMessageManager {

    private static final Singleton<OtherMessageManager> INSTANCE = new Singleton<OtherMessageManager>() {
        @Override
        protected OtherMessageManager create() {
            return new OtherMessageManager();
        }
    };

    public static OtherMessageManager getInstance() {
        return INSTANCE.get();
    }

    private final Map<Long, SessionWorker> mSessionWorkerMap = new HashMap<>();

    private OtherMessageManager() {
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

    public void enqueueOtherMessage(
            final long sessionUserId,
            final long sign,
            @NonNull final OtherMessage otherMessage) {
        getSessionWorker(sessionUserId).enqueueOtherMessage(sign, otherMessage);
    }

    public boolean dispatchTcpResponse(final long sign, @NonNull final SessionProtoByteMessageWrapper wrapper) {
        return getSessionWorker(wrapper.getSessionUserId()).dispatchTcpResponse(sign, wrapper);
    }

    private static class SessionWorker implements DebugManager.DebugInfoProvider {

        private final long mSessionUserId;
        private final List<OtherMessageObjectWrapperTask> mAllRunningTasks = new ArrayList<>();
        private final TaskQueue mActionQueue = new TaskQueue(1);
        private final TaskQueue mQueue = new TaskQueue(5);

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

        public void enqueueOtherMessage(final long sign, @NonNull final OtherMessage otherMessage) {
            mActionQueue.enqueue(new SafetyRunnable(() -> {
                IMLog.v("SessionWorker mActionQueue size:%s, mQueue size:%s",
                        mActionQueue.getCurrentCount(),
                        mQueue.getCurrentCount());

                final OtherMessageObjectWrapper otherMessageObjectWrapper = new OtherMessageObjectWrapper(
                        mSessionUserId,
                        sign,
                        otherMessage
                );
                final OtherMessageObjectWrapperTask task = new OtherMessageObjectWrapperTask(otherMessageObjectWrapper) {
                    @Override
                    public void run() {
                        try {
                            super.run();
                        } catch (Throwable e) {
                            IMLog.e(e, "unexpected");
                        }
                        otherMessageObjectWrapper.onTaskEnd();
                        synchronized (mAllRunningTasks) {
                            final OtherMessageObjectWrapperTask existsTask = removeTask(sign);
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

        private boolean dispatchTcpResponse(final long sign, @NonNull final SessionProtoByteMessageWrapper wrapper) {
            synchronized (mAllRunningTasks) {
                final OtherMessageObjectWrapperTask task = getTask(sign);
                if (task == null) {
                    return false;
                }
                if (task.mOtherMessageObjectWrapper.dispatchTcpResponse(sign, wrapper)) {
                    return true;
                }
            }
            return false;
        }

        @Nullable
        private OtherMessageObjectWrapperTask getTask(final long sign) {
            synchronized (mAllRunningTasks) {
                for (OtherMessageObjectWrapperTask task : mAllRunningTasks) {
                    if (task.mOtherMessageObjectWrapper.mSign == sign) {
                        return task;
                    }
                }
            }
            return null;
        }

        @Nullable
        private OtherMessageObjectWrapperTask removeTask(final long sign) {
            synchronized (mAllRunningTasks) {
                for (int i = 0; i < mAllRunningTasks.size(); i++) {
                    final OtherMessageObjectWrapperTask task = mAllRunningTasks.get(i);
                    if (sign == task.mOtherMessageObjectWrapper.mSign) {
                        return mAllRunningTasks.remove(i);
                    }
                }
            }
            return null;
        }

        private static class OtherMessageObjectWrapper {

            private final long mSessionUserId;
            private final long mSign;
            @NonNull
            private final OtherMessage mOtherMessage;

            public int mErrorCode;
            public String mErrorMessage;

            private final AtomicBoolean mBuildOtherMessagePacket = new AtomicBoolean(false);
            @Nullable
            private MessagePacket mOtherMessagePacket;

            @NonNull
            private final MessagePacketStateObservable.MessagePacketStateObserver mOtherMessagePacketStateObserver = new MessagePacketStateObservable.MessagePacketStateObserver() {
                @Override
                public void onStateChanged(MessagePacket packet, int oldState, int newState) {
                    if (packet != mOtherMessagePacket) {
                        final Throwable e = new IllegalStateException("invalid packet:" + Objects.defaultObjectTag(packet)
                                + ", mOtherMessagePacket:" + Objects.defaultObjectTag(mOtherMessagePacket));
                        IMLog.e(e);
                        return;
                    }

                    boolean notify = false;
                    final TimeoutMessagePacket otherMessagePacket = (TimeoutMessagePacket) packet;
                    if (newState == MessagePacket.STATE_FAIL) {
                        // 消息发送失败
                        notify = true;
                        IMLog.v("onStateChanged STATE_FAIL otherMessagePacket errorCode:%s, errorMessage:%s, timeout:%s",
                                otherMessagePacket.getErrorCode(),
                                otherMessagePacket.getErrorMessage(),
                                otherMessagePacket.isTimeoutTriggered());
                        if (otherMessagePacket.getErrorCode() != 0) {
                            setError(otherMessagePacket.getErrorCode(), otherMessagePacket.getErrorMessage());
                        } else if (otherMessagePacket.isTimeoutTriggered()) {
                            setError(GeneralErrorCode.ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT);
                        }
                        notifySendStatus(IMConstants.SendStatus.FAIL);
                    } else if (newState == MessagePacket.STATE_SUCCESS) {
                        // 消息发送成功
                        notify = true;
                        notifySendStatus(IMConstants.SendStatus.SUCCESS);
                    }

                    if (notify) {
                        // @see OtherMessageObjectWrapperTask#run -> "// wait message packet result"
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (otherMessagePacket) {
                            otherMessagePacket.notify();
                        }
                    }
                }
            };

            private OtherMessageObjectWrapper(long sessionUserId, long sign, @NonNull OtherMessage otherMessage) {
                mSessionUserId = sessionUserId;
                mSign = sign;
                mOtherMessage = otherMessage;
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
                        OtherMessageObservable.DEFAULT.notifyOtherMessageLoading(mSign, mOtherMessage);
                        break;
                    case IMConstants.SendStatus.SUCCESS:
                        OtherMessageObservable.DEFAULT.notifyOtherMessageSuccess(mSign, mOtherMessage);
                        break;
                    case IMConstants.SendStatus.FAIL:
                        OtherMessageObservable.DEFAULT.notifyOtherMessageError(mSign, mOtherMessage, mErrorCode, mErrorMessage);
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
                final MessagePacket otherMessagePacket = mOtherMessagePacket;
                if (otherMessagePacket == null || otherMessagePacket.getState() != MessagePacket.STATE_SUCCESS) {
                    notifySendStatus(IMConstants.SendStatus.FAIL);
                } else {
                    notifySendStatus(IMConstants.SendStatus.SUCCESS);
                }
            }

            @Nullable
            private MessagePacket buildMessagePacket() {
                if (!mBuildOtherMessagePacket.weakCompareAndSet(false, true)) {
                    throw new IllegalStateException("buildMessagePacket only support called once");
                }

                mOtherMessagePacket = mOtherMessage.getMessagePacket();
                mOtherMessagePacket.getMessagePacketStateObservable().registerObserver(mOtherMessagePacketStateObserver);
                return mOtherMessagePacket;
            }

            private boolean dispatchTcpResponse(final long sign, @NonNull final SessionProtoByteMessageWrapper wrapper) {
                final MessagePacket otherMessagePacket = mOtherMessagePacket;
                if (otherMessagePacket == null) {
                    final Throwable e = new IllegalStateException(Objects.defaultObjectTag(this) + " unexpected mOtherMessagePacket is null");
                    IMLog.e(e);
                    return false;
                }
                if (mSign != sign) {
                    final Throwable e = new IllegalStateException(Objects.defaultObjectTag(this) + " unexpected sign not match mSign:" + mSign + ", sign:" + sign);
                    IMLog.e(e);
                    return false;
                }
                final boolean result = otherMessagePacket.doProcess(wrapper);
                IMLog.v(Objects.defaultObjectTag(this) + " dispatchTcpResponse otherMessagePacket.doProcess result:%s", result);
                return result;
            }
        }

        private class OtherMessageObjectWrapperTask implements Runnable {

            @NonNull
            private final OtherMessageObjectWrapper mOtherMessageObjectWrapper;

            private OtherMessageObjectWrapperTask(@NonNull OtherMessageObjectWrapper otherMessageObjectWrapper) {
                mOtherMessageObjectWrapper = otherMessageObjectWrapper;
            }

            @Nullable
            private SessionTcpClient waitTcpClientConnected() {
                final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxyWithBlockOrTimeout();
                if (mOtherMessageObjectWrapper.hasError()) {
                    return null;
                }

                if (proxy == null) {
                    mOtherMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL);
                    return null;
                }

                if (IMSessionManager.getInstance().getSessionUserId() != mSessionUserId) {
                    mOtherMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_SESSION_INVALID);
                    return null;
                }

                if (!proxy.isOnline()) {
                    mOtherMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR);
                    return null;
                }

                final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
                if (sessionTcpClient == null) {
                    mOtherMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_ERROR_UNKNOWN);
                    return null;
                }

                if (mOtherMessageObjectWrapper.hasError()) {
                    return null;
                }
                return sessionTcpClient;
            }

            @Override
            public void run() {
                try {
                    if (mOtherMessageObjectWrapper.hasError()) {
                        return;
                    }

                    mOtherMessageObjectWrapper.notifySendStatus(IMConstants.SendStatus.SENDING);

                    {
                        // wait tcp client connected
                        final SessionTcpClient sessionTcpClient = this.waitTcpClientConnected();
                        if (sessionTcpClient == null) {
                            return;
                        }
                    }

                    final MessagePacket messagePacket = mOtherMessageObjectWrapper.buildMessagePacket();
                    if (messagePacket == null) {
                        mOtherMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_MESSAGE_PACKET_BUILD_FAIL);
                        return;
                    }

                    // 通过长连接发送 proto buf
                    final SessionTcpClient sessionTcpClient = this.waitTcpClientConnected();
                    if (sessionTcpClient == null) {
                        return;
                    }

                    sessionTcpClient.sendMessagePacketQuietly(messagePacket);
                    if (messagePacket.getState() != MessagePacket.STATE_WAIT_RESULT) {
                        mOtherMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_MESSAGE_PACKET_SEND_FAIL);
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
                        mOtherMessageObjectWrapper.setError(((GeneralErrorCodeException) e).errorCode);
                    } else if (mOtherMessageObjectWrapper.mErrorCode == 0) {
                        mOtherMessageObjectWrapper.setError(GeneralErrorCode.ERROR_CODE_UNKNOWN);
                    }
                }
            }
        }
    }

}
