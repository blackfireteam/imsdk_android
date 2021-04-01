package com.masonsoft.imsdk.core.message.packet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.ProtoByteMessage;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 忽略 Result 的 message packet
 *
 * @since 1.0
 */
public class ResultIgnoreMessagePacket extends MessagePacket {

    public ResultIgnoreMessagePacket(@NonNull ProtoByteMessage protoByteMessage) {
        super(protoByteMessage, IMConstants.RESULT_SIGN_IGNORE);
    }

    @Override
    public boolean doProcess(@Nullable ProtoByteMessageWrapper target) {
        if (target == null) {
            return false;
        }

        final Object protoMessageObject = target.getProtoMessageObject();
        if (protoMessageObject instanceof ProtoMessage.Result) {
            return ((ProtoMessage.Result) protoMessageObject).getSign() == IMConstants.RESULT_SIGN_IGNORE;
        }

        return false;
    }

}
