package com.masonsoft.imsdk;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.observable.MessagePacketStateObservable;
import com.masonsoft.imsdk.util.Objects;

/**
 * 其它消息
 */
public class OtherMessage implements EnqueueMessage {

    private final long mSessionUserId;
    private final long mSign = SignGenerator.next();
    private final MessagePacket mMessagePacket;

    @NonNull
    private final EnqueueCallback<OtherMessage> mEnqueueCallback;

    @SuppressWarnings("FieldCanBeLocal")
    private final MessagePacketStateObservable.MessagePacketStateObserver mMessagePacketStateObserver = new MessagePacketStateObservable.MessagePacketStateObserver() {
        @Override
        public void onStateChanged(MessagePacket packet, int oldState, int newState) {
            if (mMessagePacket != packet) {
                final Throwable e = new IllegalAccessError("MessagePacket is not equal");
                IMLog.e(e);
                return;
            }
        }
    };

    public OtherMessage(
            long sessionUserId,
            @NonNull MessagePacket messagePacket,
            @NonNull EnqueueCallback<OtherMessage> enqueueCallback) {
        mSessionUserId = sessionUserId;
        mMessagePacket = messagePacket;
        mEnqueueCallback = enqueueCallback;

        mMessagePacket.getMessagePacketStateObservable().registerObserver(mMessagePacketStateObserver);
    }

    public long getSessionUserId() {
        return mSessionUserId;
    }

    public long getSign() {
        return mSign;
    }

    public MessagePacket getMessagePacket() {
        return mMessagePacket;
    }

    @NonNull
    public EnqueueCallback<OtherMessage> getEnqueueCallback() {
        return mEnqueueCallback;
    }

    @NonNull
    public String toShortString() {
        //noinspection StringBufferReplaceableByString
        final StringBuilder builder = new StringBuilder();
        builder.append(Objects.defaultObjectTag(this));
        builder.append(" sessionUserId:").append(mSessionUserId);
        builder.append(" sign:").append(mSign);
        builder.append(" messagePacket:").append(mMessagePacket);
        return builder.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return this.toShortString();
    }

}
