package com.masonsoft.imsdk.core.processor;

import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMMessageUploadManager;
import com.masonsoft.imsdk.core.SignGenerator;
import com.masonsoft.imsdk.core.db.DatabaseHelper;
import com.masonsoft.imsdk.core.db.DatabaseProvider;
import com.masonsoft.imsdk.core.db.DatabaseSessionWriteLock;
import com.masonsoft.imsdk.core.db.IdleSendingMessage;
import com.masonsoft.imsdk.core.db.IdleSendingMessageProvider;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.db.MessageFactory;
import com.masonsoft.imsdk.core.db.Sequence;

/**
 * 将需要发送的消息写入数据库
 *
 * @since 1.0
 */
public class SendMessageWriteDatabaseProcessor extends SendMessageNotNullValidateProcessor {

    @Override
    protected boolean doNotNullProcess(@NonNull IMSessionMessage target) {
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
        final IMMessage message = target.getIMMessage();
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
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_MESSAGE_ID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_message_id)
                );
                return true;
            }
            if (dbMessage.fromUserId.isUnset()
                    || dbMessage.fromUserId.get() != sessionUserId
                    || sessionUserId <= 0) {
                // 发送者信息不正确
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_FROM_USER_ID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_from_user_id)
                );
                return true;
            }
            if (dbMessage.toUserId.isUnset()
                    || dbMessage.toUserId.get() != targetUserId
                    || targetUserId <= 0) {
                // 接收者信息不正确
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_TO_USER_ID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_to_user_id)
                );
                return true;
            }

            final IdleSendingMessage idleSendingMessage = IdleSendingMessageProvider.getInstance().getIdleSendingMessageByTargetMessage(
                    sessionUserId,
                    conversationType,
                    targetUserId,
                    localMessageId,
                    null
            );
            if (idleSendingMessage != null) {
                // 消息已经在发送队列内了
                // 做容错处理, 提示成功入队
                target.getEnqueueCallback().onEnqueueSuccess(target);

                // 通知上传任务队列检查新内容
                IMMessageUploadManager.getInstance().notifySyncIdleSendingMessage();

                // 返回 true, 终止后续 processor
                return true;
            } else {
                // 消息没有在上传队列中

                final SQLiteDatabase db = databaseHelper.getDBHelper().getWritableDatabase();
                db.beginTransaction();
                try {
                    // 更新消息表的状态
                    final Message messageUpdate = new Message();
                    messageUpdate.localId.set(dbMessage.localId.get());
                    // 将消息的发送状态设置为 IDLE
                    messageUpdate.localSendStatus.set(IMConstants.SendStatus.IDLE);
                    if (MessageDatabaseProvider.getInstance().updateMessage(
                            sessionUserId,
                            conversationType,
                            targetUserId,
                            messageUpdate)) {

                        // 消息表对应记录变更成功后，在上传队列表插入新记录
                        final IdleSendingMessage idleSendingMessageInsert = new IdleSendingMessage();
                        idleSendingMessageInsert.conversationType.set(conversationType);
                        idleSendingMessageInsert.messageLocalId.set(dbMessage.localId.get());
                        idleSendingMessageInsert.targetUserId.set(targetUserId);
                        if (IdleSendingMessageProvider.getInstance().insertIdleSendingMessage(
                                sessionUserId,
                                idleSendingMessageInsert)) {
                            IMLog.v("success insert idle sending message: %s", idleSendingMessageInsert);

                            // 上传对列表插入成功后，设置事务完成
                            db.setTransactionSuccessful();

                            // 提示成功入队
                            target.getEnqueueCallback().onEnqueueSuccess(target);

                            // 通知上传任务队列检查新内容
                            IMMessageUploadManager.getInstance().notifySyncIdleSendingMessage();

                            // 返回 true, 终止后续 processor
                            return true;
                        } else {
                            final Throwable e = new IllegalAccessError("insertIdleSendingMessage return false");
                            IMLog.e(e);
                        }
                    } else {
                        final Throwable e = new IllegalAccessError("updateMessage return false");
                        IMLog.e(e);
                    }
                } finally {
                    db.endTransaction();
                }
            }
        }

        return false;
    }

    /**
     * 发送一个新消息
     */
    private boolean sendNewMessage(@NonNull IMSessionMessage target) {
        final IMMessage message = target.getIMMessage();
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
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_FROM_USER_ID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_from_user_id)
                );
                return true;
            }
            if (dbMessageInsert.toUserId.isUnset()
                    || dbMessageInsert.toUserId.get() != targetUserId
                    || targetUserId <= 0) {
                // 接收者信息不正确
                target.getEnqueueCallback().onEnqueueFail(
                        target,
                        IMSessionMessage.EnqueueCallback.ERROR_CODE_INVALID_TO_USER_ID,
                        I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_to_user_id)
                );
                return true;
            }

            // 新消息清空 localId
            dbMessageInsert.localId.clear();
            // 设置新消息的 seq
            dbMessageInsert.localSeq.set(Sequence.create(SignGenerator.next()));
            // 设置新消息的显示时间为当前时间
            dbMessageInsert.localTimeMs.set(System.currentTimeMillis());
            // 重置 errorCode 与 errorMessage
            dbMessageInsert.errorCode.clear();
            dbMessageInsert.errorMessage.clear();
            // 重置发送状态为 IDLE
            dbMessageInsert.localSendStatus.set(IMConstants.SendStatus.IDLE);
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
                    final IdleSendingMessage idleSendingMessageInsert = new IdleSendingMessage();
                    idleSendingMessageInsert.conversationType.set(conversationType);
                    idleSendingMessageInsert.messageLocalId.set(dbMessageInsert.localId.get());
                    idleSendingMessageInsert.targetUserId.set(targetUserId);
                    if (IdleSendingMessageProvider.getInstance().insertIdleSendingMessage(
                            sessionUserId,
                            idleSendingMessageInsert)) {
                        IMLog.v("success insert idle sending message: %s", idleSendingMessageInsert);

                        // 上传对列表插入成功后，设置事务完成
                        db.setTransactionSuccessful();

                        // 提示成功入队
                        target.getEnqueueCallback().onEnqueueSuccess(target);

                        // 通知上传任务队列检查新内容
                        IMMessageUploadManager.getInstance().notifySyncIdleSendingMessage();

                        // 返回 true, 终止后续 processor
                        return true;
                    } else {
                        final Throwable e = new IllegalAccessError("insertIdleSendingMessage return false");
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
