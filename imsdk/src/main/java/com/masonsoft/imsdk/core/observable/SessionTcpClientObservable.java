package com.masonsoft.imsdk.core.observable;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.message.packet.SignInMessagePacket;
import com.masonsoft.imsdk.core.message.packet.SignOutMessagePacket;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.util.WeakObservable;

public class SessionTcpClientObservable extends WeakObservable<SessionTcpClientObservable.SessionTcpClientObserver> {

    public static final SessionTcpClientObservable DEFAULT = new SessionTcpClientObservable();

    public interface SessionTcpClientObserver {
        void onConnectionStateChanged(@NonNull SessionTcpClient sessionTcpClient);

        void onSignInStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignInMessagePacket messagePacket);

        void onSignOutStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignOutMessagePacket messagePacket);
    }

    public void notifyConnectionStateChanged(@NonNull SessionTcpClient sessionTcpClient) {
        forEach(sessionTcpClientObserver -> sessionTcpClientObserver.onConnectionStateChanged(sessionTcpClient));
    }

    public void notifySignInStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignInMessagePacket messagePacket) {
        forEach(sessionTcpClientObserver -> sessionTcpClientObserver.onSignInStateChanged(sessionTcpClient, messagePacket));
    }

    public void notifySignOutStateChanged(@NonNull SessionTcpClient sessionTcpClient, @NonNull SignOutMessagePacket messagePacket) {
        forEach(sessionTcpClientObserver -> sessionTcpClientObserver.onSignOutStateChanged(sessionTcpClient, messagePacket));
    }

}
