package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMConversationManager;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.block.MessageBlock;
import com.masonsoft.imsdk.core.db.DatabaseHelper;
import com.masonsoft.imsdk.core.db.DatabaseProvider;
import com.masonsoft.imsdk.core.db.DatabaseSessionWriteLock;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.db.MessageFactory;
import com.masonsoft.imsdk.core.db.Sequence;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.user.UserInfoSyncManager;
import com.masonsoft.imsdk.util.Preconditions;

/**
 * 处理单条 ChatR 消息
 *
 * @since 1.0
 */
public class TinyChatRProcessor extends ReceivedProtoMessageProtoTypeProcessor<ProtoMessage.ChatR> {

    public TinyChatRProcessor() {
        super(ProtoMessage.ChatR.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(@NonNull SessionProtoByteMessageWrapper wrapper, @NonNull ProtoMessage.ChatR target) {
        {
            final long messageTime = target.getMsgTime();
            wrapper.getSessionTcpClient().setConversationListUpdateTimeTmp(messageTime);
        }

        final Message message = MessageFactory.create(target);
        // 接收到新消息
        // 设置新消息的 seq
        message.localSeq.set(Sequence.create(SignGenerator.next()));
        // 设置新消息的显示时间为当前时间
        message.localTimeMs.set(System.currentTimeMillis());

        final long sessionUserId = wrapper.getSessionUserId();
        final long fromUserId = message.fromUserId.get();
        final long toUserId = message.toUserId.get();
        if (fromUserId != sessionUserId && toUserId != sessionUserId) {
            IMLog.e("unexpected. sessionUserId:%s invalid fromUserId and toUserId %s", sessionUserId, message);
            return false;
        }

        {
            // 同步 from user id 的用户信息
            final long fromUserIdLastModifyMs = message.remoteFromUserProfileLastModifyMs.getOrDefault(0L);
            UserInfoSyncManager.getInstance().enqueueSyncUserInfo(fromUserId, fromUserIdLastModifyMs);
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
        synchronized (DatabaseSessionWriteLock.getInstance().

                getSessionWriteLock(databaseHelper)) {
            final Message dbMessage = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                    sessionUserId,
                    conversationType,
                    targetUserId,
                    remoteMessageId);
            if (dbMessage == null) {
                // 消息在本地不存在，入库

                // 设置 block id
                final long generateBlockId = MessageBlock.generateBlockId(
                        sessionUserId,
                        conversationType,
                        targetUserId,
                        remoteMessageId
                );
                Preconditions.checkArgument(generateBlockId > 0);
                message.localBlockId.set(generateBlockId);

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
                    // 更新未读消息数
                    IMConversationManager.getInstance().increaseConversationUnreadCount(
                            sessionUserId,
                            conversationType,
                            targetUserId,
                            message
                    );
                } else {
                    final Throwable e = new IllegalAccessError("unexpected insertMessage return false " + message);
                    IMLog.e(e);
                }
            } else {
                // 消息在本地存在
                // 校验是否需要更新 messageType
                final int dbMessageType = dbMessage.messageType.get();
                final int newMessageType = message.messageType.get();
                if (dbMessageType != newMessageType) {
                    // 更新 message type
                    final Message messageUpdate = new Message();
                    messageUpdate.localId.set(dbMessage.localId.get());
                    messageUpdate.messageType.set(newMessageType);
                    if (!MessageDatabaseProvider.getInstance().updateMessage(
                            sessionUserId,
                            conversationType,
                            targetUserId,
                            messageUpdate)) {
                        final Throwable e = new IllegalAccessError("unexpected updateMessage return false " + messageUpdate);
                        IMLog.e(e);
                    }
                }

                // 校验是否需要更新 block id
                final long generateBlockId = MessageBlock.generateBlockId(
                        sessionUserId,
                        conversationType,
                        targetUserId,
                        remoteMessageId
                );
                Preconditions.checkArgument(generateBlockId > 0);

                if (dbMessage.localBlockId.get() != generateBlockId) {
                    // 更新 block id
                    if (dbMessage.localBlockId.get() <= 0) {
                        // 更新一条
                        final Message messageUpdate = new Message();
                        messageUpdate.localId.set(dbMessage.localId.get());
                        messageUpdate.localBlockId.set(generateBlockId);
                        if (!MessageDatabaseProvider.getInstance().updateMessage(
                                sessionUserId,
                                conversationType,
                                targetUserId,
                                messageUpdate)) {
                            final Throwable e = new IllegalAccessError("unexpected updateMessage return false " + messageUpdate);
                            IMLog.e(e);
                        }
                    } else {
                        // 更新多条
                        if (!MessageDatabaseProvider.getInstance().updateBlockId(
                                sessionUserId,
                                conversationType,
                                targetUserId,
                                dbMessage.localBlockId.get(),
                                generateBlockId)) {
                            final Throwable e = new IllegalAccessError("unexpected updateBlockId return false");
                            IMLog.e(e);
                        }
                    }
                }
            }
        }

        return true;
    }

}
