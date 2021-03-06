package com.masonsoft.imsdk;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.SimpleAbortSignal;
import com.idonans.core.Singleton;
import com.idonans.core.thread.Threads;
import com.idonans.core.util.AbortUtil;
import com.masonsoft.imsdk.core.KeyValueStorage;
import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.util.WeakObservable;

/**
 * 处理登录状态相关内容，当登录状态发生变更时，将强制断开长连接然后发起新的长连接(如果有登录信息)。
 * 并且在读取任意数据时，都将校验当前长连接上通过认证的登陆信息是否与最新的登录信息一致，如果不一致，将强制中断长连接然后发起新的长连接(如果有需要)。
 *
 * @since 1.0
 */
public class MSIMSessionManager {

    private static final Singleton<MSIMSessionManager> INSTANCE = new Singleton<MSIMSessionManager>() {
        @Override
        protected MSIMSessionManager create() {
            return new MSIMSessionManager();
        }
    };

    /**
     * 获取 MSIMSessionManager 单例
     *
     * @see MSIMManager#getSessionManager()
     */
    @NonNull
    static MSIMSessionManager getInstance() {
        return INSTANCE.get();
    }

    private static final String KEY_SESSION_USER_ID_BY_TOKEN_PREFIX = "SessionUserIdByToken_20210306_";

    private final SessionObservable mSessionObservable = new SessionObservable();

    @Nullable
    private Session mSession;
    private long mSessionUserId;

    @Nullable
    private SessionTcpClientProxy mSessionTcpClientProxy;

    private MSIMSessionManager() {
    }

    /**
     * 设置当前的登录信息。设置为 null 表示退出登录.
     */
    public void setSession(@Nullable Session session) {
        if (mSession != session) {
            if (mSessionTcpClientProxy != null) {
                mSessionTcpClientProxy.setAbort();
                mSessionTcpClientProxy = null;
            }

            mSession = session;
            // 重新设置 session 时，清除 session user id.
            mSessionUserId = 0;

            // 从历史记录中恢复可能存在的 token 与 session user id 的对应关系
            tryRestoreSessionUserIdFromDB(session);

            mSessionObservable.notifySessionChanged();

            if (mSession != null) {
                mSessionTcpClientProxy = new SessionTcpClientProxy(new SessionTcpClient(mSession));
            }
        }
    }

    @Nullable
    public Session getSession() {
        return mSession;
    }

    public void setSessionUserId(@Nullable final Session session, final long sessionUserId) {
        this.setSessionUserId(session, sessionUserId, true);
    }

    private void setSessionUserId(@Nullable final Session session, final long sessionUserId, final boolean acceptSave) {
        boolean trySave = false;
        if (mSession == session) {
            if (mSessionUserId != sessionUserId) {
                mSessionUserId = sessionUserId;
                mSessionObservable.notifySessionUserIdChanged();
                trySave = true;
            }
        } else {
            trySave = true;
        }

        if (acceptSave && trySave) {
            trySaveSessionUserIdToDB(session, sessionUserId);
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

    private class SessionTcpClientProxy extends SimpleAbortSignal implements SessionTcpClient.Observer {

        @SuppressWarnings("FieldCanBeLocal")
        @NonNull
        private final SessionTcpClient mSessionTcpClient;

        private SessionTcpClientProxy(@NonNull SessionTcpClient sessionTcpClient) {
            mSessionTcpClient = sessionTcpClient;
            mSessionTcpClient.getObservable().registerObserver(this);
        }

        @Override
        public boolean isAbort() {
            return super.isAbort() || MSIMSessionManager.this.mSessionTcpClientProxy != this;
        }

        @Override
        public void onConnectionStateChanged() {
            if (AbortUtil.isAbort(this)) {
                IMLog.v("ignore onConnectionStateChanged. SessionTcpClientProxy is abort.");
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

            if (mSessionTcpClient.isOnline()) {
                final Session session = mSessionTcpClient.getSession();
                final long sessionUserId = mSessionTcpClient.getSessionUserId();
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

            // TODO
        }
    }

}
