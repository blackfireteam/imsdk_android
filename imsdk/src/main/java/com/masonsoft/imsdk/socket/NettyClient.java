package com.masonsoft.imsdk.socket;

import androidx.annotation.IntDef;

import java.io.Closeable;

/**
 * 处理 Netty 长连接. 该长连接是一次性的，如果需要重连，需要关闭连接，并重新创建.<br/>
 * 链接状态的变化，单向变化，在一个链接生命周期中，每一个状态至多只存在一次。<br/>
 * 所有可能存在的状态迁移路径是：<br/>
 * <ol>
 *   <li>
 *     {@linkplain #STATE_IDLE} -> {@linkplain #STATE_CONNECTING} -> {@linkplain #STATE_CONNECTED} -> {@linkplain #STATE_CLOSED}
 *   </li>
 *   <li>
 *     {@linkplain #STATE_IDLE} -> {@linkplain #STATE_CONNECTING} -> {@linkplain #STATE_CLOSED}
 *   </li>
 *   <li>
 *     {@linkplain #STATE_IDLE} -> {@linkplain #STATE_CLOSED}
 *   </li>
 * </ol>
 */
public abstract class NettyClient implements Closeable {

    /**
     * 当前长连接处于空闲状态(默认状态)
     */
    public static final int STATE_IDLE = 0;
    /**
     * 发起长连接中，此时长连接尚未建立。
     */
    public static final int STATE_CONNECTING = 1;
    /**
     * 长连接建立成功，此时可以在长连接上收发数据
     */
    public static final int STATE_CONNECTED = 2;
    /**
     * 长连接已关闭，不能再使用.
     */
    public static final int STATE_CLOSED = 3;

    @IntDef({STATE_IDLE, STATE_CONNECTING, STATE_CONNECTED, STATE_CLOSED})
    public @interface State {
    }

    @State
    private int mState = STATE_IDLE;

    private void moveToState(@State int state) {
        // TODO
    }

}
