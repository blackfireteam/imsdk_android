package com.masonsoft.imsdk.core.observable;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.util.WeakObservable;

public class SessionTcpClientObservable extends WeakObservable<SessionTcpClientObservable.SessionTcpClientObserver> {

    public static final SessionTcpClientObservable DEFAULT = new SessionTcpClientObservable();

    public interface SessionTcpClientObserver {
        void onConnectionStateChanged(@NonNull SessionTcpClient sessionTcpClient);

        void onSignInStateChanged(@NonNull SessionTcpClient sessionTcpClient);

        void onSignOutStateChanged(@NonNull SessionTcpClient sessionTcpClient);
    }

    public void notifyConnectionStateChanged(@NonNull SessionTcpClient sessionTcpClient) {
        forEach(sessionTcpClientObserver -> sessionTcpClientObserver.onConnectionStateChanged(sessionTcpClient));
    }

    public void notifySignInStateChanged(@NonNull SessionTcpClient sessionTcpClient) {
        forEach(sessionTcpClientObserver -> sessionTcpClientObserver.onSignInStateChanged(sessionTcpClient));
    }

    public void notifySignOutStateChanged(@NonNull SessionTcpClient sessionTcpClient) {
        forEach(sessionTcpClientObserver -> sessionTcpClientObserver.onSignOutStateChanged(sessionTcpClient));
    }

}
