package com.masonsoft.imsdk.core.session;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.github.idonans.core.util.TextUtil;

/**
 * 发起或者保持会话的必要信息。包括链接使用的 token，传输数据使用的加密信息，动态的长连接地址信息等。
 * 所有信息为只读，如果其中任意的信息发生了变更，需要重新创建会话。
 *
 * @since 1.0
 */
public final class Session {

    private final String mToken;
    private final String mAesKey;
    private final String mTcpHost;
    private final int mTcpPort;

    public static Session create(String token, String tcpServerAndPort) {
        return create(token, tcpServerAndPort, null);
    }

    public static Session create(String token, String tcpServerAndPort, String aesKey) {
        final String[] array = tcpServerAndPort.split(":");
        final String tcpHost = array[0];
        final int tcpPort = Integer.parseInt(array[1]);
        return new Session(token, tcpHost, tcpPort, aesKey);
    }

    public Session(String token, String tcpHost, int tcpPort) {
        this(token, tcpHost, tcpPort, null);
    }

    public Session(String token, String tcpHost, int tcpPort, String aesKey) {
        this.mToken = token;
        this.mTcpHost = tcpHost;
        this.mTcpPort = tcpPort;
        this.mAesKey = aesKey;

        TextUtil.checkStringNotEmpty(token, "invalid token");
        TextUtil.checkStringNotEmpty(tcpHost, "invalid tcp host");
        if (tcpPort <= 0 || tcpPort > 65535) {
            throw new IllegalArgumentException("invalid tcp port");
        }
    }

    @NonNull
    public String getToken() {
        return mToken;
    }

    @Nullable
    public String getAesKey() {
        return mAesKey;
    }

    @NonNull
    public String getTcpHost() {
        return mTcpHost;
    }

    @IntRange(from = 0, to = 65535)
    public int getTcpPort() {
        return mTcpPort;
    }

}
