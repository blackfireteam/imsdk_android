package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageFactory;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 收到一组连续的历史聊天消息
 *
 * @since 1.0
 */
public class ReceivedMessageProtoTypeChatRBatchProcessor extends ReceivedMessageProtoTypeProcessor<ProtoMessage.ChatRBatch> {

    public ReceivedMessageProtoTypeChatRBatchProcessor() {
        super(ProtoMessage.ChatRBatch.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(
            @NonNull SessionProtoByteMessageWrapper target,
            @NonNull ProtoMessage.ChatRBatch protoMessageObject) {
        final long sign = protoMessageObject.getSign();
        final List<Message> messageList = new ArrayList<>();
        final List<ProtoMessage.ChatR> chatRList = protoMessageObject.getMsgsList();
        if (chatRList != null) {
            for (ProtoMessage.ChatR chatR : chatRList) {
                if (chatR != null) {
                    messageList.add(MessageFactory.create(chatR));
                }
            }
        }

        // messageList 中的所有消息是连续的，并且是有序的(按照 msg id 有序)
        if (!messageList.isEmpty()) {
            // TODO 交给消息发送队列处理
        }

        // TODO
        return false;
    }

}
