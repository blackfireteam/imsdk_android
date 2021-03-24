package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.util.WeakObservable;

public class SessionTcpClientObservable extends WeakObservable<SessionTcpClientObservable.SessionTcpClientObserver> {

    public static final SessionTcpClientObservable DEFAULT = new SessionTcpClientObservable();

    public interface SessionTcpClientObserver {
        void onConnectionStateChanged();

        void onSignInStateChanged();

        void onSignOutStateChanged();
    }

    public void notifyConnectionStateChanged() {
        forEach(SessionTcpClientObserver::onConnectionStateChanged);
    }

    public void notifySignInStateChanged() {
        forEach(SessionTcpClientObserver::onSignInStateChanged);
    }

    public void notifySignOutStateChanged() {
        forEach(SessionTcpClientObserver::onSignOutStateChanged);
    }

}
