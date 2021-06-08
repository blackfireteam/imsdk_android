package com.masonsoft.imsdk.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.observable.NetworkObservable;
import com.masonsoft.imsdk.util.Objects;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.util.ContextUtil;

/**
 * 网络链接相关。如监听网络链接的变化
 */
public class NetworkManager {

    private static final Singleton<NetworkManager> INSTANCE = new Singleton<NetworkManager>() {
        @Override
        protected NetworkManager create() {
            return new NetworkManager();
        }
    };

    public static NetworkManager getInstance() {
        return INSTANCE.get();
    }

    private final NetworkCallbackImpl mNetworkCallback = new NetworkCallbackImpl();

    private NetworkManager() {
        registerNetworkCallback();
    }

    public void start() {
        IMLog.v(Objects.defaultObjectTag(this) + " start");
    }

    private void registerNetworkCallback() {
        try {
            final ConnectivityManager connectivityManager = (ConnectivityManager) ContextUtil.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.registerNetworkCallback(
                    new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .build(),
                    mNetworkCallback);
        } catch (Throwable e) {
            IMLog.e(e);
        }
    }

    private static class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            IMLog.v("%s onAvailable %s", Objects.defaultObjectTag(this), network);
            NetworkObservable.DEFAULT.notifyNetworkAvailable(network);
        }
    }

}
