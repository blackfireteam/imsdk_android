package com.masonsoft.imsdk.socket;

import androidx.annotation.IntDef;

import com.masonsoft.imsdk.util.IMLog;

import java.io.Closeable;

/**
 * 处理 tcp 长连接. 该长连接是一次性的，如果需要重连，需要关闭连接，并重新创建.<br/>
 * 链接的状态为单向变化，在一个链接生命周期中，每一个状态至多只存在一次。<br/>
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
public abstract class TcpClient implements Closeable {

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

    private static String translateStateAsHumanRead(@State int state) {
        switch (state) {
            case STATE_IDLE:
                return "STATE_IDLE";
            case STATE_CONNECTING:
                return "STATE_CONNECTING";
            case STATE_CONNECTED:
                return "STATE_CONNECTED";
            case STATE_CLOSED:
                return "STATE_CLOSED";
        }

        throw new IllegalStateException("unknown state " + state);
    }

    protected int getState() {
        return mState;
    }

    /**
     * 将当前状态切换到目标状态，如果切换失败，抛出 {@linkplain IllegalStateException} 异常
     *
     * @param state
     */
    protected void moveToState(@State int state) {
        if (mState > state) {
            throw new IllegalStateException();
        }
        if (mState != state) {
            final int oldState = mState;
            mState = state;
            this.onStateChanged(oldState, mState);
        }
    }

    /**
     * 长连接状态发生了迁移
     *
     * @param oldState 迁移前的状态
     * @param newState 迁移后的状态(当前状态)
     */
    protected void onStateChanged(int oldState, int newState) {
        IMLog.i("TcpClient state changed %s -> %s",
                translateStateAsHumanRead(oldState),
                translateStateAsHumanRead(newState));
    }

}
