package com.masonsoft.imsdk.message.packet;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMLog;
import com.masonsoft.imsdk.core.Message;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.lang.Processor;
import com.masonsoft.imsdk.message.MessageWrapper;
import com.masonsoft.imsdk.util.WeakObservable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 本地在长连接上发送的消息包（带有发送状态的消息）
 */
public abstract class MessagePacket implements Processor<MessageWrapper> {

    /**
     * 发送消息的默认超时时间, 60 秒
     */
    private static final long DEFAULT_MESSAGE_TIMEOUT_MS = 60 * 1000;

    /**
     * 消息包的发送状态：等待发送
     */
    public static final int STATE_IDLE = 0;

    /**
     * 消息包的发送状态：发送中
     */
    public static final int STATE_GOING = 1;

    /**
     * 消息包的发送状态：等待响应结果
     */
    public static final int STATE_WAIT_RESULT = 2;

    /**
     * 消息包的发送状态：发送成功
     */
    public static final int STATE_SUCCESS = 3;

    /**
     * 消息包的发送状态：发送失败
     */
    public static final int STATE_FAIL = 4;

    @IntDef({STATE_IDLE, STATE_GOING, STATE_WAIT_RESULT, STATE_SUCCESS, STATE_FAIL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SendState {
    }

    private final Object mStateLock = new Object();

    /**
     * 原始消息内容
     */
    private final Message mMessage;

    /**
     * 消息包的本地 sign(唯一)
     */
    private final long mSign;

    /**
     * 消息包的发送状态
     */
    @SendState
    private int mState = STATE_IDLE;

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

    private final StateObservable mStateObservable = new StateObservable();

    private long mErrorCode;
    private String mErrorMessage;

    public MessagePacket(final Message message) {
        this(message, SignGenerator.next());
    }

    public MessagePacket(final Message message, final long sign) {
        mMessage = message;
        mSign = sign;
    }

    /**
     * 将消息包发送状态转换为可读的字符串
     */
    public static String stateToString(@SendState int state) {
        switch (state) {
            case STATE_IDLE:
                return "STATE_IDLE";
            case STATE_GOING:
                return "STATE_GOING";
            case STATE_WAIT_RESULT:
                return "STATE_WAIT_RESULT";
            case STATE_SUCCESS:
                return "STATE_SUCCESS";
            case STATE_FAIL:
                return "STATE_FAIL";
        }

        throw new IllegalStateException("unknown state " + state);
    }

    public Message getMessage() {
        return mMessage;
    }

    public int getState() {
        return mState;
    }

    public long getSign() {
        return mSign;
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

    public StateObservable getStateObservable() {
        return mStateObservable;
    }

    public void setErrorCode(long errorCode) {
        mErrorCode = errorCode;
    }

    public long getErrorCode() {
        return mErrorCode;
    }

    public void setErrorMessage(String errorMessage) {
        mErrorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public void moveToState(@SendState int state) {
        synchronized (mStateLock) {
            if (mState > state) {
                throw new IllegalStateException("MessagePacketSend[" + mSign + "] fail to move state " + stateToString(mState) + " -> " + stateToString(state));
            }
            if (mState != state) {
                final int oldState = mState;
                mState = state;
                this.onStateChanged(oldState, mState);
            }
        }
    }

    /**
     * 消息包发送状态发生了迁移
     *
     * @param oldState 迁移前的状态
     * @param newState 迁移后的状态(当前状态)
     */
    protected void onStateChanged(int oldState, int newState) {
        IMLog.i("MessagePacketSend[" + mSign + "] state changed %s -> %s",
                stateToString(oldState),
                stateToString(newState));

        if (newState == STATE_WAIT_RESULT) {
            // 消息已经从长连接上发送出去，等待服务器响应。此时记录一个时间，用来计算超时。
            mSendTimeMs = System.currentTimeMillis();
        }

        mStateObservable.notifyStateChanged(this, oldState, newState);
    }

    /**
     * 验证消息是否超时，如果超时，则将状态置为发送失败
     */
    public void validateTimeout() {
        synchronized (mStateLock) {
            if (mState == STATE_WAIT_RESULT) {
                // 仅在处于 STATE_WAIT_RESULT 状态时才检查 timeout
                final long diff = System.currentTimeMillis() - mSendTimeMs;
                if (diff > mTimeoutMs) {
                    // 已经超时，设置状态为发送失败
                    IMLog.i("MessagePacketSend[" + mSign + "] timeout");
                    moveToState(STATE_FAIL);
                }
            }
        }
    }

    /**
     * 处理服务器返回的消息，如果处理成功返回 true, 否则返回 false。(仅处理服务器对当前消息的回执)
     */
    @Override
    public abstract boolean doProcess(@Nullable MessageWrapper target);

    public interface StateObserver {
        void onStateChanged(MessagePacket packet, int oldState, int newState);
    }

    public static class StateObservable extends WeakObservable<StateObserver> {
        public void notifyStateChanged(MessagePacket packet, int oldState, int newState) {
            forEach(stateObserver -> stateObserver.onStateChanged(packet, oldState, newState));
        }
    }

    @NonNull
    public String toShortString() {
        return String.format(
                "%s@%s{sign:%s,state:%s}",
                getClass().getSimpleName(),
                System.identityHashCode(this),
                mSign,
                stateToString(mState)
        );
    }

    @Override
    @NonNull
    public String toString() {
        return toShortString();
    }

}
