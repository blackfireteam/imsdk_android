package com.masonsoft.imsdk.core;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.collection.LruCache;

import com.masonsoft.imsdk.core.message.packet.FetchConversationListMessagePacket;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignInMessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignOutMessagePacket;
import com.masonsoft.imsdk.core.observable.ClockObservable;
import com.masonsoft.imsdk.core.observable.FetchConversationListObservable;
import com.masonsoft.imsdk.core.observable.MessagePacketStateObservable;
import com.masonsoft.imsdk.core.observable.SessionObservable;
import com.masonsoft.imsdk.core.observable.SessionTcpClientObservable;
import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.lang.GeneralErrorCode;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.user.UserInfoSyncManager;
import com.masonsoft.imsdk.util.Objects;
import com.masonsoft.imsdk.util.TimeUtil;

import java.util.concurrent.TimeUnit;

import io.github.idonans.core.SimpleAbortSignal;
import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.AbortUtil;
import io.github.idonans.core.util.IOUtil;
import io.github.idonans.core.util.Preconditions;
import io.reactivex.rxjava3.subjects.SingleSubject;

/**
 * 处理登录状态相关内容，当登录状态发生变更时，将强制断开长连接然后发起新的长连接(如果有登录信息)。
 * 并且在读取任意数据时，都将校验当前长连接上通过认证的登陆信息是否与最新的登录信息一致，如果不一致，将强制中断长连接然后发起新的长连接(如果有需要)。
 *
 * @since 1.0
 */
public class IMSessionManager {

    private static final Singleton<IMSessionManager> INSTANCE = new Singleton<IMSessionManager>() {
        @Override
        protected IMSessionManager create() {
            return new IMSessionManager();
        }
    };

    @NonNull
    public static IMSessionManager getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private static final String KEY_SESSION_USER_ID_BY_TOKEN_PREFIX = "SessionUserIdByToken_20210306_";
    /**
     * 上一次成功同步完所有会话内容时的时间(服务器返回)(微秒)
     */
    private static final String KEY_CONVERSATION_LIST_LAST_SYNC_TIME_BY_SESSION_USER_ID_PREFIX = "conversationListLastSyncTimeBySessionUserId_20210408_";
    private static final LruCache<Long, Long> KEY_CONVERSATION_LIST_LAST_SYNC_TIME_BY_SESSION_USER_ID_CACHE = new LruCache<>(10);

    private final Object mSessionLock = new Object();
    @Nullable
    private Session mSession;
    private long mSessionUserId;

    @Nullable
    private SessionTcpClientProxyConfig mSessionTcpClientProxyConfig;
    @Nullable
    private SessionTcpClientProxy mSessionTcpClientProxy;

    private IMSessionManager() {
        Threads.postBackground(() -> IMManager.getInstance().start());
    }

    private static String createConversationListLastSyncTimeBySessionUserIdPrefix(final long sessionUserId) {
        return KEY_CONVERSATION_LIST_LAST_SYNC_TIME_BY_SESSION_USER_ID_PREFIX + sessionUserId;
    }

    public static void setConversationListLastSyncTimeBySessionUserId(long sessionUserId, long syncTime) {
        Preconditions.checkArgument(!Threads.isUi());
        synchronized (KEY_CONVERSATION_LIST_LAST_SYNC_TIME_BY_SESSION_USER_ID_CACHE) {
            {
                final Long cache = KEY_CONVERSATION_LIST_LAST_SYNC_TIME_BY_SESSION_USER_ID_CACHE.get(sessionUserId);
                if (cache != null) {
                    if (cache >= syncTime) {
                        // 忽略
                        return;
                    }
                }
            }

            final String key = createConversationListLastSyncTimeBySessionUserIdPrefix(sessionUserId);
            KeyValueStorage.set(key, String.valueOf(syncTime));
            KEY_CONVERSATION_LIST_LAST_SYNC_TIME_BY_SESSION_USER_ID_CACHE.put(sessionUserId, syncTime);
        }
    }

