package com.masonsoft.imsdk.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignInMessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignOutMessagePacket;
import com.masonsoft.imsdk.core.observable.SessionTcpClientObservable;
import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.lang.ActivityLifecycleCallbacksAdapter;
import com.masonsoft.imsdk.lang.MultiProcessor;
import com.masonsoft.imsdk.lang.Processor;
import com.masonsoft.imsdk.lang.SafetyRunnable;
import com.masonsoft.imsdk.util.Objects;

import java.lang.ref.WeakReference;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.TaskQueue;
import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.ContextUtil;

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
    private final InternalActivityLifecycleCallbacks mActivityLifecycleCallbacks = new InternalActivityLifecycleCallbacks();

    private Object mNetworkCallbackHolder;

    private TcpClientAutoReconnectionManager() {
        SessionTcpClientObservable.DEFAULT.registerObserver(mSessionTcpClientObserver);
        registerActivityLifecycleCallbacks();

        DebugManager.getInstance().addDebugInfoProvider(mDebugInfoProvider);
    }

    /**
     * 监听 Activity 变化
     */
    private void registerActivityLifecycleCallbacks() {
        try {
            final Application application = (Application) ContextUtil.getContext().getApplicationContext();
            application.registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
        } catch (Throwable e) {
            IMLog.e(e);
        }
    }

    /**
     * 监听网络变化
     */
    private void registerNetworkCallback() {
        try {
            if (mNetworkCallbackHolder != null) {
                return;
            }
            final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {

            };
            ConnectivityManager connectivityManager = (ConnectivityManager) ContextUtil.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            }
        } catch (Throwable e) {
            IMLog.e(e);
        }
    }

    public void attach() {
        IMLog.v("%s attach", Objects.defaultObjectTag(this));
    }

    private class InternalActivityLifecycleCallbacks extends ActivityLifecycleCallbacksAdapter {

        @NonNull
        private WeakReference<Activity> mActivityRef = new WeakReference<>(null);

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            if (mActivityRef.get() == null) {
                mActivityRef = new WeakReference<>(activity);
                // 有可能是进程启动或者恢复后的第一个 Activity 创建了，立即重连
                // 注意：如果最先创建的 Activity 被手动 finish, 这个判断逻辑不能够精确判断出是进程中的第一个 Activity.
                // 这里需要的是一个重连触发时机而已，准确与否都可以。
                enqueueReconnect();

                // 当有 Activity 活动时，才去监听网络变化
                registerNetworkCallback();
            }
        }
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
        Threads.postUi(() -> {
            mActionQueue.skipQueue();
            mActionQueue.enqueue(new SafetyRunnable(new ReconnectionTask()));
        }, delayMs);
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

            try {
                return doReconnect(target);
            } catch (Throwable e) {
                IMLog.e(e);
            }
            return false;
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
     * 按照一定频率重连. 重试的次数越多，那么发起下一次重连请求的间隔就越长，但是不会超过最大值。
     */
    private static class FixedSessionTcpClientProxyReconnectProcessor extends SessionTcpClientProxyReconnectProcessor {

        private static final long[] RETRY_INTERVAL_MS = new long[]{
                100L, // 100ms
                1000L, // 1s
                5000L, // 5s
                10 * 1000L, // 10s
                30 * 1000L, // 30s
                60 * 1000L, // 60s
                2 * 60 * 1000L, // 2min
                5 * 60 * 1000L, // 5min
                10 * 60 * 1000L, // 10min
                30 * 60 * 1000L, // 30min
        };

        @Override
        protected boolean doReconnect(@NonNull IMSessionManager.SessionTcpClientProxy target) {
            final IMSessionManager.SessionTcpClientProxyConfig config = IMSessionManager.getInstance().getSessionTcpClientProxyConfig();
            if (config == null) {
                reconnectNow(target);
                return true;
            }

            final int retryCount = config.getRetryCount();
            final int fixedRetryCount = Math.min(retryCount, RETRY_INTERVAL_MS.length - 1);
            final long retryIntervalMs = RETRY_INTERVAL_MS[fixedRetryCount];
            final long intervalMsNow = System.currentTimeMillis() - config.getLastCreateTimeMs();
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
