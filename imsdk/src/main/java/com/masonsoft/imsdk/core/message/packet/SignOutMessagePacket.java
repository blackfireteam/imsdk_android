package com.masonsoft.imsdk.core.message.packet;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.ProtoByteMessage;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.util.Objects;

import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.Preconditions;

/**
 * 在长连接上的退出登录消息包
 *
 * @since 1.0
 */
public class SignOutMessagePacket extends NotNullTimeoutMessagePacket {

    private SignOutMessagePacket(ProtoByteMessage protoByteMessage, long sign) {
        super(protoByteMessage, sign);
    }

    @Override
    protected boolean doNotNullProcess(@NonNull SessionProtoByteMessageWrapper target) {
        Preconditions.checkArgument(!Threads.isUi());

        final Object protoMessageObject = target.getProtoByteMessageWrapper().getProtoMessageObject();
        if (protoMessageObject == null) {
            return false;
        }

        // 接收 Result 消息
        if (protoMessageObject instanceof ProtoMessage.Result) {
            final ProtoMessage.Result result = (ProtoMessage.Result) protoMessageObject;

            // 校验 sign 是否相等
            if (result.getSign() == getSign()) {
                synchronized (getStateLock()) {
                    final int state = getState();
                    if (state != STATE_WAIT_RESULT) {
                        IMLog.e(Objects.defaultObjectTag(this) + " unexpected. accept with same sign:%s and invalid state:%s", getSign(), stateToString(state));
                        return false;
                    }

                    if (result.getCode() != 0) {
                        setErrorCode((int) result.getCode());
                        setErrorMessage(result.getMsg());
                        IMLog.e(Objects.defaultObjectTag(this) + " unexpected. errorCode:%s, errorMessage:%s", result.getCode(), result.getMsg());
                        moveToState(STATE_FAIL);
                    } else {
                        moveToState(STATE_SUCCESS);
                    }
                }
                return true;
            }
        }

        return false;
    }

    /**
     * 判断当前是否已经退出登录(数据包状态不是待发送都认为是已经退出登录).
     */
    public boolean isSignOut() {
        return getState() != STATE_IDLE;
    }

    /**
     * 判断当前是否已经成功退出登录(数据包状态为发送成功，已经接收到服务器返回的成功退出登录的结果).
     */
    public boolean isSignOutSuccess() {
        return getState() == STATE_SUCCESS;
    }

    public static SignOutMessagePacket create() {
        final long sign = SignGenerator.nextSign();
        return new SignOutMessagePacket(
                ProtoByteMessage.Type.encode(
                        ProtoMessage.ImLogout.newBuilder()
                                .setSign(sign)
                                .build()),
                sign
        );
    }

}
