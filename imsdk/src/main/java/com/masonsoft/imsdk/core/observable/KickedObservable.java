package com.masonsoft.imsdk.core.observable;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.util.WeakObservable;

/**
 * 踢下线通知
 */
public class KickedObservable extends WeakObservable<KickedObservable.KickedObserver> {

    public static final KickedObservable DEFAULT = new KickedObservable();

    public interface KickedObserver {
        void onKicked(@NonNull final Session session, int errorCode);
    }

    public void notifyKicked(@NonNull final Session session, int errorCode) {
        forEach(kickedObserver -> kickedObserver.onKicked(session, errorCode));
    }

}
