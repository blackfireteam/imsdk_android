package com.masonsoft.imsdk.sample.im;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.ProtoByteMessage;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.packet.NotNullTimeoutMessagePacket;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.util.Objects;

import io.github.idonans.core.thread.Threads;

public class GetIMTokenMessagePacket extends NotNullTimeoutMessagePacket {

    @Nullable
    private String mToken;

    public GetIMTokenMessagePacket(ProtoByteMessage protoByteMessage, long sign) {
        super(protoByteMessage, sign);
    }

    @Nullable
    public String getToken() {
        return mToken;
    }

    @Override
    protected boolean doNotNullProcess(@NonNull ProtoByteMessageWrapper target) {
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
                        IMLog.e(Objects.defaultObjectTag(this)
                                + " unexpected. accept with same sign:%s and invalid state:%s", getSign(), stateToString(state));
                        return false;
                    }

                    if (result.getCode() != 0) {
                        setErrorCode((int) result.getCode());
                        setErrorMessage(result.getMsg());
                        IMLog.e(Objects.defaultObjectTag(this) +
                                " unexpected. errorCode:%s, errorMessage:%s", result.getCode(), result.getMsg());
                        moveToState(STATE_FAIL);
                    } else {
                        mToken = result.getMsg();
                        moveToState(STATE_SUCCESS);
                    }
                }
                return true;
            }
        }

        return false;
    }

    public static GetIMTokenMessagePacket create(final long sign, final long phone) {
        return new GetIMTokenMessagePacket(
                ProtoByteMessage.Type.encode(
                        ProtoMessage.GetImToken.newBuilder()
                                .setSign(sign)
                                .setPhone(phone)
                                .build()
                )
                , sign);
    }

}
