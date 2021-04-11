package com.masonsoft.imsdk.core.processor;

import android.database.sqlite.SQLiteDatabase;

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
import com.masonsoft.imsdk.core.observable.FetchMessageHistoryObservable;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.util.Preconditions;

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
        final boolean result = this.doNotNullProtoMessageObjectProcessInternal(target, protoMessageObject);
        final long sign = protoMessageObject.getSign();
        FetchMessageHistoryObservable.DEFAULT.notifyMessageHistoryFetched(sign);
        return result;
    }

    private boolean doNotNullProtoMessageObjectProcessInternal(
            @NonNull SessionProtoByteMessageWrapper target,
            @NonNull ProtoMessage.ChatRBatch protoMessageObject) {
        final List<Message> messageList = new ArrayList<>();
        final List<ProtoMessage.ChatR> chatRList = protoMessageObject.getMsgsList();
        if (chatRList != null) {
            for (ProtoMessage.ChatR chatR : chatRList) {
                if (chatR != null) {
                    messageList.add(MessageFactory.create(chatR));
                }
            }
        }

        if (messageList.isEmpty()) {
            IMLog.e(new IllegalArgumentException("unexpected ChatRBatch is empty"));
            return true;
        }

        // messageList 中的所有消息是连续的，并且是有序的(按照 msg id 有序)

        // server message id 最大和最小的那一条(对比第一条和最后一条)
        final Message minMessage, maxMessage;
        {
            final Message firstMessage = messageList.get(0);
            final Message lastMessage = messageList.get(messageList.size() - 1);
            if (firstMessage.remoteMessageId.get() < lastMessage.remoteMessageId.get()) {
                minMessage = firstMessage;
                maxMessage = lastMessage;
            } else {
                minMessage = lastMessage;
                maxMessage = firstMessage;
            }
        }

        final long sessionUserId = target.getSessionUserId();
        final long fromUserId = maxMessage.fromUserId.get();
        final long toUserId = maxMessage.toUserId.get();
        if (fromUserId != sessionUserId && toUserId != sessionUserId) {
            IMLog.e("unexpected. sessionUserId:%s invalid fromUserId and toUserId %s", sessionUserId, maxMessage);
            return false;
        }

        final boolean received = fromUserId != sessionUserId;
        final long targetUserId = received ? fromUserId : toUserId;
        final int conversationType = IMConstants.ConversationType.C2C;

        for (Message message : messageList) {
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
        }

        final long blockId = MessageBlock.generateBlockId(
                sessionUserId,
                conversationType,
                targetUserId,
                maxMessage.remoteMessageId.get()
        );
        Preconditions.checkArgument(blockId > 0);

        final DatabaseHelper databaseHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
        synchronized (DatabaseSessionWriteLock.getInstance().getSessionWriteLock(databaseHelper)) {
            final SQLiteDatabase database = databaseHelper.getDBHelper().getWritableDatabase();
            database.beginTransaction();
            try {
                for (Message message : messageList) {
                    final long remoteMessageId = message.remoteMessageId.get();
                    // 设置 block id
                    message.localBlockId.set(blockId);

                    final Message dbMessage = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                            sessionUserId,
                            conversationType,
                            targetUserId,
                            remoteMessageId);
                    if (dbMessage == null) {
                        // 如果消息在本地不存在，则入库
                        if (!MessageDatabaseProvider.getInstance().insertMessage(
                                sessionUserId,
                                conversationType,
                                targetUserId,
                                message)) {
                            final Throwable e = new IllegalAccessError("unexpected insertMessage return false " + message);
                            IMLog.e(e);
                        }
                    } else {
                        // 消息已经存在
                        // 回写 localId
                        message.localId.set(dbMessage.localId.get());

                        // 更新必要字段
                        boolean requireUpdate = false;
                        final Message messageUpdate = new Message();
                        messageUpdate.applyLogicField(sessionUserId, conversationType, targetUserId);
                        messageUpdate.localId.set(dbMessage.localId.get());

                        {
                            // 校验是否需要更新 message type
                            final int dbMessageType = dbMessage.messageType.get();
                            final int newMessageType = message.messageType.get();
                            if (dbMessageType != newMessageType) {
                                requireUpdate = true;
                                messageUpdate.messageType.set(newMessageType);
                            }
                        }
                        {
                            // 校验是否需要更新 block id
                            final long dbMessageBlockId = dbMessage.localBlockId.get();
                            final long newMessageBlockId = message.localBlockId.get();
                            Preconditions.checkArgument(newMessageBlockId > 0);
                            if (dbMessageBlockId != newMessageBlockId) {
                                requireUpdate = true;
                                messageUpdate.localBlockId.set(newMessageBlockId);
                            }
                        }

                        if (requireUpdate) {
                            if (!MessageDatabaseProvider.getInstance().updateMessage(
                                    sessionUserId, conversationType, targetUserId, messageUpdate)) {
                                final Throwable e = new IllegalAccessError("unexpected updateMessage return false " + messageUpdate);
                                IMLog.e(e);
                            }
                        }
                    }
                }

                // expand block id
                MessageBlock.expandBlockId(sessionUserId, conversationType, targetUserId, minMessage.remoteMessageId.get());

                database.setTransactionSuccessful();

                try {
                    // 更新对应会话的最后一条关联消息
                    IMConversationManager.getInstance().updateConversationLastMessage(
                            sessionUserId,
                            conversationType,
                            targetUserId,
                            maxMessage.localId.get()
                    );
                } catch (Throwable e) {
                    IMLog.e(e);
                }
            } finally {
                database.endTransaction();
            }
        }

        return true;
    }

}
