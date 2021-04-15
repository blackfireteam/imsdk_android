package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

import io.github.idonans.core.Singleton;

/**
 * IM 统一管理类, 核心类, 业务层使用 IM 功能的最外层入口。包括登录，消息，会话等 IM 强相关功能。
 * IM 运行在单一的进程中，如果业务上是一个多进程应用，需要注意，在所有调用 IM 相关的功能时都需要处于主进程中。
 *
 * @since 1.0
 */
public class IMManager {

    private static final Singleton<IMManager> INSTANCE = new Singleton<IMManager>() {
        @Override
        protected IMManager create() {
            //noinspection InstantiationOfUtilityClass
            return new IMManager();
        }
    };

    @NonNull
    public static IMManager getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private IMManager() {
        NetworkManager.getInstance().attach();
        TcpClientAutoReconnectionManager.getInstance().attach();
    }

}
