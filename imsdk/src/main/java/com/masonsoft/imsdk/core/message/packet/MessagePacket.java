package com.masonsoft.imsdk.core.message.packet;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.ProtoByteMessage;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.observable.MessagePacketStateObservable;
import com.masonsoft.imsdk.lang.Processor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 本地在长连接上发送的消息包（带有发送状态的消息）
 *
 * @since 1.0
 */
public abstract class MessagePacket implements Processor<ProtoByteMessageWrapper> {

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
    @NonNull
    private final ProtoByteMessage mProtoByteMessage;

    /**
     * 消息包的本地 sign(唯一)
     */
    private final long mSign;

    /**
     * 消息包的发送状态
     */
    @SendState
    private int mState = STATE_IDLE;

    private final MessagePacketStateObservable mMessagePacketStateObservable = new MessagePacketStateObservable();

    private long mErrorCode;
    private String mErrorMessage;

    public MessagePacket(@NonNull final ProtoByteMessage protoByteMessage) {
        this(protoByteMessage, SignGenerator.next());
    }

    public MessagePacket(@NonNull final ProtoByteMessage protoByteMessage, final long sign) {
        mProtoByteMessage = protoByteMessage;
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

    @NonNull
    public ProtoByteMessage getProtoByteMessage() {
        return mProtoByteMessage;
    }

    public int getState() {
        return mState;
    }

    public long getSign() {
        return mSign;
    }

    public MessagePacketStateObservable getMessagePacketStateObservable() {
        return mMessagePacketStateObservable;
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

    protected final Object getStateLock() {
        return mStateLock;
    }

    public void moveToState(@SendState int state) {
        synchronized (mStateLock) {
            if (mState > state) {
                final Object packet = MessagePacket.this;
                final Throwable e = new IllegalStateException(packet.getClass() + "@" + System.identityHashCode(packet)
                        + "[" + mSign + "] fail to move state " + stateToString(mState) + " -> " + stateToString(state));
                IMLog.e(e);
                RuntimeMode.throwIfDebug(e);
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
        IMLog.i("MessagePacket[" + mSign + "] state changed %s -> %s",
                stateToString(oldState),
                stateToString(newState));

        mMessagePacketStateObservable.notifyStateChanged(this, oldState, newState);
    }

    /**
     * 处理服务器返回的消息，如果处理成功返回 true, 否则返回 false。(仅处理服务器对当前消息的回执)
     */
    @Override
    public abstract boolean doProcess(@Nullable ProtoByteMessageWrapper target);

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
