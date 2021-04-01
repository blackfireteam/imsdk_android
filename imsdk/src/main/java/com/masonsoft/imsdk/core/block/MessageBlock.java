package com.masonsoft.imsdk.core.block;

import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.db.DatabaseHelper;
import com.masonsoft.imsdk.core.db.DatabaseProvider;
import com.masonsoft.imsdk.core.db.DatabaseSessionWriteLock;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;

public class MessageBlock {

    /**
     * 获取目标 Message 的最佳 block id
     *
     * @param sessionUserId
     * @param conversationType
     * @param targetUserId
     * @param remoteMessageId
     * @return
     */
    public static long generateBlockId(final long sessionUserId,
                                       final int conversationType,
                                       final long targetUserId,
                                       final long remoteMessageId) {
        final long nextBlockId = getMessageBlockId(
                sessionUserId, conversationType, targetUserId, remoteMessageId + 1);
        if (nextBlockId > 0) {
            return nextBlockId;
        }

        final long preBlockId = getMessageBlockId(
                sessionUserId, conversationType, targetUserId, remoteMessageId - 1);
        if (preBlockId > 0) {
            return preBlockId;
        }

        return SignGenerator.next();
    }

    /**
     * 将目标 Message 的 block id 尽可能向历史消息的方向扩展
     *
     * @param sessionUserId
     * @param conversationType
     * @param targetUserId
     * @param remoteMessageId
     */
    public static void expandBlockId(final long sessionUserId,
                                     final int conversationType,
                                     final long targetUserId,
                                     final long remoteMessageId) {
        final DatabaseHelper databaseHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
        synchronized (DatabaseSessionWriteLock.getInstance().getSessionWriteLock(databaseHelper)) {
            final long blockId = getMessageBlockId(sessionUserId, conversationType, targetUserId, remoteMessageId);
            if (blockId > 0) {
                final long preBlockId = getMessageBlockId(sessionUserId, conversationType, targetUserId, remoteMessageId - 1);
                if (preBlockId > 0 && preBlockId != blockId) {
                    // 将 preBlockId 修改为 blockId
                    MessageDatabaseProvider.getInstance().updateBlockId(
                            sessionUserId,
                            conversationType,
                            targetUserId,
                            preBlockId,
                            blockId
                    );
                }
            }
        }
    }

    private static long getMessageBlockId(final long sessionUserId,
                                          final int conversationType,
                                          final long targetUserId,
                                          final long remoteMessageId) {
        final Message dbMessage = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                sessionUserId,
                conversationType,
                targetUserId,
                remoteMessageId);
        if (dbMessage != null) {
            return dbMessage.localBlockId.getOrDefault(0L);
        }
        return 0L;
    }

}
