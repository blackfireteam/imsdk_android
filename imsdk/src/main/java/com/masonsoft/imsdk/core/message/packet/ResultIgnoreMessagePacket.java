package com.masonsoft.imsdk.core.message.packet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.ProtoByteMessage;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 忽略 Result 的 message packet
 *
 * @since 1.0
 */
public class ResultIgnoreMessagePacket extends MessagePacket {

    /**
     * 忽略 Result 信息的 sign
     */
    public static final long SIGN_IGNORE = Long.MIN_VALUE / 10;

    public ResultIgnoreMessagePacket(@NonNull ProtoByteMessage protoByteMessage) {
        super(protoByteMessage, SIGN_IGNORE);
    }

    @Override
    public boolean doProcess(@Nullable SessionProtoByteMessageWrapper target) {
        if (target == null) {
            return false;
        }

        final Object protoMessageObject = target.getProtoByteMessageWrapper().getProtoMessageObject();
        if (protoMessageObject instanceof ProtoMessage.Result) {
            return ((ProtoMessage.Result) protoMessageObject).getSign() == SIGN_IGNORE;
        }

        return false;
    }

}
