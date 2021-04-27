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
 * 获取会话列表
 *
 * @since 1.0
 */
public class FetchConversationListMessagePacket extends NotNullTimeoutMessagePacket {

    private FetchConversationListMessagePacket(ProtoByteMessage protoByteMessage, long sign) {
        super(protoByteMessage, sign);
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
                        IMLog.e(Objects.defaultObjectTag(FetchConversationListMessagePacket.this)
                                + " unexpected. accept with same sign:%s and invalid state:%s", getSign(), stateToString(state));
                        return false;
                    }

                    final long code = result.getCode();
                    if (code == 12) {
                        // 会话列表为空
                        IMLog.v(Objects.defaultObjectTag(FetchConversationListMessagePacket.this) + " conversation list is empty");
                        moveToState(STATE_SUCCESS);
                    } else if (code != 0) {
                        setErrorCode((int) result.getCode());
                        setErrorMessage(result.getMsg());
                        IMLog.e(Objects.defaultObjectTag(FetchConversationListMessagePacket.this) +
                                " unexpected. errorCode:%s, errorMessage:%s", result.getCode(), result.getMsg());
                        moveToState(STATE_FAIL);
                    } else {
                        IMLog.v(Objects.defaultObjectTag(FetchConversationListMessagePacket.this) + " success");
                        moveToState(STATE_SUCCESS);
                    }
                }
                return true;
            }
        }

        return false;
    }

    public static FetchConversationListMessagePacket create(final long updateTime) {
        final long sign = SignGenerator.next();
        return new FetchConversationListMessagePacket(
                ProtoByteMessage.Type.encode(ProtoMessage.GetChatList.newBuilder()
                        .setSign(sign)
                        .setUpdateTime(updateTime)
                        .build()),
                sign
        );
    }

}
