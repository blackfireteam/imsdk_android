package com.masonsoft.imsdk.message.packet;

import androidx.annotation.Nullable;

import com.idonans.core.thread.Threads;
import com.masonsoft.imsdk.IMLog;
import com.masonsoft.imsdk.core.Message;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.message.MessageWrapper;
import com.masonsoft.imsdk.proto.ProtoMessage;

/**
 * 在长连接上的登录消息包
 *
 * @since 1.0
 */
public class SignInMessagePacket extends MessagePacket {

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
        // check thread state
        Threads.mustNotUi();

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

                if (result.getCode() != 0) {
                    setErrorCode(result.getCode());
                    setErrorMessage(result.getMsg());
                    moveToState(STATE_FAIL);
                } else {
                    final long sessionUserId = result.getUid();
                    if (sessionUserId <= 0) {
                        IMLog.e("SignInMessagePacket unexpected. accept with same sign:%s and invalid user id:%s", getSign(), sessionUserId);
                        return false;
                    }
                    mSessionUserId = sessionUserId;
                    moveToState(STATE_SUCCESS);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * 判断当前是否登录成功(数据包状态为已经发送成功并且获得有效的服务器返回的用户 id).
     */
    public boolean isSignIn() {
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
