package com.masonsoft.imsdk.core;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.SimpleAbortSignal;
import com.idonans.core.Singleton;
import com.idonans.core.thread.Threads;
import com.idonans.core.util.AbortUtil;
import com.idonans.core.util.IOUtil;
import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.util.WeakObservable;

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

    private final SessionObservable mSessionObservable = new SessionObservable();

    private final Object mSessionLock = new Object();
    @Nullable
    private Session mSession;
    private long mSessionUserId;

    @Nullable
    private SessionTcpClientProxy mSessionTcpClientProxy;

    private IMSessionManager() {
    }

    /**
     * 设置当前的登录信息。设置为 null 表示退出登录.
     */
    public void setSession(@Nullable Session session) {
        boolean notifySessionChanged = false;
        synchronized (mSessionLock) {
            if (mSession != session) {
                notifySessionChanged = true;

                if (mSessionTcpClientProxy != null) {
                    mSessionTcpClientProxy.setAbort();
                    mSessionTcpClientProxy = null;
                }

                mSession = session;
                // 重新设置 session 时，清除 session user id.
                mSessionUserId = 0;

                // 从历史记录中恢复可能存在的 token 与 session user id 的对应关系
                tryRestoreSessionUserIdFromDB(session);

                if (mSession != null) {
                    mSessionTcpClientProxy = new SessionTcpClientProxy(mSession);
                }
            }
        }

        if (notifySessionChanged) {
            mSessionObservable.notifySessionChanged();
        }
    }

    /**
     * 销毁长连接
     */
    public void destroySessionTcpClient() {
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
        synchronized (mSessionLock) {
            if (mSessionTcpClientProxy != null) {
                mSessionTcpClientProxy.setAbort();
                mSessionTcpClientProxy = null;
            }
            if (mSession != null) {
                mSessionTcpClientProxy = new SessionTcpClientProxy(mSession);
            }
        }
    }

    @Nullable
    public Session getSession() {
        return mSession;
    }

    @Nullable
    public SessionTcpClientProxy getSessionTcpClientProxy() {
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
            mSessionObservable.notifySessionUserIdChanged();
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

    @NonNull
    public SessionObservable getSessionObservable() {
        return mSessionObservable;
    }

    public interface SessionObserver {
        void onSessionChanged();

        void onSessionUserIdChanged();
    }

    public static class SessionObservable extends WeakObservable<SessionObserver> {
        public void notifySessionChanged() {
            forEach(SessionObserver::onSessionChanged);
        }

        public void notifySessionUserIdChanged() {
            forEach(SessionObserver::onSessionUserIdChanged);
        }
    }

    public class SessionTcpClientProxy extends SimpleAbortSignal implements SessionTcpClient.Observer {

        @Nullable
        private SessionTcpClient mSessionTcpClient;

        private SessionTcpClientProxy(@NonNull Session session) {
            mSessionTcpClient = new SessionTcpClient(session);
            mSessionTcpClient.getObservable().registerObserver(this);
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

        @Override
        public boolean isAbort() {
            return super.isAbort() || IMSessionManager.this.mSessionTcpClientProxy != this;
        }

        @Override
        public void onConnectionStateChanged() {
            if (AbortUtil.isAbort(this)) {
                IMLog.v("ignore onConnectionStateChanged. SessionTcpClientProxy is abort.");
                return;
            }

            SessionTcpClient sessionTcpClient = mSessionTcpClient;
            if (sessionTcpClient == null) {
                return;
            }

            // TODO
        }

        @Override
        public void onSignInStateChanged() {
            if (AbortUtil.isAbort(this)) {
                IMLog.v("ignore onSignInStateChanged. SessionTcpClientProxy is abort.");
                return;
            }

            SessionTcpClient sessionTcpClient = mSessionTcpClient;
            if (sessionTcpClient == null) {
                return;
            }

            if (sessionTcpClient.isOnline()) {
                final Session session = sessionTcpClient.getSession();
                final long sessionUserId = sessionTcpClient.getSessionUserId();
                if (sessionUserId > 0) {
                    // 长连接上有合法的用户 id 时，同步到本地存储
                    setSessionUserId(session, sessionUserId);
                }
            }
        }

        @Override
        public void onSignOutStateChanged() {
            if (AbortUtil.isAbort(this)) {
                IMLog.v("ignore onSignOutStateChanged. SessionTcpClientProxy is abort.");
                return;
            }

            SessionTcpClient sessionTcpClient = mSessionTcpClient;
            if (sessionTcpClient == null) {
                return;
            }

            // TODO
        }
    }

}