    public static long getConversationListLastSyncTimeBySessionUserId(long sessionUserId) {
        Preconditions.checkArgument(!Threads.isUi());
        synchronized (KEY_CONVERSATION_LIST_LAST_SYNC_TIME_BY_SESSION_USER_ID_CACHE) {
            {
                final Long cache = KEY_CONVERSATION_LIST_LAST_SYNC_TIME_BY_SESSION_USER_ID_CACHE.get(sessionUserId);
                if (cache != null) {
                    return cache;
                }
            }

            final String key = createConversationListLastSyncTimeBySessionUserIdPrefix(sessionUserId);
            final String value = KeyValueStorage.get(key);
            long syncTime = 0L;
            try {
                if (!TextUtils.isEmpty(value)) {
                    syncTime = Long.parseLong(value);
                }
            } catch (Throwable e) {
                IMLog.e(e);
            }
            KEY_CONVERSATION_LIST_LAST_SYNC_TIME_BY_SESSION_USER_ID_CACHE.put(sessionUserId, syncTime);
            return syncTime;
        }
    }

    /**
     * 设置当前的登录信息。设置为 null 表示退出登录.
     */
    public void setSession(@Nullable Session session) {
        boolean notifySessionChanged = false;
        synchronized (mSessionLock) {
            if (mSession != session) {
                notifySessionChanged = true;

                destroySessionTcpClient();

                mSession = session;
                // 重新设置 session 时，清除 session user id.
                mSessionUserId = 0;

                // 从历史记录中恢复可能存在的 token 与 session user id 的对应关系
                tryRestoreSessionUserIdFromDB(session);

                recreateSessionTcpClient(true);
            }
        }

        if (notifySessionChanged) {
            SessionObservable.DEFAULT.notifySessionChanged();
        }
    }

    /**
     * 尽可能获取一个可用的登录用户 id，如果长连接上的登录用户信息暂时暂时无效，则会先 block, 直到登录成功或者超时.
     */
    @WorkerThread
    @NonNull
    public GeneralResult getSessionUserIdWithBlockOrTimeout() {
        final long sessionUserId = mSessionUserId;
        if (sessionUserId > 0) {
            return GeneralResult.success();
        }

        final SingleSubject<GeneralResult> subject = SingleSubject.create();
        final Runnable validateSubjectState = () -> {
            // 检查当前 sessionUserId 是否已经正常了
            final long innerSessionUserId = mSessionUserId;
            if (innerSessionUserId > 0) {
                subject.onSuccess(GeneralResult.success());
            }
        };
        final Runnable subjectTimeout = () ->
                subject.onSuccess(GeneralResult.valueOf(GeneralResult.ERROR_CODE_TIMEOUT));
        final SessionObservable.SessionObserver sessionObserver = new SessionObservable.SessionObserver() {
            @Override
            public void onSessionChanged() {
                validateSubjectState.run();
            }

            @Override
            public void onSessionUserIdChanged() {
                validateSubjectState.run();
            }
        };
        final ClockObservable.ClockObserver clockObserver = new ClockObservable.ClockObserver() {

            // 超时时间
            private final long TIME_OUT = TimeUnit.SECONDS.toMillis(60);
            private final long mTimeStart = System.currentTimeMillis();

            @Override
            public void onClock() {
                if (System.currentTimeMillis() - mTimeStart > TIME_OUT) {
                    // 超时
                    IMLog.v(Objects.defaultObjectTag(this) + " getSessionUserIdWithBlockOrTimeout onClock timeout");
                    subjectTimeout.run();
                    ClockObservable.DEFAULT.unregisterObserver(this);
                } else {
                    validateSubjectState.run();
                }
            }
        };
        SessionObservable.DEFAULT.registerObserver(sessionObserver);
        ClockObservable.DEFAULT.registerObserver(clockObserver);

        final GeneralResult result = subject.blockingGet();
        IMLog.v("getSessionUserIdWithBlockOrTimeout GeneralResult:%s", result.toShortString());
        return result;
    }

    /**
     * 直接断开长连接, 清空 token
     */
    public void signOutImmediately() {
        setSession(null);
    }

    public boolean isSignOut() {
        return mSession == null;
    }

