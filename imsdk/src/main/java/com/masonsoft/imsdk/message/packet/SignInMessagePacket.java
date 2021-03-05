package com.masonsoft.imsdk.message.packet;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMLog;
import com.masonsoft.imsdk.core.Message;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.message.MessageWrapper;
import com.masonsoft.imsdk.proto.ProtoMessage;

/**
 * 在长连接上的登录消息包
 */
public class SignInMessagePacket extends MessagePacketSend {

    /**
     * 登录信息中对应的用户 id.(服务器返回的)
     */
    private long mSessionUserId;

    private SignInMessagePacket(Message message, long sign) {
        super(message, sign);
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    @Override
    public boolean doProcess(@Nullable MessageWrapper target) {
        if (target != null && target.getProtoMessageObject() instanceof ProtoMessage.Result) {
            // 接收 Result 消息
            final ProtoMessage.Result result = (ProtoMessage.Result) target.getProtoMessageObject();
            if (result.getSign() == getSign()) {
                // 校验 sign 是否相等

                final int state = getState();
                if (state != STATE_WAIT_RESULT) {
                    IMLog.e("SignInMessagePacket unexpected. accept with same sign:%s and invalid state:%s", getSign(), stateToString(state));
                    return false;
                }

                mSessionUserId = result.getUid();
                if (result.getCode() != 0) {
                    setErrorCode(result.getCode());
                    setErrorMessage(result.getMsg());
                    moveToState(STATE_FAIL);
                } else {
                    if (mSessionUserId <= 0) {
                        IMLog.e("SignInMessagePacket unexpected. accept with same sign:%s and invalid user id:%s", getSign(), mSessionUserId);
                        return false;
                    }
                    moveToState(STATE_SUCCESS);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * 判断当前登录状态是否是处于活动状态(数据包状态为已经发送成功并且获得有效的服务器返回的用户 id).即长连接已经认证通过。
     */
    public boolean isActive() {
        return getState() == STATE_SUCCESS && mSessionUserId > 0;
    }

    public static SignInMessagePacket create(final String token) {
        final long sign = SignGenerator.next();
        return new SignInMessagePacket(
                new Message(
                        Message.Type.IM_LOGIN,
                        ProtoMessage.ImLogin.newBuilder()
                                .setSign(sign)
                                .setToken(token)
                                .build()
                                .toByteArray()
                ),
                sign
        );
    }

}
