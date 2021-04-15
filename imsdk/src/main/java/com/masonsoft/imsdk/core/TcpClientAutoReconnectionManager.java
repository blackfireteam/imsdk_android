package com.masonsoft.imsdk.core;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignInMessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignOutMessagePacket;
import com.masonsoft.imsdk.core.observable.SessionTcpClientObservable;
import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.lang.MultiProcessor;
import com.masonsoft.imsdk.lang.Processor;
import com.masonsoft.imsdk.lang.SafetyRunnable;
import com.masonsoft.imsdk.util.Objects;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.TaskQueue;
import io.github.idonans.core.thread.Threads;

/**
 * 处理长连接的自动重连
 */
public class TcpClientAutoReconnectionManager {

    private static final Singleton<TcpClientAutoReconnectionManager> INSTANCE = new Singleton<TcpClientAutoReconnectionManager>() {
        @Override
        protected TcpClientAutoReconnectionManager create() {
            return new TcpClientAutoReconnectionManager();
        }
    };

    public static TcpClientAutoReconnectionManager getInstance() {
        return INSTANCE.get();
    }

    private final TaskQueue mActionQueue = new TaskQueue(1);
    @SuppressWarnings("FieldCanBeLocal")
    private final SessionTcpClientObservable.SessionTcpClientObserver mSessionTcpClientObserver = new SessionTcpClientObservable.SessionTcpClientObserver() {
        @Override
        public void onConnectionStateChanged(@NonNull SessionTcpClient sessionTcpClient) {
            final int sessionTcpClientState = sessionTcpClient.getState();
            if (sessionTcpClientState == TcpClient.STATE_CLOSED) {
                // 链接关闭，尝试重连
                IMLog.v("TcpClientAutoReconnectionManager onConnectionStateChanged sessionTcpClientState is TcpClient.STATE_CLOSED, call enqueueReconnect");
                enqueueReconnect();
            }
        }

        @Override
        public void onSignInStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignInMessagePacket messagePacket) {
            final int messagePacketState = messagePacket.getState();
            if (messagePacketState == MessagePacket.STATE_FAIL) {
                // 登录失败
                final long messagePacketErrorCode = messagePacket.getErrorCode();
                if (messagePacketErrorCode == 0L) {
                    // 不是由于服务器返回的错误导致的登录失败(如可能是因为登录超时引起的登陆失败)，尝试重连
                    IMLog.v("TcpClientAutoReconnectionManager onSignInStateChanged messagePacketState is MessagePacket.STATE_FAIL, messagePacketErrorCode is 0, call enqueueReconnect");
                    enqueueReconnect();
                }
            }
        }

