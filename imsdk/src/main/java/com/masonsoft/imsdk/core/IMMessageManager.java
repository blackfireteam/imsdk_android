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
import com.masonsoft.imsdk.core.observable.ClockObservable;
import com.masonsoft.imsdk.core.observable.FetchMessageHistoryObservable;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.user.UserInfoSyncManager;
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.List;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.Threads;
import io.reactivex.rxjava3.subjects.SingleSubject;

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

    private static final long TIMEOUT_MS = 20 * 1000L;

    private IMMessageManager() {
        Threads.postBackground(() -> {
            IMManager.getInstance().attach();
        });
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
                    final float progress = IMSessionMessageUploadManager.getInstance().getUploadProgress(sessionUserId, localSendingMessageLocalId);
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
            // 检查是否有缺失的消息
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
                boolean requireLoadMoreFromRemote = false;
                if (queryHistory) {
                    if (targetBlockId > 0) {
                        // 检查 block start 是否到达了 conversation message start
                        final Message blockStartMessage = MessageDatabaseProvider.getInstance().getMinRemoteMessageIdWithBlockId(
                                sessionUserId,
                                conversationType,
                                targetUserId,
                                targetBlockId
                        );
                        if (blockStartMessage != null) {
                            final long blockStartRemoteMessageId = blockStartMessage.remoteMessageId.get();
                            if (blockStartRemoteMessageId > 1) {
                                // 还有更多历史消息没有加载
                                requireLoadMoreFromRemote = true;
                            }
                        } else {
                            // unexpected. block id 逻辑错误
                            //noinspection ConstantConditions
                            IMLog.e(new IllegalArgumentException("unexpected. block start message id null."),
                                    "sessionUserId:%s, conversationType:%s, targetUserId:%s, targetBlockId:%s, queryHistory:%s",
                                    sessionUserId, conversationType, targetUserId, targetBlockId, queryHistory);
                        }
                    } else {
                        // 检查整体 message start 是否到达了 conversation message start
                        final long conversationRemoteMessageEnd = conversation.remoteMessageEnd.get();
                        final Message globalStartMessage = MessageDatabaseProvider.getInstance().getMinRemoteMessageId(
                                sessionUserId,
                                conversationType,
                                targetUserId
                        );
                        if (globalStartMessage == null) {
                            if (conversationRemoteMessageEnd > 0) {
                                // 本地没有消息，但是服务器有消息没有加载
                                requireLoadMoreFromRemote = true;
                            }
                        } else {
                            final long globalStartRemoteMessageId = globalStartMessage.remoteMessageId.get();
                            if (globalStartRemoteMessageId > 1) {
                                // 还有更多历史消息没有加载
                                requireLoadMoreFromRemote = true;
                            }
                        }
                    }
                } else {
                    if (targetBlockId > 0) {
                        // 检查 block end 是否到达了 conversation message end
                        final Message blockEndMessage = MessageDatabaseProvider.getInstance().getMaxRemoteMessageIdWithBlockId(
                                sessionUserId,
                                conversationType,
                                targetUserId,
                                targetBlockId
                        );
                        if (blockEndMessage != null) {
                            final long blockEndRemoteMessageId = blockEndMessage.remoteMessageId.get();
                            final long conversationRemoteMessageEnd = conversation.remoteMessageEnd.get();
                            if (blockEndRemoteMessageId < conversationRemoteMessageEnd) {
                                // 还有更多新消息没有加载
                                requireLoadMoreFromRemote = true;
                            }
                        } else {
                            // unexpected. block id 逻辑错误
                            //noinspection ConstantConditions
                            IMLog.e(new IllegalArgumentException("unexpected. block end message id null."),
                                    "sessionUserId:%s, conversationType:%s, targetUserId:%s, targetBlockId:%s, queryHistory:%s",
                                    sessionUserId, conversationType, targetUserId, targetBlockId, queryHistory);
                        }
                    } else {
                        // 检查整体 message end 是否到达了 conversation message end
                        final long conversationRemoteMessageEnd = conversation.remoteMessageEnd.get();
                        final Message globalEndMessage = MessageDatabaseProvider.getInstance().getMaxRemoteMessageId(
                                sessionUserId,
                                conversationType,
                                targetUserId
                        );
                        if (globalEndMessage == null) {
                            if (conversationRemoteMessageEnd > 0) {
                                // 本地没有消息，但是服务器有消息没有加载
                                requireLoadMoreFromRemote = true;
                            }
                        } else {
                            final long globalEndRemoteMessageId = globalEndMessage.remoteMessageId.get();
                            if (globalEndRemoteMessageId < conversationRemoteMessageEnd) {
                                // 还有更多新消息没有加载
                                requireLoadMoreFromRemote = true;
                            }
                        }
                    }
                }
                if (requireLoadMoreFromRemote) {
                    // 需要从服务器加载更多消息
                    result.hasMore = true;

                    final long sign = SignGenerator.next();
                    IMLog.v(Objects.defaultObjectTag(this) + " requireLoadMoreFromRemote start fetchWithBlockOrTimeout." +
                                    " targetBlockId:%s with sessionUserId:%s, seq:%s, limit:%s, conversationType:%s, targetUserId:%s, queryHistory:%s, sign:%s",
                            targetBlockId, sessionUserId, seq, limit, conversationType, targetUserId, queryHistory, sign);
                    final GeneralResult generalResult = fetchWithBlockOrTimeout(
                            sign,
                            sessionUserId,
                            conversationType,
                            targetUserId,
                            targetBlockId,
                            queryHistory);
                    // query again
                    IMLog.v(Objects.defaultObjectTag(this) + " requireLoadMoreFromRemote end of fetchWithBlockOrTimeout, generalResult:%s." +
                                    " targetBlockId:%s with sessionUserId:%s, seq:%s, limit:%s, conversationType:%s, targetUserId:%s, queryHistory:%s, sign:%s",
                            generalResult, targetBlockId, sessionUserId, seq, limit, conversationType, targetUserId, queryHistory, sign);
                    if (generalResult.isSuccess()) {
                        return pageQueryMessage(sessionUserId, seq, limit, conversationType, targetUserId, queryHistory);
                    } else {
                        result.generalResult = generalResult;
                    }
                }
            }
        }

        IMLog.v(Objects.defaultObjectTag(this) + " pageQueryMessage result:%s, targetBlockId:%s with sessionUserId:%s, seq:%s, limit:%s, conversationType:%s, targetUserId:%s, queryHistory:%s",
                result, targetBlockId, sessionUserId, seq, limit, conversationType, targetUserId, queryHistory);
        return result;
    }

    private GeneralResult fetchWithBlockOrTimeout(
            final long sign,
            final long sessionUserId,
            final int conversationType,
            final long targetUserId,
            final long blockId,
            final boolean history) {
        // 需要从服务器获取更多消息
        final long originSign = sign;
        final SingleSubject<GeneralResult> subject = SingleSubject.create();

        final FetchMessageHistoryObservable.FetchMessageHistoryObserver fetchMessageHistoryObserver = new FetchMessageHistoryObservable.FetchMessageHistoryObserver() {
            @Override
            public void onMessageHistoryFetchedLoading(long sign) {
                if (originSign != sign) {
                    return;
                }
                IMLog.v(Objects.defaultObjectTag(this) + " fetchWithBlockOrTimeout onMessageHistoryFetchedLoading sign:%s", sign);
            }

            @Override
            public void onMessageHistoryFetchedSuccess(long sign) {
                if (originSign != sign) {
                    return;
                }
                IMLog.v(Objects.defaultObjectTag(this) + " fetchWithBlockOrTimeout onMessageHistoryFetchedSuccess sign:%s", sign);
                subject.onSuccess(GeneralResult.success());
            }

            @Override
            public void onMessageHistoryFetchedError(long sign, long errorCode, String errorMessage) {
                if (originSign != sign) {
                    return;
                }
                IMLog.v(Objects.defaultObjectTag(this) + " fetchWithBlockOrTimeout onMessageHistoryFetchedError sign:%s, errorCode:%s, errorMessage:%s",
                        sign, errorCode, errorMessage);
                subject.onSuccess(GeneralResult.valueOfSubResult(GeneralResult.valueOf((int) errorCode, errorMessage)));
            }
        };
        final ClockObservable.ClockObserver clockObserver = new ClockObservable.ClockObserver() {

            private final long mTimeStartMs = System.currentTimeMillis();

            @Override
            public void onClock() {
                if (System.currentTimeMillis() - mTimeStartMs > TIMEOUT_MS) {
                    // 超时
                    IMLog.v(Objects.defaultObjectTag(this) + " fetchWithBlockOrTimeout onClock timeout sign:%s", originSign);
                    subject.onSuccess(GeneralResult.valueOf(GeneralResult.CODE_ERROR_TIMEOUT));
                }
            }
        };
        FetchMessageHistoryObservable.DEFAULT.registerObserver(fetchMessageHistoryObserver);
        ClockObservable.DEFAULT.registerObserver(clockObserver);
        FetchMessageHistoryManager.getInstance().enqueueFetchMessageHistory(
                sessionUserId,
                sign,
                conversationType,
                targetUserId,
                blockId,
                history);

        return subject.blockingGet();
    }

}
