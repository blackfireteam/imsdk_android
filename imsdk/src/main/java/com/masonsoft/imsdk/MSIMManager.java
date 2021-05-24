package com.masonsoft.imsdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignInMessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignOutMessagePacket;
import com.masonsoft.imsdk.core.observable.SessionTcpClientObservable;
import com.masonsoft.imsdk.core.observable.TokenOfflineObservable;
import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.lang.GeneralResult;

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

    private String mAppId;
    @NonNull
    private MSIMSdkListener mSdkListener = new MSIMSdkListenerAdapter();
    @SuppressWarnings("FieldCanBeLocal")
    private final TokenOfflineObservable.TokenOfflineObserver mTokenOfflineObserver = new TokenOfflineObservable.TokenOfflineObserver() {
        @Override
        public void onKickedOffline(@NonNull Session session, int errorCode, String errorMessage) {
            mSdkListener.onKickedOffline();
        }

        @Override
        public void onTokenExpired(@NonNull Session session, int errorCode, String errorMessage) {
            mSdkListener.onTokenExpired();
        }
    };
    @SuppressWarnings("FieldCanBeLocal")
    private final SessionTcpClientObservable.SessionTcpClientObserver mSessionTcpClientObserver = new SessionTcpClientObservable.SessionTcpClientObserver() {
        @Override
        public void onConnectionStateChanged(@NonNull SessionTcpClient sessionTcpClient) {
            final int state = sessionTcpClient.getState();
            if (state == SessionTcpClient.STATE_CONNECTING) {
                mSdkListener.onConnecting();
            } else if (state == SessionTcpClient.STATE_CONNECTED) {
                mSdkListener.onConnectSuccess();
            } else if (state == SessionTcpClient.STATE_CLOSED) {
                mSdkListener.onConnectClosed();
            }
        }

        @Override
        public void onSignInStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignInMessagePacket messagePacket) {
            final int state = messagePacket.getState();
            if (state == MessagePacket.STATE_GOING) {
                mSdkListener.onSigningIn();
            } else if (state == MessagePacket.STATE_SUCCESS) {
                mSdkListener.onSignInSuccess();
            } else if (state == MessagePacket.STATE_FAIL) {
                mSdkListener.onSignInFail(messagePacket.getErrorCode(), messagePacket.getErrorMessage());
            }
        }

        @Override
        public void onSignOutStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignOutMessagePacket messagePacket) {
            final int state = messagePacket.getState();
            if (state == MessagePacket.STATE_GOING) {
                mSdkListener.onSigningOut();
            } else if (state == MessagePacket.STATE_SUCCESS) {
                mSdkListener.onSignOutSuccess();
            } else if (state == MessagePacket.STATE_FAIL) {
                mSdkListener.onSignOutFail(messagePacket.getErrorCode(), messagePacket.getErrorMessage());
            }
        }
    };

    private Object mSignInOrSignOutTag;

    private MSIMManager() {
        TokenOfflineObservable.DEFAULT.registerObserver(mTokenOfflineObserver);
        SessionTcpClientObservable.DEFAULT.registerObserver(mSessionTcpClientObserver);
    }

    public void initSdk(String appId, @Nullable MSIMSdkListener listener) {
        mAppId = appId;
        mSdkListener = new MSIMSdkListenerProxy(listener);
    }

    public void signIn(String token, String tcpServerAndPort, MSIMCallback callback) {
        final Object signInOrSignOutTag = resetSignInOrSignOutTag();
        final MSIMCallback proxy = new MSIMCallbackProxy(callback);
        final Session session = Session.create(token, tcpServerAndPort);
        IMSessionManager.getInstance().setSession(session);
        Threads.postBackground(() -> {
            if (isSignInOrSignOutTagChanged(signInOrSignOutTag)) {
                return;
            }
            final GeneralResult result = IMSessionManager.getInstance().getSessionUserIdWithBlockOrTimeout();
            final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
            if (isSignInOrSignOutTagChanged(signInOrSignOutTag)) {
                return;
            }
            if (sessionUserId > 0) {
                proxy.onSuccess();
            } else {
                if (result.other != null) {
                    proxy.onError(result.other.code, result.other.message);
                } else {
                    proxy.onError(result.code, result.message);
                }
            }
        });
    }

    @WorkerThread
    @NonNull
    public GeneralResult signInWithBlock(String token, String tcpServerAndPort) {
        resetSignInOrSignOutTag();
        final Session session = Session.create(token, tcpServerAndPort);
        IMSessionManager.getInstance().setSession(session);
        final GeneralResult result = IMSessionManager.getInstance().getSessionUserIdWithBlockOrTimeout();
        if (result.isSuccess()) {
            return result;
        }
        if (result.other != null) {
            return result.other;
        }
        return result;
    }

    public void signOut(MSIMCallback callback) {
        final Object signInOrSignOutTag = resetSignInOrSignOutTag();
        final MSIMCallback proxy = new MSIMCallbackProxy(callback);
        Threads.postBackground(() -> {
            if (isSignInOrSignOutTagChanged(signInOrSignOutTag)) {
                return;
            }
            final GeneralResult result = IMSessionManager.getInstance().signOutWithBlockOrTimeout();
            if (isSignInOrSignOutTagChanged(signInOrSignOutTag)) {
                return;
            }
            if (result.isSuccess()) {
                proxy.onSuccess();
            } else {
                if (result.other != null) {
                    proxy.onError(result.other.code, result.other.message);
                } else {
                    proxy.onError(result.code, result.message);
                }
            }
        });
    }

    @WorkerThread
    @NonNull
    public GeneralResult signOutWithBlock() {
        resetSignInOrSignOutTag();
        final GeneralResult result = IMSessionManager.getInstance().signOutWithBlockOrTimeout();
        if (result.isSuccess()) {
            return result;
        }
        if (result.other != null) {
            return result.other;
        }
        return result;
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

    @NonNull
    public MSIMMessageManager getMessageManager() {
        return MSIMMessageManager.getInstance();
    }

    @NonNull
    public MSIMConversationManager getConversationManager() {
        return MSIMConversationManager.getInstance();
    }

    public final String getAppId() {
        return mAppId;
    }

}
