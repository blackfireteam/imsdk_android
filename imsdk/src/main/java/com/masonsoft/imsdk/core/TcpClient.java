package com.masonsoft.imsdk.core;

import androidx.annotation.IntDef;

import com.masonsoft.imsdk.util.Objects;

import java.io.Closeable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
 *
 * @since 1.0
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
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    private final Object mStateLock = new Object();

    @State
    private int mState = STATE_IDLE;

    /**
     * 将长连接状态转换为可读的字符串
     */
    public static String stateToString(@State int state) {
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

    /**
     * 获取当前长连接状态
     */
    @State
    public int getState() {
        synchronized (mStateLock) {
            return mState;
        }
    }

    /**
     * 将当前状态切换到目标状态，如果切换失败，抛出 {@linkplain IllegalStateException} 异常
     *
     * @param state
     */
    protected void moveToState(@State int state) {
        synchronized (mStateLock) {
            if (mState > state) {
                throw new IllegalStateException(Objects.defaultObjectTag(TcpClient.this)
                        + " fail to move state "
                        + stateToString(mState) + " -> " + stateToString(state));
            }
            if (mState != state) {
                final int oldState = mState;
                mState = state;
                this.onStateChanged(oldState, mState);
            }
        }
    }

    /**
     * 校验当前长连接状态必然是指定状态，否则抛出 {@linkplain IllegalStateException} 异常
     *
     * @param state
     */
    protected void checkState(@State int state) {
        synchronized (mStateLock) {
            if (mState != state) {
                throw new IllegalStateException(Objects.defaultObjectTag(TcpClient.this)
                        + " required " + stateToString(state) + " but " + stateToString(mState));
            }
        }
    }

    /**
     * 长连接状态发生了迁移
     *
     * @param oldState 迁移前的状态
     * @param newState 迁移后的状态(当前状态)
     */
    protected void onStateChanged(int oldState, int newState) {
        IMLog.v(Objects.defaultObjectTag(TcpClient.this) + " state changed %s -> %s",
                stateToString(oldState),
                stateToString(newState));
    }

}
