package com.masonsoft.imsdk.core.processor;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMConversationManager;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMMessage;
import com.masonsoft.imsdk.core.IMSessionMessage;
import com.masonsoft.imsdk.core.IMSessionMessageUploadManager;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.db.DatabaseHelper;
import com.masonsoft.imsdk.core.db.DatabaseProvider;
import com.masonsoft.imsdk.core.db.DatabaseSessionWriteLock;
import com.masonsoft.imsdk.core.db.LocalSendingMessage;
import com.masonsoft.imsdk.core.db.LocalSendingMessageProvider;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.db.MessageFactory;
import com.masonsoft.imsdk.core.db.Sequence;
import com.masonsoft.imsdk.lang.GeneralResult;

/**
 * 将需要发送的消息写入数据库
 *
 * @since 1.0
 */
public class SendSessionMessageWriteDatabaseProcessor extends SendSessionMessageNotNullValidateProcessor {

    @Override
    protected boolean doNotNullProcess(@NonNull IMSessionMessage target) {
        final long sessionUserId = target.getSessionUserId();
        // 初始化发送队列
        IMSessionMessageUploadManager.getInstance().touch(sessionUserId);

        if (target.isResend()) {
            return resendMessage(target);
        } else {
            return sendNewMessage(target);
        }
    }

