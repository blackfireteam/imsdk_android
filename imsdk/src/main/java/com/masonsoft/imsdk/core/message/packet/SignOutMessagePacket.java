package com.masonsoft.imsdk.core.message.packet;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.Message;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.message.MessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 在长连接上的退出登录消息包
 *
 * @since 1.0
 */
public class SignOutMessagePacket extends TimeoutMessagePacket {

    private SignOutMessagePacket(Message message, long sign) {
        super(message, sign);
    }

    @Override
    public boolean doProcess(@Nullable MessageWrapper target) {
        if (target != null && target.getProtoMessageObject() instanceof ProtoMessage.Result) {
            // 接收 Result 消息
            final ProtoMessage.Result result = (ProtoMessage.Result) target.getProtoMessageObject();
            if (result.getSign() == getSign()) {
                // 校验 sign 是否相等

                synchronized (getStateLock()) {
                    final int state = getState();
                    if (state != STATE_WAIT_RESULT) {
                        IMLog.e("SignOutMessagePacket unexpected. accept with same sign:%s and invalid state:%s", getSign(), stateToString(state));
                        return false;
                    }

                    if (result.getCode() != 0) {
                        setErrorCode(result.getCode());
                        setErrorMessage(result.getMsg());
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

    public static SignOutMessagePacket create() {
        final long sign = SignGenerator.next();
        return new SignOutMessagePacket(
                new Message(
                        Message.Type.IM_LOGOUT,
                        ProtoMessage.ImLogout.newBuilder()
                                .setSign(sign)
                                .build()
                                .toByteArray()
                ),
                sign
        );
    }

}
