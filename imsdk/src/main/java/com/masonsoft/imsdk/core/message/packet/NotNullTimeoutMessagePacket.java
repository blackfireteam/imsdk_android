package com.masonsoft.imsdk.core.message.packet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.ProtoByteMessage;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;

/**
 * @since 1.0
 */
public abstract class NotNullTimeoutMessagePacket extends TimeoutMessagePacket {

    public NotNullTimeoutMessagePacket(ProtoByteMessage protoByteMessage) {
        super(protoByteMessage);
    }

    public NotNullTimeoutMessagePacket(ProtoByteMessage protoByteMessage, long sign) {
        super(protoByteMessage, sign);
    }

    @Override
    public final boolean doProcess(@Nullable ProtoByteMessageWrapper target) {
        if (target == null) {
            return false;
        }
        return doNotNullProcess(target);
    }

    protected abstract boolean doNotNullProcess(@NonNull ProtoByteMessageWrapper target);

}
