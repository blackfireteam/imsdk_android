package com.masonsoft.imsdk.core.observable;

import android.net.Network;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.util.WeakObservable;

/**
 * 网络状态的变更
 *
 * @since 1.0
 */
public class NetworkObservable extends WeakObservable<NetworkObservable.NetworkObserver> {

    public static final NetworkObservable DEFAULT = new NetworkObservable();

    public interface NetworkObserver {
        void onNetworkAvailable(@NonNull Network network);
    }

    public void notifyNetworkAvailable(@NonNull Network network) {
        forEach(networkObserver -> networkObserver.onNetworkAvailable(network));
    }

}