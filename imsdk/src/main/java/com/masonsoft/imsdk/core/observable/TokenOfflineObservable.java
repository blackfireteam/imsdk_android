package com.masonsoft.imsdk.core.observable;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.util.WeakObservable;

/**
 * 登录失效状态通知
 */
public class TokenOfflineObservable extends WeakObservable<TokenOfflineObservable.TokenOfflineObserver> {

    public static final TokenOfflineObservable DEFAULT = new TokenOfflineObservable();

    public interface TokenOfflineObserver {
        void onKickedOffline(@NonNull final Session session, int errorCode, String errorMessage);

        void onTokenExpired(@NonNull final Session session, int errorCode, String errorMessage);
    }

    public void notifyKickedOffline(@NonNull final Session session, int errorCode, String errorMessage) {
        forEach(tokenOfflineObserver -> tokenOfflineObserver.onKickedOffline(session, errorCode, errorMessage));
    }

    public void notifyTokenExpired(@NonNull final Session session, int errorCode, String errorMessage) {
        forEach(tokenOfflineObserver -> tokenOfflineObserver.onTokenExpired(session, errorCode, errorMessage));
    }

}
