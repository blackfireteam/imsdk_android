package com.masonsoft.imsdk.core.processor;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import com.masonsoft.imsdk.lang.Processor;
import com.masonsoft.imsdk.util.Objects;
import com.masonsoft.imsdk.util.TimeDiffDebugHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.idonans.core.util.Preconditions;

/**
 * 批量处理多个收到的新消息(注意：与 ChatRBatch 不同，这个里的每一个 ChatR(包装在 SessionProtoByteMessageWrapper 内) 都必须是新消息, 并且具有相同的 sessionUserId)
 */
public class TinyChatRNewMessageListProcessor implements Processor<List<SessionProtoByteMessageWrapper>> {

    private final TimeDiffDebugHelper mTimeDiffDebugHelper = new TimeDiffDebugHelper(Objects.defaultObjectTag(this));

    @Override
    public boolean doProcess(@Nullable List<SessionProtoByteMessageWrapper> targetList) {
        if (targetList == null) {
            return true;
        }

        // 按照 targetUserId 分组
        final Map<Long, List<SessionProtoByteMessageWrapper>> targetMap = new HashMap<>();
        long sessionUserId = 0L;
        for (SessionProtoByteMessageWrapper target : targetList) {
            Preconditions.checkArgument(sessionUserId == 0L || sessionUserId == target.getSessionUserId());
            sessionUserId = target.getSessionUserId();
            final ProtoMessage.ChatR chatR = (ProtoMessage.ChatR) target.getProtoByteMessageWrapper().getProtoMessageObject();
            final boolean received = sessionUserId != chatR.getFromUid();
            final long targetUserId = received ? chatR.getFromUid() : chatR.getToUid();
            List<SessionProtoByteMessageWrapper> list = targetMap.get(targetUserId);
            if (list == null) {
                list = new ArrayList<>();
                targetMap.put(targetUserId, list);
            }
            list.add(target);
        }

        for (Map.Entry<Long, List<SessionProtoByteMessageWrapper>> entry : targetMap.entrySet()) {
            final List<SessionProtoByteMessageWrapper> list = entry.getValue();
            if (list.isEmpty()) {
                continue;
            }
            mTimeDiffDebugHelper.mark();
            doProcessWithSameTargetUserId(sessionUserId, entry.getKey(), list);
            mTimeDiffDebugHelper.mark();
            mTimeDiffDebugHelper.print("list size:" + list.size() + "/" + targetList.size());
        }
        return true;
    }

    /**
     * chatRList 中的消息都是同一个会话的，并且连续
     */
    private void doProcessWithSameTargetUserId(final long sessionUserId, final long targetUserId, @NonNull final List<SessionProtoByteMessageWrapper> chatRList) {
        Preconditions.checkArgument(!chatRList.isEmpty());
        final List<Message> messageList = new ArrayList<>();
        for (SessionProtoByteMessageWrapper target : chatRList) {
            final ProtoMessage.ChatR chatR = (ProtoMessage.ChatR) target.getProtoByteMessageWrapper().getProtoMessageObject();
            final Message message = MessageFactory.create(chatR);
            // 接收到新消息
            // 设置新消息的 seq
            message.localSeq.set(Sequence.create(SignGenerator.nextMicroSeconds()));
            // 设置新消息的显示时间为当前时间
            message.localTimeMs.set(System.currentTimeMillis());
            messageList.add(message);
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

        final long fromUserId = maxMessage.fromUserId.get();
        final long toUserId = maxMessage.toUserId.get();
        if (fromUserId != sessionUserId && toUserId != sessionUserId) {
            IMLog.e("unexpected. sessionUserId:%s invalid fromUserId and toUserId %s", sessionUserId, maxMessage);
            return;
        }

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
                minMessage.remoteMessageId.get()
        );
        Preconditions.checkArgument(blockId > 0);

        final DatabaseHelper databaseHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
        synchronized (DatabaseSessionWriteLock.getInstance().getSessionWriteLock(databaseHelper)) {
            final SQLiteDatabase database = databaseHelper.getDBHelper().getWritableDatabase();
            database.beginTransaction();
            try {
                int conversationUnreadCountDiff = 0;
                Message conversationBestShowMessage = null;

                for (Message message : messageList) {
                    final boolean actionMessage = message.localActionMessage.get() > 0;
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
                        } else {
                            // 新消息入库成功
                            if (!actionMessage) {
                                conversationUnreadCountDiff++;
                                if (conversationBestShowMessage == null
                                        || conversationBestShowMessage.localSeq.get() < message.localSeq.get()) {
                                    conversationBestShowMessage = message;
                                }
                            }
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

                try {
                    // 更新未读消息数
                    if (conversationUnreadCountDiff != 0) {
                        IMConversationManager.getInstance().increaseConversationUnreadCount(
                                sessionUserId,
                                conversationType,
                                targetUserId,
                                conversationUnreadCountDiff
                        );
                    }

                    // 更新对应会话的最后一条关联消息
                    if (conversationBestShowMessage != null) {
                        IMConversationManager.getInstance().updateConversationLastMessage(
                                sessionUserId,
                                conversationType,
                                targetUserId,
                                conversationBestShowMessage.localId.get()
                        );
                    }
                } catch (Throwable e) {
                    IMLog.e(e);
                }

                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
    }

}
