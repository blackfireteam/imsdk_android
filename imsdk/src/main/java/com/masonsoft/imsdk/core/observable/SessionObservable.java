package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.util.WeakObservable;

/**
 * Session 信息的变更
 *
 * @see com.masonsoft.imsdk.core.IMSessionManager
 */
public class SessionObservable extends WeakObservable<SessionObservable.SessionObserver> {

    public static final SessionObservable DEFAULT = new SessionObservable();

    public interface SessionObserver {
        void onSessionChanged();

        void onSessionUserIdChanged();
    }

    public void notifySessionChanged() {
        forEach(SessionObserver::onSessionChanged);
    }

    public void notifySessionUserIdChanged() {
        forEach(SessionObserver::onSessionUserIdChanged);
    }

}