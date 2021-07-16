package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.masonsoft.imsdk.core.IMManager;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignInMessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignOutMessagePacket;
import com.masonsoft.imsdk.core.observable.SessionObservable;
import com.masonsoft.imsdk.core.observable.SessionTcpClientObservable;
import com.masonsoft.imsdk.core.observable.TokenOfflineObservable;
import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.util.RxJavaUtil;
import com.masonsoft.imsdk.util.WeakObservable;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.Threads;

/**
 * @since 1.0
 */
public class MSIMManager {

    private static final Singleton<MSIMManager> INSTANCE = new Singleton<MSIMManager>() {
        @Override
        protected MSIMManager create() {
            return new MSIMManager();
        }
    };

    @NonNull
    public static MSIMManager getInstance() {
        return INSTANCE.get();
    }

    private final AtomicBoolean mInit = new AtomicBoolean(false);
    private int mSubApp;

    @NonNull
    private final WeakObservable<MSIMSdkListener> mSdkListeners = new WeakObservable<>();
    @SuppressWarnings("FieldCanBeLocal")
    private final TokenOfflineObservable.TokenOfflineObserver mTokenOfflineObserver = new TokenOfflineObservable.TokenOfflineObserver() {
        @Override
        public void onKickedOffline(@NonNull Session session, int errorCode, String errorMessage) {
            mSdkListeners.forEach(listener -> {
                if (listener != null) {
                    listener.onKickedOffline();
                }
            });
        }

        @Override
        public void onTokenExpired(@NonNull Session session, int errorCode, String errorMessage) {
            mSdkListeners.forEach(listener -> {
                if (listener != null) {
                    listener.onTokenExpired();
                }
            });
        }
    };
    @SuppressWarnings("FieldCanBeLocal")
    private final SessionTcpClientObservable.SessionTcpClientObserver mSessionTcpClientObserver = new SessionTcpClientObservable.SessionTcpClientObserver() {
        @Override
        public void onConnectionStateChanged(@NonNull SessionTcpClient sessionTcpClient) {
            final int state = sessionTcpClient.getState();
            if (state == SessionTcpClient.STATE_CONNECTING) {
                mSdkListeners.forEach(listener -> {
                    if (listener != null) {
                        listener.onConnecting();
                    }
                });
            } else if (state == SessionTcpClient.STATE_CONNECTED) {
                mSdkListeners.forEach(listener -> {
                    if (listener != null) {
                        listener.onConnectSuccess();
                    }
                });
            } else if (state == SessionTcpClient.STATE_CLOSED) {
                mSdkListeners.forEach(listener -> {
                    if (listener != null) {
                        listener.onConnectClosed();
                    }
                });
            }
        }

        @Override
        public void onSignInStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignInMessagePacket messagePacket) {
            final int state = messagePacket.getState();
            if (state == MessagePacket.STATE_GOING) {
                mSdkListeners.forEach(listener -> {
                    if (listener != null) {
                        listener.onSigningIn();
                    }
                });
            } else if (state == MessagePacket.STATE_SUCCESS) {
                mSdkListeners.forEach(listener -> {
                    if (listener != null) {
                        listener.onSignInSuccess();
                    }
                });
            } else if (state == MessagePacket.STATE_FAIL) {
                mSdkListeners.forEach(listener -> {
                    if (listener != null) {
                        listener.onSignInFail(GeneralResult.valueOf(messagePacket.getErrorCode(), messagePacket.getErrorMessage()));
                    }
                });
            }
        }

