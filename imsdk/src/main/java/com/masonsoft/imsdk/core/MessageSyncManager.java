package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;

import com.idonans.core.Singleton;
import com.idonans.core.thread.TaskQueue;
import com.masonsoft.imsdk.core.db.Conversation;
import com.masonsoft.imsdk.core.db.ConversationDatabaseProvider;
import com.masonsoft.imsdk.core.db.Message;
import com.masonsoft.imsdk.core.db.MessageDatabaseProvider;
import com.masonsoft.imsdk.core.message.ProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.message.packet.MessagePacket;
import com.masonsoft.imsdk.core.message.packet.NotNullTimeoutMessagePacket;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.lang.SafetyRunnable;
import com.masonsoft.imsdk.util.Objects;

/**
 * 处理消息同步
 */
public class MessageSyncManager {

    private static final Singleton<MessageSyncManager> INSTANCE = new Singleton<MessageSyncManager>() {
        @Override
        protected MessageSyncManager create() {
            return new MessageSyncManager();
        }
    };

    public static MessageSyncManager getInstance() {
        return INSTANCE.get();
    }

    private final TaskQueue mSyncQueue = new TaskQueue(1);

    private MessageSyncManager() {
    }

    /**
     * 如果目标会话的有新消息，加载最后一页的消息
     *
     * @param sessionUserId
     * @param conversationId
     */
    public void enqueueLoadLatestMessages(final long sessionUserId,
                                          final long conversationId) {
        mSyncQueue.enqueue(new SafetyRunnable(new SyncLatestMessagesTask(sessionUserId, conversationId)));
    }

    private static class SyncLatestMessagesTask implements Runnable {

        private final long mSessionUserId;
        private final long mConversationId;

        private SyncLatestMessagesTask(long sessionUserId, long conversationId) {
            mSessionUserId = sessionUserId;
            mConversationId = conversationId;
        }

        private boolean isSessionChanged() {
            return this.mSessionUserId != IMSessionManager.getInstance().getSessionUserId();
        }

        private void checkValid() {
            if (mSessionUserId <= 0) {
                throw new IllegalStateException("mSessionUserId:" + this.mSessionUserId + " is invalid");
            }

            if (isSessionChanged()) {
                throw new IllegalStateException("session is changed mSessionUserId:"
                        + mSessionUserId
                        + ", manager sessionUserId:"
                        + IMSessionManager.getInstance().getSessionUserId());
            }
        }

        @Override
        public void run() {
            checkValid();

            final Conversation conversation = ConversationDatabaseProvider.getInstance().getConversation(mSessionUserId, mConversationId);
            if (conversation == null) {
                IMLog.e(Objects.defaultObjectTag(this) + " ignore. conversation is null. mSessionUserId:%s, mConversationId:%s", mSessionUserId, mConversationId);
                return;
            }

            final long remoteShowMessageId = conversation.remoteShowMessageId.get();
            if (remoteShowMessageId <= 0) {
                IMLog.v(Objects.defaultObjectTag(this) + " ignore. remoteShowMessageId:" + remoteShowMessageId);
                return;
            }

            final Message message = MessageDatabaseProvider.getInstance().getMessageWithRemoteMessageId(
                    mSessionUserId,
                    conversation.localConversationType.get(),
                    conversation.targetUserId.get(),
                    remoteShowMessageId
            );
            if (message == null) {
                // remote show message id 还没有加载到本地
                sendMessagePacket(conversation, 0L, 0L);
                return;
            }

            final long blockId = message.localBlockId.get();
            final Message max = MessageDatabaseProvider.getInstance().getMaxRemoteMessageIdWithBlockId(
                    mSessionUserId,
                    conversation.localConversationType.get(),
                    conversation.targetUserId.get(),
                    blockId
            );
            if (max == null) {
                final Throwable e = new IllegalAccessError(Objects.defaultObjectTag(this) + " unexpected. max is null");
                IMLog.e(e);
                return;
            }
            final long maxRemoteMessageId = max.remoteMessageId.get();
            if (maxRemoteMessageId < conversation.remoteMessageEnd.get()) {
                // remote show message id 在本地，但是 remote show message id 与 remote message end 之间还有数据没有加载到本地
                sendMessagePacket(conversation, blockId, maxRemoteMessageId);
                return;
            }

            IMLog.v(Objects.defaultObjectTag(this) + " ignore. all latest message is load to local. mSessionUserId:%s, mConversationId:%s", mSessionUserId, mConversationId);
        }

        private void sendMessagePacket(@NonNull final Conversation conversation, long blockId, long maxRemoteMessageId) {
            final IMSessionManager.SessionTcpClientProxy proxy = IMSessionManager.getInstance().getSessionTcpClientProxy();
            if (proxy == null) {
                throw new IllegalStateException("proxy is null");
            }
            if (!proxy.isOnline()) {
                throw new IllegalStateException("proxy is not online");
            }
            checkValid();

            final GetHistoryMessagePacket messagePacket = GetHistoryMessagePacket.create(conversation, blockId, maxRemoteMessageId);
            proxy.getSessionTcpClient().sendMessagePacketQuietly(messagePacket);
            if (messagePacket.getState() != MessagePacket.STATE_WAIT_RESULT) {
                final Throwable e = new IllegalStateException("unexpected GetHistoryMessagePacket state " + MessagePacket.stateToString(messagePacket.getState()));
                IMLog.e(e);
            }
        }
    }

    private static class GetHistoryMessagePacket extends NotNullTimeoutMessagePacket {

        public GetHistoryMessagePacket(ProtoByteMessage protoByteMessage, long sign) {
            super(protoByteMessage, sign);
        }

        @Override
        protected boolean doNotNullProcess(@NonNull ProtoByteMessageWrapper target) {
            return false;
        }

        public static GetHistoryMessagePacket create(@NonNull final Conversation conversation, long blockId, long maxRemoteMessageId) {
            final long sign = SignGenerator.next();
            return new GetHistoryMessagePacket(
                    ProtoByteMessage.Type.encode(
                            ProtoMessage.GetHistory.newBuilder()
                                    .setSign(sign)
                                    .setToUid(conversation.targetUserId.get())
                                    .setMsgStart(maxRemoteMessageId)
                                    .build()),
                    sign
            );
        }
    }

}
