package com.masonsoft.imsdk.core.observable;

import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.util.WeakObservable;

/**
 * @see MessagePacket
 */
public class MessagePacketStateObservable extends WeakObservable<MessagePacketStateObservable.MessagePacketStateObserver> {

    public interface MessagePacketStateObserver {
        void onStateChanged(MessagePacket packet, int oldState, int newState);
    }

    public void notifyStateChanged(MessagePacket packet, int oldState, int newState) {
        forEach(stateObserver -> stateObserver.onStateChanged(packet, oldState, newState));
    }

}