package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

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
        Threads.postBackground(() -> IMManager.getInstance().start());
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

        if (sessionUserId < 0 || conversationType < 0 || targetUserId < 0 || localMessageId < 0) {
            return null;
        }

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
        return pageQueryMessage(
                sessionUserId,
                seq,
                limit,
                conversationType,
                targetUserId,
                queryHistory,
                null
        );
    }

    @WorkerThread
    @NonNull
    private TinyPage<IMMessage> pageQueryMessage(final long sessionUserId,
                                                 final long seq,
                                                 final int limit,
                                                 final int conversationType,
                                                 final long targetUserId,
                                                 final boolean queryHistory,
                                                 @Nullable final String lastLoopInvokeCondition) {
        Threads.mustNotUi();

        final TinyPage<IMMessage> page = pageQueryMessageInternal(
                sessionUserId,
                seq,
                limit,
                conversationType,
                targetUserId,
                queryHistory,
                lastLoopInvokeCondition
        );
        final TinyPage<IMMessage> pageFilter = filter(page);
        if (pageFilter.generalResult != null) {
            // 同步远程消息时的发生异常
            return pageFilter;
        }

        if (!pageFilter.hasMore) {
            // 没有更多消息了
            return pageFilter;
        }

        int oldPageItemsSize = 0;
        if (page.items != null) {
            oldPageItemsSize = page.items.size();
        }
        final int filterItemsSize = pageFilter.items.size();
        if (filterItemsSize == oldPageItemsSize) {
            // 没有指令消息
            return pageFilter;
        }

        // 需要读取更多的非指令消息以满足当前页容量
        final long lastSeq = page.items.get(page.items.size() - 1).seq.getOrDefault(-1L);
        if (lastSeq <= 0) {
            return pageFilter;
        }
        final int nextPageLimit = oldPageItemsSize - filterItemsSize;
        if (nextPageLimit <= 0) {
            // unexpected
            return pageFilter;
        }
        final TinyPage<IMMessage> nextPage = pageQueryMessage(
                sessionUserId,
                lastSeq,
                nextPageLimit,
                conversationType,
                targetUserId,
                queryHistory,
                lastLoopInvokeCondition
        );

        // merge pageFilter nextPage
        final TinyPage<IMMessage> merge = merge(pageFilter, nextPage);
        IMLog.v("pageQueryMessage merge:%s", merge);
        return merge;
    }

    /**
     * 去除指令消息
     */
    @NonNull
    private TinyPage<IMMessage> filter(@NonNull TinyPage<IMMessage> input) {
        final TinyPage<IMMessage> page = new TinyPage<>();
        page.hasMore = input.hasMore;
        page.generalResult = input.generalResult;
        page.items = new ArrayList<>();
        if (input.items != null) {
            for (IMMessage message : input.items) {
                if (!message.type.isUnset()) {
                    final int type = message.type.get();
                    if (!IMConstants.MessageType.isActionMessage(type)) {
                        page.items.add(message);
                    }
                }
            }
        }
        return page;
    }

    /**
     * 合并分页内容
     */
    @NonNull
    private TinyPage<IMMessage> merge(@NonNull TinyPage<IMMessage> page1, @NonNull TinyPage<IMMessage> page2) {
        final TinyPage<IMMessage> page = new TinyPage<>();
        page.hasMore = page2.hasMore;
        page.generalResult = page2.generalResult;
        page.items = new ArrayList<>();
        if (page1.items != null) {
            page.items.addAll(page1.items);
        }
        if (page2.items != null) {
            page.items.addAll(page2.items);
        }
        return page;
    }

    @WorkerThread
    @NonNull
    private TinyPage<IMMessage> pageQueryMessageInternal(final long sessionUserId,
                                                         final long seq,
                                                         final int limit,
                                                         final int conversationType,
                                                         final long targetUserId,
                                                         final boolean queryHistory,
                                                         @Nullable String lastLoopInvokeCondition) {
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
                @Nullable final Message blockStartMessage;
                @Nullable final Message blockEndMessage;
                if (targetBlockId > 0) {
                    blockStartMessage = MessageDatabaseProvider.getInstance().getMinRemoteMessageIdWithBlockId(
                            sessionUserId,
                            conversationType,
                            targetUserId,
                            targetBlockId
                    );
                    blockEndMessage = MessageDatabaseProvider.getInstance().getMaxRemoteMessageIdWithBlockId(
                            sessionUserId,
                            conversationType,
                            targetUserId,
                            targetBlockId
                    );
                } else {
                    blockStartMessage = null;
                    blockEndMessage = null;
                }
                @Nullable final Message globalStartMessage = MessageDatabaseProvider.getInstance().getMinRemoteMessageId(
                        sessionUserId,
                        conversationType,
                        targetUserId
                );
                @Nullable final Message globalEndMessage = MessageDatabaseProvider.getInstance().getMaxRemoteMessageId(
                        sessionUserId,
                        conversationType,
                        targetUserId
                );
                final String loopInvokeCondition;
                {
                    final StringBuilder builder = new StringBuilder();
                    builder.append("_sessionUserId:").append(sessionUserId);
                    builder.append("_targetUserId:").append(targetUserId);
                    builder.append("_seq:").append(seq);
                    builder.append("_targetBlockId:").append(targetBlockId);
                    if (blockStartMessage != null) {
                        builder.append("_blockStartMessage:").append(blockStartMessage.localId.get());
                    }
                    if (blockEndMessage != null) {
                        builder.append("_blockEndMessage:").append(blockEndMessage.localId.get());
                    }
                    if (globalStartMessage != null) {
                        builder.append("_globalStartMessage:").append(globalStartMessage.localId.get());
                    }
                    if (globalEndMessage != null) {
                        builder.append("_globalEndMessage:").append(globalEndMessage.localId.get());
                    }
                    loopInvokeCondition = builder.toString();
                }
                if (loopInvokeCondition.equals(lastLoopInvokeCondition)) {
                    // loop
                    IMLog.v("abort loop call. loopInvokeCondition:%s", loopInvokeCondition);
                } else {
                    lastLoopInvokeCondition = loopInvokeCondition;

                    if (queryHistory) {
                        if (targetBlockId > 0) {
                            // 检查 block start 是否到达了 conversation message start
                            if (blockStartMessage != null) {
                                final long blockStartRemoteMessageId = blockStartMessage.remoteMessageId.get();
                                if (blockStartRemoteMessageId > 1) {
                                    // 还有更多历史消息没有加载
                                    requireLoadMoreFromRemote = true;
                                }
                            } else {
                                // unexpected. block id 逻辑错误
                                IMLog.e(new IllegalArgumentException("unexpected. block start message id null."),
                                        "sessionUserId:%s, conversationType:%s, targetUserId:%s, targetBlockId:%s, queryHistory:%s",
                                        sessionUserId, conversationType, targetUserId, targetBlockId, queryHistory);
                            }
                        } else {
                            // 检查整体 message start 是否到达了 conversation message start
                            final long conversationRemoteMessageEnd = conversation.remoteMessageEnd.get();
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
                            if (blockEndMessage != null) {
                                final long blockEndRemoteMessageId = blockEndMessage.remoteMessageId.get();
                                final long conversationRemoteMessageEnd = conversation.remoteMessageEnd.get();
                                if (blockEndRemoteMessageId < conversationRemoteMessageEnd) {
                                    // 还有更多新消息没有加载
                                    requireLoadMoreFromRemote = true;
                                }
                            } else {
                                // unexpected. block id 逻辑错误
                                IMLog.e(new IllegalArgumentException("unexpected. block end message id null."),
                                        "sessionUserId:%s, conversationType:%s, targetUserId:%s, targetBlockId:%s, queryHistory:%s",
                                        sessionUserId, conversationType, targetUserId, targetBlockId, queryHistory);
                            }
                        } else {
                            // 检查整体 message end 是否到达了 conversation message end
                            final long conversationRemoteMessageEnd = conversation.remoteMessageEnd.get();
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
                }

                if (requireLoadMoreFromRemote) {
                    // 需要从服务器加载更多消息
                    result.hasMore = true;

                    final long sign = SignGenerator.nextSign();
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
                        return pageQueryMessageInternal(sessionUserId, seq, limit, conversationType, targetUserId, queryHistory, lastLoopInvokeCondition);
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
            public void onMessageHistoryFetchedError(long sign, int errorCode, String errorMessage) {
                if (originSign != sign) {
                    return;
                }
                IMLog.v(Objects.defaultObjectTag(this) + " fetchWithBlockOrTimeout onMessageHistoryFetchedError sign:%s, errorCode:%s, errorMessage:%s",
                        sign, errorCode, errorMessage);
                subject.onSuccess(GeneralResult.valueOfOther(GeneralResult.valueOf(errorCode, errorMessage)));
            }
        };
        final ClockObservable.ClockObserver clockObserver = new ClockObservable.ClockObserver() {

            private final long mTimeStartMs = System.currentTimeMillis();

            @Override
            public void onClock() {
                if (System.currentTimeMillis() - mTimeStartMs > TIMEOUT_MS) {
                    // 超时
                    IMLog.v(Objects.defaultObjectTag(this) + " fetchWithBlockOrTimeout onClock timeout sign:%s", originSign);
                    subject.onSuccess(GeneralResult.valueOf(GeneralResult.ERROR_CODE_TIMEOUT));
                    ClockObservable.DEFAULT.unregisterObserver(this);
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