        @Override
        public void onSignOutStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignOutMessagePacket messagePacket) {
            final int state = messagePacket.getState();
            if (state == MessagePacket.STATE_GOING) {
                mSdkListeners.forEach(listener -> {
                    if (listener != null) {
                        listener.onSigningOut();
                    }
                });
            } else if (state == MessagePacket.STATE_SUCCESS) {
                mSdkListeners.forEach(listener -> {
                    if (listener != null) {
                        listener.onSignOutSuccess();
                    }
                });
            } else if (state == MessagePacket.STATE_FAIL) {
                mSdkListeners.forEach(listener -> {
                    if (listener != null) {
                        listener.onSignOutFail(GeneralResult.valueOf(messagePacket.getErrorCode(), messagePacket.getErrorMessage()));
                    }
                });
            }
        }
    };

    private Object mSignInOrSignOutTag;

    @NonNull
    private final WeakObservable<MSIMSessionListener> mSessionListeners = new WeakObservable<>();

    @SuppressWarnings("FieldCanBeLocal")
    private final SessionObservable.SessionObserver mSessionObserver = new SessionObservable.SessionObserver() {
        @Override
        public void onSessionChanged() {
            mSessionListeners.forEach(listener -> {
                if (listener != null) {
                    listener.onSessionChanged();
                }
            });
        }

        @Override
        public void onSessionUserIdChanged() {
            mSessionListeners.forEach(listener -> {
                if (listener != null) {
                    listener.onSessionUserIdChanged();
                }
            });
        }
    };

    private MSIMManager() {
        RxJavaUtil.setErrorHandler();

        TokenOfflineObservable.DEFAULT.registerObserver(mTokenOfflineObserver);
        SessionTcpClientObservable.DEFAULT.registerObserver(mSessionTcpClientObserver);
        SessionObservable.DEFAULT.registerObserver(mSessionObserver);
    }

    public void initSdk(int subApp, @Nullable MSIMSdkListener listener) {
        mSubApp = subApp;
        mInit.set(true);

        addSdkListener(listener);

        Threads.postBackground(() -> IMManager.getInstance().start());
    }

    private void requireInit() {
        if (!mInit.get()) {
            throw new IllegalStateException("not init. see #initSdk");
        }
    }

    public void addSdkListener(@Nullable MSIMSdkListener listener) {
        if (listener != null) {
            mSdkListeners.registerObserver(listener);
        }
    }

    public void removeSdkListener(@Nullable MSIMSdkListener listener) {
        if (listener != null) {
            mSdkListeners.unregisterObserver(listener);
        }
    }

    public void addSessionListener(@Nullable MSIMSessionListener listener) {
        if (listener != null) {
            mSessionListeners.registerObserver(listener);
        }
    }

    public void removeSessionListener(@Nullable MSIMSessionListener listener) {
        if (listener != null) {
            mSessionListeners.unregisterObserver(listener);
        }
    }

    public void signIn(@NonNull String token, @NonNull String tcpServerAndPort, @Nullable MSIMCallback<GeneralResult> callback) {
        requireInit();

        final Object signInOrSignOutTag = resetSignInOrSignOutTag();
        final MSIMCallback<GeneralResult> proxy = new MSIMCallbackProxy<>(callback);
        final Session session = Session.create(token, tcpServerAndPort);
        IMSessionManager.getInstance().setSession(session);
        Threads.postBackground(() -> {
            if (isSignInOrSignOutTagChanged(signInOrSignOutTag)) {
                return;
            }
            final GeneralResult result = IMSessionManager.getInstance().getSessionUserIdWithBlockOrTimeout();
            if (isSignInOrSignOutTagChanged(signInOrSignOutTag)) {
                return;
            }
            proxy.onCallback(result.getCause());
        });
    }

    @WorkerThread
    @NonNull
    public GeneralResult signInWithBlock(@NonNull String token, @NonNull String tcpServerAndPort) {
        requireInit();

        resetSignInOrSignOutTag();
        final Session session = Session.create(token, tcpServerAndPort);
        IMSessionManager.getInstance().setSession(session);
        final GeneralResult result = IMSessionManager.getInstance().getSessionUserIdWithBlockOrTimeout();
        return result.getCause();
    }

    public void signOut(@Nullable MSIMCallback<GeneralResult> callback) {
        requireInit();

        final Object signInOrSignOutTag = resetSignInOrSignOutTag();
        final MSIMCallback<GeneralResult> proxy = new MSIMCallbackProxy<>(callback);
        Threads.postBackground(() -> {
            if (isSignInOrSignOutTagChanged(signInOrSignOutTag)) {
                return;
            }
            final GeneralResult result = IMSessionManager.getInstance().signOutWithBlockOrTimeout();
            if (isSignInOrSignOutTagChanged(signInOrSignOutTag)) {
                return;
            }
            proxy.onCallback(result.getCause());
        });
    }

    @WorkerThread
    @NonNull
    public GeneralResult signOutWithBlock() {
        requireInit();

        resetSignInOrSignOutTag();
        final GeneralResult result = IMSessionManager.getInstance().signOutWithBlockOrTimeout();
        return result.getCause();
    }

    @NonNull
    private Object resetSignInOrSignOutTag() {
        final Object tag = new Object();
        mSignInOrSignOutTag = tag;
        return tag;
    }

    private boolean isSignInOrSignOutTagChanged(Object signInOrSignOutTag) {
        return signInOrSignOutTag == null || mSignInOrSignOutTag != signInOrSignOutTag;
    }

    public long getSessionUserId() {
        requireInit();

        return IMSessionManager.getInstance().getSessionUserId();
    }

    public boolean hasSession() {
        requireInit();

        return IMSessionManager.getInstance().getSession() != null;
    }

    public void setSession(@NonNull String token, @NonNull String tcpServerAndPort) {
        requireInit();

        resetSignInOrSignOutTag();
        final Session session = Session.create(token, tcpServerAndPort);
        IMSessionManager.getInstance().setSession(session);
    }

    @NonNull
    public MSIMMessageManager getMessageManager() {
        requireInit();

        return MSIMMessageManager.getInstance();
    }

    @NonNull
    public MSIMConversationManager getConversationManager() {
        requireInit();

        return MSIMConversationManager.getInstance();
    }

    @NonNull
    public MSIMUserInfoManager getUserInfoManager() {
        requireInit();

        return MSIMUserInfoManager.getInstance();
    }

    public final int getSubApp() {
        requireInit();

        return mSubApp;
    }

}