        @Override
        public void onSignOutStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignOutMessagePacket messagePacket) {
            // ignore
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final DebugManager.DebugInfoProvider mDebugInfoProvider = TcpClientAutoReconnectionManager.this::fetchDebugInfo;

    private TcpClientAutoReconnectionManager() {
        SessionTcpClientObservable.DEFAULT.registerObserver(mSessionTcpClientObserver);
        DebugManager.getInstance().addDebugInfoProvider(mDebugInfoProvider);
    }

    public void attach() {
        IMLog.v("%s attach", Objects.defaultObjectTag(this));
    }

    private void fetchDebugInfo(@NonNull StringBuilder builder) {
        final String tag = Objects.defaultObjectTag(this);
        builder.append(tag).append("--:\n");
        mActionQueue.printDetail(builder);
        builder.append(tag).append("-- end\n");
    }

    /**
     * 请求重新建立长连接
     */
    @AnyThread
    public void enqueueReconnect() {
        this.enqueueReconnect(0L);
    }

    /**
     * 在指定延迟之后请求重新建立长连接
     */
    @AnyThread
    public void enqueueReconnect(long delayMs) {
        IMLog.v("%s enqueueReconnect delayMs:%s", Objects.defaultObjectTag(this), delayMs);
        Threads.postUi(() -> mActionQueue.enqueue(new SafetyRunnable(new ReconnectionTask())), delayMs);
    }

    private static class ReconnectionTask implements Runnable {

        private final MultiProcessor<IMSessionManager.SessionTcpClientProxy> mProcessor;

        private ReconnectionTask() {
            mProcessor = new MultiProcessor<>();
            mProcessor.addLastProcessor(new FixedSessionTcpClientProxyReconnectProcessor());
        }

        @Override
        public void run() {
            final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxy();
            if (!mProcessor.doProcess(proxy)) {
                IMLog.e("unexpected. mProcessor.doProcess(proxy) return false.");
            }
        }
    }

    private abstract static class SessionTcpClientProxyReconnectProcessor implements Processor<IMSessionManager.SessionTcpClientProxy> {

        @Override
        public boolean doProcess(@Nullable IMSessionManager.SessionTcpClientProxy target) {
            if (target == null) {
                reconnectNow(null);
                return true;
            }

            return doNotNullProcess(target);
        }

        private boolean doNotNullProcess(@NonNull IMSessionManager.SessionTcpClientProxy target) {
            final Session session = IMSessionManager.getInstance().getSession();
            if (session == null) {
                // 登录信息无效，不重连。
                IMLog.v("%s session is null, fast return true.", target);
                return true;
            }

            if (target.isAbort()) {
                // 长链接已被主动中断，不重连
                IMLog.v("%s isAbort, fast return true.", target);
                return true;
            }

            if (target.isOnline()) {
                // 长连接在线，不重连
                return true;
            }

            final SessionTcpClient sessionTcpClient = target.getSessionTcpClient();
            if (sessionTcpClient != null) {
                final SignOutMessagePacket messagePacket = sessionTcpClient.getSignOutMessagePacket();
                if (messagePacket.isSignOut()) {
                    // 至少客户端已经发起了退出登录的请求，不重连。
                    return true;
                }
            }
            return doReconnect(target);
        }

        protected abstract boolean doReconnect(@NonNull IMSessionManager.SessionTcpClientProxy target);

        public void reconnectNow(@Nullable IMSessionManager.SessionTcpClientProxy target) {
            if (IMSessionManager.getInstance().getSessionTcpClientProxy() == target) {
                IMSessionManager.getInstance().recreateSessionTcpClient();
            } else {
                IMLog.e("ignore. reconnectNow target is another one");
            }
        }
    }

    /**
     * 按照一定频率重连<br>
     * 100ms<br>
     * 1s<br>
     * 5s<br>
     * 10s<br>
     * 30s<br>
     * 60s<br>
     * 60s<br>
     * 60s<br>
     */
    private static class FixedSessionTcpClientProxyReconnectProcessor extends SessionTcpClientProxyReconnectProcessor {

        private static final long[] RETRY_INTERVAL_MS = new long[]{
                100L,
                1000L,
                5000L,
                10 * 1000L,
                30 * 1000L,
                60 * 1000L,
        };

        @Override
        protected boolean doReconnect(@NonNull IMSessionManager.SessionTcpClientProxy target) {
            final IMSessionManager.SessionTcpClientProxyConfig config = IMSessionManager.getInstance().getSessionTcpClientProxyConfig();
            if (config == null) {
                reconnectNow(target);
                return true;
            }

            final int retryCount = config.getRetryCount();
            final int fixedRetryCount = Math.min(retryCount, RETRY_INTERVAL_MS.length);
            final long retryIntervalMs = RETRY_INTERVAL_MS[fixedRetryCount];
            final long intervalMsNow = config.getLastCreateTimeMs() - System.currentTimeMillis();
            final boolean requireRetry = intervalMsNow >= retryIntervalMs;
            if (requireRetry) {
                reconnectNow(target);
            } else {
                final long delayMs = retryIntervalMs - intervalMsNow;
                TcpClientAutoReconnectionManager.getInstance().enqueueReconnect(delayMs);
            }
            return true;
        }

    }

}
