package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMMessageFactory;
import com.masonsoft.imsdk.core.db.Conversation;
import com.masonsoft.imsdk.core.db.ConversationDatabaseProvider;
import com.masonsoft.imsdk.core.db.LocalSendingMessage;
import com.masonsoft.imsdk.core.db.LocalSendingMessageProvider;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.db.TinyPage;
import com.masonsoft.imsdk.user.UserInfoSyncManager;
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.List;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.Threads;

/**
 * 处理消息相关内容。
 *
 * @since 1.0
 */
public class IMMessageManager {

    private static final Singleton<IMMessageManager> INSTANCE = new Singleton<IMMessageManager>() {
        @Override
        protected IMMessageManager create() {
            return new IMMessageManager();
        }
    };

    @NonNull
    public static IMMessageManager getInstance() {
        IMProcessValidator.validateProcess();

        return INSTANCE.get();
    }

    private IMMessageManager() {
    }

    @WorkerThread
    @NonNull
    private IMMessage buildInternal(@NonNull final Message message) {
        Threads.mustNotUi();

        IMMessage target = IMMessageFactory.create(message);

        if (message.remoteMessageId.get() <= 0) {
            // 消息可能是没有发送成功的
            final long sessionUserId = message._sessionUserId.get();
            if (sessionUserId == message.fromUserId.get()) {
                // 发送的消息，查询发送状态与发送进度
                final LocalSendingMessage localSendingMessage = LocalSendingMessageProvider.getInstance().getLocalSendingMessageByTargetMessage(
                        sessionUserId,
                        message._conversationType.get(),
                        message._targetUserId.get(),
                        message.localId.get()
                );
                if (localSendingMessage == null) {
                    final Throwable e = new IllegalAccessError("localSendingMessage not found: " + message);
                    IMLog.e(e);
                } else {
                    final long localSendingMessageLocalId = localSendingMessage.localId.get();
                    final float progress = IMMessageUploadManager.getInstance().getUploadProgress(sessionUserId, localSendingMessageLocalId);
                    target = IMMessageFactory.merge(target, localSendingMessage);
                    target.sendProgress.set(progress);
                }
            }
        }

        return target;
    }

    @WorkerThread
    @Nullable
    public IMMessage getMessage(
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long localMessageId) {
        Threads.mustNotUi();

        final Message message = MessageDatabaseProvider.getInstance().getMessage(
                sessionUserId,
                conversationType,
                targetUserId,
                localMessageId);
        if (message != null) {
            return buildInternal(message);
        }
        return null;
    }

    @WorkerThread
    @NonNull
    public TinyPage<IMMessage> pageQueryMessage(final long sessionUserId,
                                                final long seq,
                                                final int limit,
                                                final int conversationType,
                                                final long targetUserId,
                                                final boolean queryHistory) {
        Threads.mustNotUi();

        if (seq == 0) {
            // 读取第一页消息时，尝试同步用户信息
            UserInfoSyncManager.getInstance().enqueueSyncUserInfo(sessionUserId);
            UserInfoSyncManager.getInstance().enqueueSyncUserInfo(targetUserId);
        }

        long targetBlockId = 0;
        if (seq > 0) {
            targetBlockId = MessageDatabaseProvider.getInstance().getBlockIdWithSeq(
                    sessionUserId, conversationType, targetUserId, seq, !queryHistory);
        }

        final TinyPage<Message> page = MessageDatabaseProvider.getInstance().pageQueryMessage(
                sessionUserId, seq, limit, conversationType, targetUserId, queryHistory, null);

        final List<IMMessage> filterItems = new ArrayList<>();
        for (Message item : page.items) {
            if (item.localBlockId.get() == 0L) {
                filterItems.add(buildInternal(item));
                continue;
            }

            if (targetBlockId <= 0) {
                targetBlockId = item.localBlockId.get();
                filterItems.add(buildInternal(item));
                continue;
            }

            if (targetBlockId == item.localBlockId.get()) {
                filterItems.add(buildInternal(item));
                continue;
            }

            break;
        }

        final TinyPage<IMMessage> result = new TinyPage<>();
        result.items = filterItems;
        result.hasMore = page.hasMore;
        if (filterItems.size() < page.items.size()) {
            result.hasMore = false;
        }

        if (result.items.size() < limit) {
            // 检查该 block 在此时是否有缺失的消息

            final Conversation conversation = ConversationDatabaseProvider.getInstance()
                    .getConversationByTargetUserId(
                            sessionUserId,
                            conversationType,
                            targetUserId);
            if (conversation == null) {
                final Throwable e = new IllegalAccessError("unexpected. conversation is null");
                IMLog.e(e, "sessionUserId:%s, conversationType:%s, targetUserId:%s",
                        sessionUserId,
                        conversationType,
                        targetUserId);
            } else {
                // 当前会话中的消息是否已经
            }
        }
        // TODO

        IMLog.v(Objects.defaultObjectTag(this) + " pageQueryMessage result:%s, targetBlockId:%s with sessionUserId:%s, seq:%s, limit:%s, conversationType:%s, targetUserId:%s, queryHistory:%s",
                result, targetBlockId, sessionUserId, seq, limit, conversationType, targetUserId, queryHistory);
        return result;
    }

}
