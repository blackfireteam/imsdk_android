package com.masonsoft.imsdk.core.message.packet;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.ProtoByteMessage;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.util.Objects;

import io.github.idonans.core.thread.Threads;

/**
 * 在长连接上的登录消息包
 *
 * @since 1.0
 */
public class SignInMessagePacket extends NotNullTimeoutMessagePacket {

    /**
     * 登录信息中对应的用户 id.(服务器返回的)
     */
    private long mSessionUserId;

    private SignInMessagePacket(ProtoByteMessage protoByteMessage, long sign) {
        super(protoByteMessage, sign);
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    @Override
    protected boolean doNotNullProcess(@NonNull ProtoByteMessageWrapper target) {
        // check thread state
        Threads.mustNotUi();

        final Object protoMessageObject = target.getProtoMessageObject();
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
                        IMLog.e(Objects.defaultObjectTag(SignInMessagePacket.this)
                                + " unexpected. accept with same sign:%s and invalid state:%s", getSign(), stateToString(state));
                        return false;
                    }

                    if (result.getCode() != 0) {
                        setErrorCode((int) result.getCode());
                        setErrorMessage(result.getMsg());
                        IMLog.e(Objects.defaultObjectTag(SignInMessagePacket.this) +
                                " unexpected. errorCode:%s, errorMessage:%s", result.getCode(), result.getMsg());
                        moveToState(STATE_FAIL);
                    } else {
                        final long sessionUserId = result.getUid();
                        if (sessionUserId <= 0) {
                            IMLog.e("SignInMessagePacket unexpected. accept with same sign:%s and invalid user id:%s", getSign(), sessionUserId);
                            return false;
                        }
                        IMLog.v(Objects.defaultObjectTag(SignInMessagePacket.this) + " sign in success, sessionUserId:%s", sessionUserId);
                        mSessionUserId = sessionUserId;
                        moveToState(STATE_SUCCESS);
                    }
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
                ProtoByteMessage.Type.encode(ProtoMessage.ImLogin.newBuilder()
                        .setSign(sign)
                        .setToken(token)
                        .build()),
                sign
        );
    }

}
