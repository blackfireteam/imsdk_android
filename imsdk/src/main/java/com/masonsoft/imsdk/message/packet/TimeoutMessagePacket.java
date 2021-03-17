package com.masonsoft.imsdk.message.packet;

import com.masonsoft.imsdk.IMLog;
import com.masonsoft.imsdk.core.Message;
import com.masonsoft.imsdk.core.WeakClockManager;

/**
 * 能够计算超时的消息发送包
 *
 * @since 1.0
 */
public abstract class TimeoutMessagePacket extends MessagePacket {

    /**
     * 发送消息的默认超时时间, 60 秒
     */
    private static final long DEFAULT_MESSAGE_TIMEOUT_MS = 60 * 1000;

    /**
     * 消息的实际发送时间，毫秒
     *
     * @see #mTimeoutMs
     * @see #validateTimeout()
     */
    private long mSendTimeMs;

    /**
     * 消息的超时时间，毫秒
     *
     * @see #mSendTimeMs
     * @see #validateTimeout()
     */
    private long mTimeoutMs = DEFAULT_MESSAGE_TIMEOUT_MS;

    public TimeoutMessagePacket(Message message) {
        super(message);
    }

    public TimeoutMessagePacket(Message message, long sign) {
        super(message, sign);
    }

    public long getSendTimeMs() {
        return mSendTimeMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        mTimeoutMs = timeoutMs;
    }

    public long getTimeoutMs() {
        return mTimeoutMs;
    }

    // timeout 计时器监听
    private final WeakClockManager.ClockObserver mClockObserver = this::validateTimeout;

    @Override
    protected void onStateChanged(int oldState, int newState) {
        super.onStateChanged(oldState, newState);

        if (newState == STATE_WAIT_RESULT) {
            // 消息已经从长连接上发送出去，等待服务器响应。此时记录一个时间，用来计算超时。
            mSendTimeMs = System.currentTimeMillis();

            // 添加 timeout 计时器监听
            registerClockObserver();
        }
    }

    private void registerClockObserver() {
        synchronized (mClockObserver) {
            WeakClockManager.getInstance().getClockObservable().registerObserver(mClockObserver);
        }
    }

    private void unregisterClockObserver() {
        synchronized (mClockObserver) {
            WeakClockManager.getInstance().getClockObservable().unregisterObserver(mClockObserver);
        }
    }

    /**
     * 立即做一次发送超时校验，如果超时，则将状态置为发送失败
     */
    private void validateTimeoutImmediately() {
        validateTimeout();
    }

    /**
     * 验证消息是否超时，如果超时，则将状态置为发送失败
     */
    private void validateTimeout() {
        synchronized (getStateLock()) {
            if (getState() == STATE_WAIT_RESULT) {
                // 仅在处于 STATE_WAIT_RESULT 状态时才检查 timeout
                final long diff = System.currentTimeMillis() - mSendTimeMs;
                if (diff > mTimeoutMs) {
                    // 已经超时，设置状态为发送失败
                    IMLog.e("TimeoutMessagePacket[" + getSign() + "] timeout");
                    moveToState(STATE_FAIL);

                    // 移除 timeout 计时器监听
                    unregisterClockObserver();
                }
            }
        }
    }

}