    /**
     * 尽可能从长连接上发送退出登录的消息，如果长连接暂时无效，则会先 block, 直到成功发送退出消息或者超时.
     */
    @WorkerThread
    @NonNull
    public GeneralResult signOutWithBlockOrTimeout() {
        return this.signOutWithBlockOrTimeout(true);
    }

    /**
     * 尽可能从长连接上发送退出登录的消息，如果长连接暂时无效，则会先 block, 直到成功发送退出消息或者超时.
     */
    @WorkerThread
    @NonNull
    public GeneralResult signOutWithBlockOrTimeout(boolean alwaysClearToken) {
        final Session session = mSession;
        if (session == null) {
            return GeneralResult.success();
        }
        session.setPendingSignOut();

        final SingleSubject<GeneralResult> subject = SingleSubject.create();
        final Runnable validateSubjectState = () -> {
            if (mSession == null) {
                subject.onSuccess(GeneralResult.success());
                return;
            }
            final SessionTcpClientProxy proxy = mSessionTcpClientProxy;
            if (proxy != null) {
                final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
                if (sessionTcpClient != null) {
                    final SignOutMessagePacket signOutMessagePacket = sessionTcpClient.getSignOutMessagePacket();
                    if (signOutMessagePacket.isSignOutSuccess()) {
                        setSession(null);
                        subject.onSuccess(GeneralResult.success());
                    } else if (signOutMessagePacket.isEnd()) {
                        subject.onSuccess(GeneralResult.valueOfOther(
                                GeneralResult.valueOf(
                                        signOutMessagePacket.getErrorCode(),
                                        signOutMessagePacket.getErrorMessage()
                                )
                        ));
                    }
                }
            }
        };
        final Runnable subjectTimeout = () ->
                subject.onSuccess(GeneralResult.valueOf(GeneralResult.ERROR_CODE_TIMEOUT));
        final SessionTcpClientObservable.SessionTcpClientObserver sessionTcpClientObserver = new SessionTcpClientObservable.SessionTcpClientObserver() {
            @Override
            public void onConnectionStateChanged(@NonNull SessionTcpClient sessionTcpClient) {
            }

            @Override
            public void onSignInStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignInMessagePacket messagePacket) {
            }

            @Override
            public void onSignOutStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignOutMessagePacket messagePacket) {
                validateSubjectState.run();
            }
        };

        final ClockObservable.ClockObserver clockObserver = new ClockObservable.ClockObserver() {

            // 超时时间
            private final long TIME_OUT = TimeUnit.SECONDS.toMillis(20);
            private final long mTimeStart = System.currentTimeMillis();

            @Override
            public void onClock() {
                if (System.currentTimeMillis() - mTimeStart > TIME_OUT) {
                    // 超时
                    IMLog.v(Objects.defaultObjectTag(this) + " signOutWithBlockOrTimeout onClock timeout");
                    subjectTimeout.run();
                    ClockObservable.DEFAULT.unregisterObserver(this);
                } else {
                    validateSubjectState.run();
                }
            }
        };
        SessionTcpClientObservable.DEFAULT.registerObserver(sessionTcpClientObserver);
        ClockObservable.DEFAULT.registerObserver(clockObserver);
        Threads.postBackground(() -> {
            if (mSession == null) {
                subject.onSuccess(GeneralResult.success());
                return;
            }
            final SessionTcpClientProxy proxy = getSessionTcpClientProxyWithBlockOrTimeout();
            if (subject.hasValue()) {
                return;
            }
            if (proxy == null) {
                subject.onSuccess(GeneralResult.valueOf(GeneralResult.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_IS_NULL));
                return;
            }
            if (!proxy.isOnline()) {
                subject.onSuccess(GeneralResult.valueOf(GeneralResult.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR));
                return;
            }
            final SessionTcpClient sessionTcpClient = proxy.getSessionTcpClient();
            if (sessionTcpClient == null) {
                subject.onSuccess(GeneralResult.valueOf(GeneralResult.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR));
                return;
            }
            if (!sessionTcpClient.isOnline()) {
                subject.onSuccess(GeneralResult.valueOf(GeneralResult.ERROR_CODE_SESSION_TCP_CLIENT_PROXY_CONNECTION_ERROR));
                return;
            }
            final SignOutMessagePacket signOutMessagePacket = sessionTcpClient.getSignOutMessagePacket();
            if (signOutMessagePacket.isIdle()) {
                sessionTcpClient.signOut();
            } else if (signOutMessagePacket.isSignOutSuccess()) {
                setSession(null);
                subject.onSuccess(GeneralResult.success());
            } else if (signOutMessagePacket.isEnd()) {
                subject.onSuccess(GeneralResult.valueOfOther(
                        GeneralResult.valueOf(
                                signOutMessagePacket.getErrorCode(),
                                signOutMessagePacket.getErrorMessage()
                        )
                ));
            }
        });
        validateSubjectState.run();

        final GeneralResult result = subject.blockingGet();
        IMLog.v("signOutWithBlockOrTimeout GeneralResult:%s", result.toShortString());

        if (alwaysClearToken) {
            setSession(null);
        }
        return result;
    }

    /**
     * 销毁长连接
     */
    public void destroySessionTcpClient() {
        IMLog.v(Objects.defaultObjectTag(this) + " destroySessionTcpClient");
        synchronized (mSessionLock) {
            if (mSessionTcpClientProxy != null) {
                mSessionTcpClientProxy.setAbort();
                mSessionTcpClientProxy = null;
            }
        }
    }

    /**
     * 终止旧的长连接，建立新的长连接(如果存在有效的登录信息)
     */
    public void recreateSessionTcpClient() {
        this.recreateSessionTcpClient(false);
    }

    /**
     * 终止旧的长连接，建立新的长连接(如果存在有效的登录信息)
     */
    private void recreateSessionTcpClient(boolean resetConfig) {
        IMLog.v(Objects.defaultObjectTag(this) + " recreateSessionTcpClient");
        synchronized (mSessionLock) {

            if (mSessionTcpClientProxy != null) {
                if (!mSessionTcpClientProxy.isAbort()) {
                    final SessionTcpClient sessionTcpClient = mSessionTcpClientProxy.getSessionTcpClient();
                    if (sessionTcpClient != null) {
                        final int sessionTcpClientState = sessionTcpClient.getState();
                        if (sessionTcpClientState == TcpClient.STATE_IDLE
                                || sessionTcpClientState == TcpClient.STATE_CONNECTING) {
                            // 正在请求建立长连接
                            IMLog.v(Objects.defaultObjectTag(this) + " recreateSessionTcpClient fast return. current tcp client is in connecting.");
                            return;
                        }

                        if (sessionTcpClientState == TcpClient.STATE_CONNECTED) {
                            if (!sessionTcpClient.getSignInMessagePacket().isEnd()
                                    && sessionTcpClient.getSignOutMessagePacket().isIdle()) {
                                // 正在登陆中
                                IMLog.v(Objects.defaultObjectTag(this) + " recreateSessionTcpClient fast return. current tcp client is in sign in ing.");
                                return;
                            }
                        }
                    }
                }
            }

            if (resetConfig) {
                mSessionTcpClientProxyConfig = null;
            }
            if (mSessionTcpClientProxy != null) {
                mSessionTcpClientProxy.setAbort();
                mSessionTcpClientProxy = null;
            }
            if (mSession != null) {
                if (mSessionTcpClientProxyConfig == null) {
                    mSessionTcpClientProxyConfig = new SessionTcpClientProxyConfig();
                } else {
                    mSessionTcpClientProxyConfig.mLastCreateTimeMs = System.currentTimeMillis();
                    mSessionTcpClientProxyConfig.mRetryCount++;
                }
                mSessionTcpClientProxy = new SessionTcpClientProxy(mSession);
                mSessionTcpClientProxy.connect();
            }
        }
    }

    /**
     * 将长连接的失败重连次数清零
     */
    public void clearSessionTcpClientProxyConfigRetryCount() {
        final SessionTcpClientProxyConfig sessionTcpClientProxyConfig = mSessionTcpClientProxyConfig;
        if (sessionTcpClientProxyConfig != null) {
            sessionTcpClientProxyConfig.mRetryCount = 0;
        }
    }

    @Nullable
    public Session getSession() {
        return mSession;
    }

    @Nullable
    public SessionTcpClientProxyConfig getSessionTcpClientProxyConfig() {
        return mSessionTcpClientProxyConfig;
    }

    @Nullable
    public SessionTcpClientProxy getSessionTcpClientProxy() {
        return mSessionTcpClientProxy;
    }

    /**
     * 尽可能获取一个可用的长连接，如果长连接暂时不可用，则会先 block, 直到长连接恢复或者超时.
     *
     * @return 即使返回结果不为空，也需要进一步校验返回结果中包含的具体长连接信息是否可用。
     */
    @WorkerThread
    @Nullable
    public SessionTcpClientProxy getSessionTcpClientProxyWithBlockOrTimeout() {
        final SessionTcpClientProxy proxy = mSessionTcpClientProxy;
        if (proxy != null && proxy.isOnline()) {
            return proxy;
        }

        final SingleSubject<GeneralResult> subject = SingleSubject.create();
        final Runnable validateSubjectState = () -> {
            // 检查当前 SessionTcpClientProxy 是否已经正常了
            final SessionTcpClientProxy innerProxy = mSessionTcpClientProxy;
            if (innerProxy != null && innerProxy.isOnline()) {
                subject.onSuccess(GeneralResult.success());
            }
        };
        final Runnable subjectTimeout = () ->
                subject.onSuccess(GeneralResult.valueOf(GeneralResult.ERROR_CODE_TIMEOUT));
        final SessionTcpClientObservable.SessionTcpClientObserver sessionTcpClientObserver = new SessionTcpClientObservable.SessionTcpClientObserver() {
            @Override
            public void onConnectionStateChanged(@NonNull SessionTcpClient sessionTcpClient) {
                validateSubjectState.run();
            }

            @Override
            public void onSignInStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignInMessagePacket messagePacket) {
                validateSubjectState.run();
            }

            @Override
            public void onSignOutStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignOutMessagePacket messagePacket) {
                validateSubjectState.run();
            }

        };
        final ClockObservable.ClockObserver clockObserver = new ClockObservable.ClockObserver() {

            // 超时时间
            private final long TIME_OUT = TimeUnit.SECONDS.toMillis(60);
            private final long mTimeStart = System.currentTimeMillis();

            @Override
            public void onClock() {
                if (System.currentTimeMillis() - mTimeStart > TIME_OUT) {
                    // 超时
                    IMLog.v(Objects.defaultObjectTag(this) + " getSessionTcpClientProxyWithBlockOrTimeout onClock timeout");
                    subjectTimeout.run();
                    ClockObservable.DEFAULT.unregisterObserver(this);
                } else {
                    validateSubjectState.run();
                }
            }
        };
        SessionTcpClientObservable.DEFAULT.registerObserver(sessionTcpClientObserver);
        ClockObservable.DEFAULT.registerObserver(clockObserver);

        final GeneralResult result = subject.blockingGet();
        IMLog.v("getSessionTcpClientProxyWithBlockOrTimeout GeneralResult:%s", result.toShortString());
        return mSessionTcpClientProxy;
    }

    /**
     * 获取当前 session 对应的用户 id<br/>
     * 此 session user id 可能是从历史缓存中获取的，也可能是从当前成功登录的长连接上获取的。<br/>
     * 即：能够获取到 session user id, 仅代表以当前 session 中的 token 信息至少(或曾经)成功登录过一次，并不能说明
     * 当前的长连接已经建立成功或者已经在当前长连接上登录成功。
     */
    public long getSessionUserId() {
        return mSessionUserId;
    }

    public void setSessionUserId(@Nullable final Session session, final long sessionUserId) {
        this.setSessionUserId(session, sessionUserId, true);
    }

    private void setSessionUserId(@Nullable final Session session, final long sessionUserId, final boolean acceptSave) {
        boolean trySave = false;
        boolean notifySessionUserIdChanged = false;
        synchronized (mSessionLock) {
            if (mSession == session) {
                if (mSessionUserId != sessionUserId) {
                    mSessionUserId = sessionUserId;
                    notifySessionUserIdChanged = true;
                    trySave = true;
                }
            } else {
                trySave = true;
            }
        }

        if (acceptSave && trySave) {
            trySaveSessionUserIdToDB(session, sessionUserId);
        }

        if (notifySessionUserIdChanged) {
            SessionObservable.DEFAULT.notifySessionUserIdChanged();
        }
    }

    private String createSessionUserIdKeyPrefix(final String token) {
        return KEY_SESSION_USER_ID_BY_TOKEN_PREFIX + token;
    }

    private void trySaveSessionUserIdToDB(@Nullable final Session session, final long sessionUserId) {
        if (session == null || sessionUserId <= 0) {
            return;
        }

        final String token = session.getToken();
        if (TextUtils.isEmpty(token)) {
            return;
        }

        Threads.postBackground(() -> {
            try {
                KeyValueStorage.set(createSessionUserIdKeyPrefix(token), String.valueOf(sessionUserId));
            } catch (Throwable e) {
                IMLog.e(e);
            }
        });
    }

    private void tryRestoreSessionUserIdFromDB(@Nullable final Session session) {
        if (session == null) {
            return;
        }

        final String token = session.getToken();
        if (TextUtils.isEmpty(token)) {
            return;
        }

        Threads.postBackground(() -> {
            try {
                final String sessionUserIdString = KeyValueStorage.get(createSessionUserIdKeyPrefix(token));
                if (TextUtils.isEmpty(sessionUserIdString)) {
                    return;
                }
                final long sessionUserId = Long.parseLong(sessionUserIdString);
                if (sessionUserId <= 0) {
                    throw new IllegalArgumentException("unexpected. parse invalid session user id " + sessionUserIdString + " -> " + sessionUserId);
                }
                setSessionUserId(session, sessionUserId, false/*不需要再存储到数据库*/);
            } catch (Throwable e) {
                IMLog.e(e);
            }
        });
    }

    public static class SessionTcpClientProxyConfig implements DebugManager.DebugInfoProvider {

        private final long mFirstCreateTimeMs = System.currentTimeMillis();
        private long mLastCreateTimeMs = mFirstCreateTimeMs;
        // 长连接失败的重连次数
        private int mRetryCount;

        private SessionTcpClientProxyConfig() {
            DebugManager.getInstance().addDebugInfoProvider(this);
        }

        public long getFirstCreateTimeMs() {
            return mFirstCreateTimeMs;
        }

        public long getLastCreateTimeMs() {
            return mLastCreateTimeMs;
        }

        public int getRetryCount() {
            return mRetryCount;
        }

        @Override
        public void fetchDebugInfo(@NonNull StringBuilder builder) {
            final String tag = Objects.defaultObjectTag(this);
            builder.append(tag).append("--:\n");
            builder.append(" mFirstCreateTimeMs:").append(this.mFirstCreateTimeMs).append("\n");
            builder.append(TimeUtil.msToHumanString(this.mFirstCreateTimeMs)).append("\n");
            builder.append(" mLastCreateTimeMs:").append(this.mLastCreateTimeMs).append("\n");
            builder.append(TimeUtil.msToHumanString(this.mLastCreateTimeMs)).append("\n");
            builder.append(" mRetryCount:").append(this.mRetryCount).append("\n");
            builder.append(tag).append("-- end\n");
        }

        @NonNull
        public String toShortString() {
            @SuppressWarnings("StringBufferReplaceableByString") final StringBuilder builder = new StringBuilder();
            builder.append(Objects.defaultObjectTag(this));
            builder.append(" mFirstCreateTimeMs:").append(this.mFirstCreateTimeMs);
            builder.append(" ").append(TimeUtil.msToHumanString(this.mFirstCreateTimeMs));
            builder.append(" mLastCreateTimeMs:").append(this.mLastCreateTimeMs);
            builder.append(" ").append(this.mLastCreateTimeMs);
            builder.append(" mRetryCount:").append(this.mRetryCount);
            return builder.toString();
        }

        @NonNull
        @Override
        public String toString() {
            return this.toShortString();
        }
    }

    public class SessionTcpClientProxy extends SimpleAbortSignal implements SessionTcpClientObservable.SessionTcpClientObserver {

        @Nullable
        private SessionTcpClient mSessionTcpClient;
        private FetchConversationListMessagePacket mFetchConversationListMessagePacket;
        @NonNull
        private final MessagePacketStateObservable.MessagePacketStateObserver mFetchConversationListMessagePacketStateObserver = new MessagePacketStateObservable.MessagePacketStateObserver() {
            @Override
            public void onStateChanged(MessagePacket packet, int oldState, int newState) {
                if (mFetchConversationListMessagePacket != packet) {
                    final Throwable e = new IllegalAccessError("invalid packet:" + Objects.defaultObjectTag(packet)
                            + ", mFetchConversationListMessagePacket:" + Objects.defaultObjectTag(mFetchConversationListMessagePacket));
                    IMLog.e(e);
                    return;
                }

                final FetchConversationListMessagePacket fetchConversationListMessagePacket = (FetchConversationListMessagePacket) packet;
                if (newState == MessagePacket.STATE_FAIL) {
                    // 发送失败
                    IMLog.v("onStateChanged STATE_FAIL fetchConversationListMessagePacket errorCode:%s, errorMessage:%s, timeout:%s",
                            fetchConversationListMessagePacket.getErrorCode(), fetchConversationListMessagePacket.getErrorMessage(), fetchConversationListMessagePacket.isTimeoutTriggered());
                    int errorCode = fetchConversationListMessagePacket.getErrorCode();
                    String errorMessage = fetchConversationListMessagePacket.getErrorMessage();
                    if (errorCode == 0) {
                        if (fetchConversationListMessagePacket.isTimeoutTriggered()) {
                            errorCode = GeneralErrorCode.ERROR_CODE_MESSAGE_PACKET_SEND_TIMEOUT;
                            errorMessage = GeneralErrorCode.findDefaultErrorMessage(errorCode);
                        }
                    }
                    if (errorCode == 0) {
                        errorCode = GeneralErrorCode.ERROR_CODE_UNKNOWN;
                        errorMessage = GeneralErrorCode.findDefaultErrorMessage(errorCode);
                    }

                    FetchConversationListObservable.DEFAULT.notifyConversationListFetchedError(errorCode, errorMessage);
                } else if (newState == MessagePacket.STATE_SUCCESS) {
                    // 发送成功
                    if (fetchConversationListMessagePacket.isEmptyConversationList()) {
                        // 会话列表没有变动或者为空
                        FetchConversationListObservable.DEFAULT.notifyConversationListFetchedSuccess();
                    }
                } else if (newState == MessagePacket.STATE_GOING) {
                    FetchConversationListObservable.DEFAULT.notifyConversationListFetchedLoading();
                }
            }
        };

        private SessionTcpClientProxy(@NonNull Session session) {
            IMLog.v(Objects.defaultObjectTag(this) + " SessionTcpClientProxy init");
            SessionTcpClientObservable.DEFAULT.registerObserver(this);

            mSessionTcpClient = new SessionTcpClient(session);
            mSessionTcpClient.getLocalMessageProcessor().addLastProcessor(target -> {
                // 处理获取会话列表的响应结果
                final FetchConversationListMessagePacket fetchConversationListMessagePacket = mFetchConversationListMessagePacket;
                if (fetchConversationListMessagePacket != null) {
                    return fetchConversationListMessagePacket.doProcess(target);
                }
                return false;
            });
        }

        private void connect() {
            Preconditions.checkNotNull(mSessionTcpClient);
            IMLog.v(Objects.defaultObjectTag(this) + " before connect. SessionTcpClient state:%s",
                    SessionTcpClient.stateToString(mSessionTcpClient.getState()));
            mSessionTcpClient.connect();
            IMLog.v(Objects.defaultObjectTag(this) + " after connect. SessionTcpClient state:%s",
                    SessionTcpClient.stateToString(mSessionTcpClient.getState()));
        }

        @Override
        public void setAbort() {
            super.setAbort();

            IOUtil.closeQuietly(mSessionTcpClient);
            mSessionTcpClient = null;
        }

        /**
         * 判断当前长连接是否在线。当长连接不在线时，不能够发送消息。长连接在线的含义是：长连接建立成功并且认证通过。
         * 认证消息(登录消息)在长连接内部自动维护(长连接首次建立成功时会自动发送登录消息)。
         */
        public boolean isOnline() {
            SessionTcpClient sessionTcpClient = mSessionTcpClient;
            return !isAbort() && sessionTcpClient != null && sessionTcpClient.isOnline();
        }

        @Nullable
        public SessionTcpClient getSessionTcpClient() {
            return mSessionTcpClient;
        }

        @Override
        public boolean isAbort() {
            return super.isAbort() || IMSessionManager.this.mSessionTcpClientProxy != this;
        }

        @Override
        public void onConnectionStateChanged(@NonNull SessionTcpClient sessionTcpClient) {
            if (AbortUtil.isAbort(this)) {
                setAbort();
                IMLog.v("ignore onConnectionStateChanged. SessionTcpClientProxy is abort.");
                return;
            }

            if (sessionTcpClient != mSessionTcpClient) {
                setAbort();
                IMLog.v("ignore onConnectionStateChanged sessionTcpClient is another one");
                return;
            }

            // TODO
        }

        @Override
        public void onSignInStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignInMessagePacket messagePacket) {
            if (AbortUtil.isAbort(this)) {
                setAbort();
                IMLog.v("ignore onSignInStateChanged. SessionTcpClientProxy is abort.");
                return;
            }

            if (sessionTcpClient != mSessionTcpClient) {
                setAbort();
                IMLog.v("ignore onSignInStateChanged sessionTcpClient is another one");
                return;
            }

            if (sessionTcpClient.isOnline()) {
                // 登录成功，清空长连接失败重连次数
                clearSessionTcpClientProxyConfigRetryCount();

                final Session session = sessionTcpClient.getSession();
                final long sessionUserId = messagePacket.getSessionUserId();
                if (sessionUserId > 0) {
                    // 长连接上有合法的用户 id 时，同步到本地存储
                    setSessionUserId(session, sessionUserId);

                    if (session.isPendingSignOut()) {
                        // 退出登录
                        sessionTcpClient.signOut();
                    } else {
                        // 获取当前登录用户的 profile
                        UserInfoSyncManager.getInstance().enqueueSyncUserInfo(sessionUserId);
                        // 读取会话列表
                        mFetchConversationListMessagePacket = FetchConversationListMessagePacket.create(getConversationListLastSyncTimeBySessionUserId(sessionUserId));
                        mFetchConversationListMessagePacket.getMessagePacketStateObservable().registerObserver(mFetchConversationListMessagePacketStateObserver);
                        sessionTcpClient.sendMessagePacketQuietly(mFetchConversationListMessagePacket);
                    }
                }
            }
        }

        @Override
        public void onSignOutStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignOutMessagePacket messagePacket) {
            if (AbortUtil.isAbort(this)) {
                setAbort();
                IMLog.v("ignore onSignOutStateChanged. SessionTcpClientProxy is abort.");
                return;
            }

            if (sessionTcpClient != mSessionTcpClient) {
                setAbort();
                IMLog.v("ignore onSignOutStateChanged sessionTcpClient is another one");
                return;
            }

            if (messagePacket.isSignOutSuccess()) {
                // 成功退出登录，关闭长连接
                setSession(null);
                return;
            }

            if (messagePacket.isFail()) {
                final int errorCode = messagePacket.getErrorCode();
                final String errorMessage = messagePacket.getErrorMessage();
                IMLog.v("onSignOutStateChanged messagePacket.isFail errorCode:%s errorMessage:%s", errorCode, errorMessage);
                if (errorCode != 0) {
                    // 退出登录失败，服务器返回了某种异常信息
                    // 关闭长连接
                    setSession(null);
                }
            }
        }
    }

}
