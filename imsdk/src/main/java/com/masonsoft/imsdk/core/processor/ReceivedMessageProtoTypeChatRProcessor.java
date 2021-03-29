package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMConversationManager;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.block.MessageBlock;
import com.masonsoft.imsdk.core.db.DatabaseHelper;
import com.masonsoft.imsdk.core.db.DatabaseProvider;
import com.masonsoft.imsdk.core.db.DatabaseSessionWriteLock;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.db.MessageFactory;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;

/**
 * 收到一条新的聊天消息
 *
 * @since 1.0
 */
public class ReceivedMessageProtoTypeChatRProcessor extends ReceivedMessageProtoTypeProcessor<ProtoMessage.ChatR> {

    public ReceivedMessageProtoTypeChatRProcessor() {
        super(ProtoMessage.ChatR.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(
            @NonNull SessionProtoByteMessageWrapper target,
            @NonNull ProtoMessage.ChatR protoMessageObject) {
        final Message message = MessageFactory.create(protoMessageObject);
        final long sessionUserId = target.getSessionUserId();
        final long fromUserId = message.fromUserId.get();
        final long toUserId = message.toUserId.get();
        if (fromUserId != sessionUserId && toUserId != sessionUserId) {
            IMLog.e("unexpected. sessionUserId:%s invalid fromUserId and toUserId %s", sessionUserId, message);
            return false;
        }

        final boolean received = fromUserId != sessionUserId;
        final long targetUserId = received ? fromUserId : toUserId;
        final int conversationType = IMConstants.ConversationType.C2C;

        message.applyLogicField(sessionUserId, conversationType, targetUserId);

        final int messageType = message.messageType.get();
        if (messageType == IMConstants.MessageType.REVOKE_MESSAGE) {
            // 撤回了一条消息，如果目标消息在本地，则需要将目标消息的类型修改为已撤回
            final long targetMessageId = Long.parseLong(message.body.get().trim());
            final Message dbMessage = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                    sessionUserId,
                    conversationType,
                    targetUserId,
                    targetMessageId);
            if (dbMessage != null) {
                if (dbMessage.messageType.get() != IMConstants.MessageType.REVOKED) {
                    // 将本地目标消息修改为已撤回
                    final DatabaseHelper databaseHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
                    synchronized (DatabaseSessionWriteLock.getInstance().getSessionWriteLock(databaseHelper)) {
                        final Message messageUpdate = new Message();
                        messageUpdate.localId.apply(dbMessage.localId);
                        messageUpdate.messageType.set(IMConstants.MessageType.REVOKED);
                        MessageDatabaseProvider.getInstance().updateMessage(
                                sessionUserId,
                                conversationType,
                                targetUserId,
                                messageUpdate);
                    }
                }
            }
        }

        final long remoteMessageId = message.remoteMessageId.get();
        final DatabaseHelper databaseHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
        synchronized (DatabaseSessionWriteLock.getInstance().getSessionWriteLock(databaseHelper)) {
            final Message dbMessage = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                    sessionUserId,
                    conversationType,
                    targetUserId,
                    remoteMessageId);
            if (dbMessage == null) {
                // 如果消息在本地不存在，则入库

                // 将发送状态设置为发送成功
                message.localSendStatus.set(IMConstants.SendStatus.SUCCESS);
                // 设置 block id
                message.localBlockId.set(MessageBlock.generateBlockId(
                        sessionUserId,
                        conversationType,
                        targetUserId,
                        remoteMessageId
                ));

                if (MessageDatabaseProvider.getInstance().insertMessage(
                        sessionUserId,
                        conversationType,
                        targetUserId,
                        message)) {
                    MessageBlock.expandBlockId(sessionUserId,
                            conversationType,
                            targetUserId,
                            remoteMessageId);

                    // 更新对应会话的最后一条关联消息
                    IMConversationManager.getInstance().updateConversationLastMessage(
                            sessionUserId,
                            conversationType,
                            targetUserId,
                            message.localId.get()
                    );
                }
            }
        }

        return true;
    }

}