    /**
     * 重发一个已有的旧消息
     */
    private boolean resendMessage(@NonNull IMSessionMessage target) {
        final IMMessage message = target.getMessage();
        final long sessionUserId = target.getSessionUserId();
        final int conversationType = IMConstants.ConversationType.C2C;
        final long targetUserId = target.getToUserId();
        final long localMessageId = message.id.get();

        final DatabaseHelper databaseHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
        synchronized (DatabaseSessionWriteLock.getInstance().getSessionWriteLock(databaseHelper)) {
            final Message dbMessage = MessageDatabaseProvider.getInstance().getMessage(
                    sessionUserId,
                    conversationType,
                    targetUserId,
                    localMessageId);
            if (dbMessage == null) {
                // 消息没有找到
                target.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_MESSAGE_ID)
                                .withPayload(target)
                );
                return true;
            }
            if (dbMessage.fromUserId.isUnset()
                    || dbMessage.fromUserId.get() != sessionUserId
                    || sessionUserId <= 0) {
                // 发送者信息不正确
                target.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_FROM_USER_ID)
                                .withPayload(target)
                );
                return true;
            }
            if (dbMessage.toUserId.isUnset()
                    || dbMessage.toUserId.get() != targetUserId
                    || targetUserId <= 0) {
                // 接收者信息不正确
                target.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_TO_USER_ID)
                                .withPayload(target)
                );
                return true;
            }
            if (dbMessage.sign.isUnset()
                    || dbMessage.sign.get() == null
                    || dbMessage.sign.get() == 0) {
                // 重发消息需要有正确的 sign
                target.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_MESSAGE_SIGN)
                                .withPayload(target)
                );
                return true;
            }
            if (dbMessage.remoteMessageId.isUnset()
                    || dbMessage.remoteMessageId.get() > 0) {
                // 有服务器消息 id, 说明消息已经发送成功
                target.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_MESSAGE_SEND_STATUS)
                                .withPayload(target)
                );
                return true;
            }

            final LocalSendingMessage localSendingMessage = LocalSendingMessageProvider.getInstance().getLocalSendingMessageByTargetMessage(
                    sessionUserId,
                    conversationType,
                    targetUserId,
                    localMessageId
            );
            if (localSendingMessage == null || localSendingMessage.localSendStatus.get() != IMConstants.SendStatus.FAIL) {
                // 仅允许对发送失败的消息进行重新发送
                target.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_MESSAGE_SEND_STATUS)
                                .withPayload(target)
                );
                return true;
            }

            final LocalSendingMessage localSendingMessageUpdate = new LocalSendingMessage();
            localSendingMessageUpdate.localId.set(localSendingMessage.localId.get());
            // 重置 abort id
            localSendingMessageUpdate.localAbortId.set(IMConstants.AbortId.RESET);
            // 设置发送状态为 IDLE
            localSendingMessageUpdate.localSendStatus.set(IMConstants.SendStatus.IDLE);
            // 重置 errorCode 与 errorMessage
            localSendingMessageUpdate.errorCode.set(0);
            localSendingMessageUpdate.errorMessage.set(null);

            if (LocalSendingMessageProvider.getInstance().updateLocalSendingMessage(sessionUserId, localSendingMessageUpdate)) {
                IMLog.v("success updateLocalSendingMessage: %s", localSendingMessageUpdate);

                // 提示成功入队
                target.getEnqueueCallback().onCallback(GeneralResult.success().withPayload(target));

                // 通知上传任务队列检查新内容
                IMSessionMessageUploadManager.getInstance().notifySyncLocalSendingMessage(sessionUserId);

                // 返回 true, 终止后续 processor
                return true;
            } else {
                final Throwable e = new IllegalAccessError("updateLocalSendingMessage return false");
                IMLog.e(e);
            }
        }

        return false;
    }

    /**
     * 发送一个新消息
     */
    private boolean sendNewMessage(@NonNull IMSessionMessage target) {
        final IMMessage message = target.getMessage();
        final long sessionUserId = target.getSessionUserId();
        final int conversationType = IMConstants.ConversationType.C2C;
        final long targetUserId = target.getToUserId();

        final DatabaseHelper databaseHelper = DatabaseProvider.getInstance().getDBHelper(sessionUserId);
        synchronized (DatabaseSessionWriteLock.getInstance().getSessionWriteLock(databaseHelper)) {
            final Message dbMessageInsert = MessageFactory.create(message);
            if (dbMessageInsert.fromUserId.isUnset()
                    || dbMessageInsert.fromUserId.get() != sessionUserId
                    || sessionUserId <= 0) {
                // 发送者信息不正确
                target.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_FROM_USER_ID)
                                .withPayload(target)
                );
                return true;
            }
            if (dbMessageInsert.toUserId.isUnset()
                    || dbMessageInsert.toUserId.get() != targetUserId
                    || targetUserId <= 0) {
                // 接收者信息不正确
                target.getEnqueueCallback().onCallback(
                        GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_TO_USER_ID)
                                .withPayload(target)
                );
                return true;
            }

            // 新消息清空 localId
            dbMessageInsert.localId.clear();
            // 设置新消息的 sign
            dbMessageInsert.sign.set(SignGenerator.nextSign());
            // 设置新消息的 seq
            dbMessageInsert.localSeq.set(Sequence.create(SignGenerator.nextMicroSeconds()));
            // 设置新消息的显示时间为当前时间
            dbMessageInsert.localTimeMs.set(System.currentTimeMillis());
            // 清空 remote 相关内容
            dbMessageInsert.remoteMessageId.clear();
            dbMessageInsert.remoteMessageTime.clear();
            // 清空 block id
            dbMessageInsert.localBlockId.clear();

            final SQLiteDatabase db = databaseHelper.getDBHelper().getWritableDatabase();
            db.beginTransaction();
            try {
                // 插入消息表
                if (MessageDatabaseProvider.getInstance().insertMessage(
                        sessionUserId,
                        conversationType,
                        targetUserId,
                        dbMessageInsert)) {

                    // 消息表对应记录插入成功后，在上传队列表插入新记录
                    final LocalSendingMessage localSendingMessageInsert = new LocalSendingMessage();
                    localSendingMessageInsert.conversationType.set(conversationType);
                    localSendingMessageInsert.messageLocalId.set(dbMessageInsert.localId.get());
                    localSendingMessageInsert.targetUserId.set(targetUserId);
                    // 重置 abort id
                    localSendingMessageInsert.localAbortId.set(IMConstants.AbortId.RESET);
                    // 设置发送状态为 IDLE
                    localSendingMessageInsert.localSendStatus.set(IMConstants.SendStatus.IDLE);
                    if (LocalSendingMessageProvider.getInstance().insertLocalSendingMessage(
                            sessionUserId,
                            localSendingMessageInsert)) {
                        IMLog.v("success insertLocalSendingMessage: %s", localSendingMessageInsert);

                        // 上传对列表插入成功后，设置事务完成
                        db.setTransactionSuccessful();

                        // 提示成功入队
                        target.getEnqueueCallback().onCallback(GeneralResult.success().withPayload(target));

                        // 通知上传任务队列检查新内容
                        IMSessionMessageUploadManager.getInstance().notifySyncLocalSendingMessage(sessionUserId);

                        // 更新对应会话的最后一条关联消息
                        IMConversationManager.getInstance().updateConversationLastMessage(
                                sessionUserId,
                                conversationType,
                                targetUserId,
                                dbMessageInsert.localId.get()
                        );

                        // 返回 true, 终止后续 processor
                        return true;
                    } else {
                        final Throwable e = new IllegalAccessError("insertLocalSendingMessage return false");
                        IMLog.e(e);
                    }
                } else {
                    final Throwable e = new IllegalAccessError("insertMessage return false");
                    IMLog.e(e);
                }
            } finally {
                db.endTransaction();
            }
        }

        return false;
    }

}
