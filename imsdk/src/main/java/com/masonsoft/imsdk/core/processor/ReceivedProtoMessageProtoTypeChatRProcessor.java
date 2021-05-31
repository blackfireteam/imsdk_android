package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.util.Objects;
import com.masonsoft.imsdk.util.TimeDiffDebugHelper;

import java.util.ArrayList;
import java.util.List;

import io.github.idonans.core.thread.TaskQueue;

/**
 * 收到一条新的聊天消息
 *
 * @since 1.0
 */
public class ReceivedProtoMessageProtoTypeChatRProcessor extends ReceivedProtoMessageProtoTypeProcessor<ProtoMessage.ChatR> {

    private final TimeDiffDebugHelper mTimeDiffDebugHelper = new TimeDiffDebugHelper(Objects.defaultObjectTag(this));
    private final Object mBatchLock = new Object();
    private Batch mCurrentBatch;

    public ReceivedProtoMessageProtoTypeChatRProcessor() {
        super(ProtoMessage.ChatR.class);
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(
            @NonNull SessionProtoByteMessageWrapper target,
            @NonNull ProtoMessage.ChatR protoMessageObject) {
        mTimeDiffDebugHelper.mark();
        synchronized (mBatchLock) {
            if (mCurrentBatch != null) {
                if (mCurrentBatch.merge(target)) {
                    mCurrentBatch.doNextBatch();
                    return true;
                } else {
                    mCurrentBatch.join();
                    mCurrentBatch = null;
                }
            }
            if (mCurrentBatch == null) {
                mCurrentBatch = new Batch(target);
                mCurrentBatch.doNextBatch();
            }
        }
        mTimeDiffDebugHelper.mark();
        mTimeDiffDebugHelper.print();
        return true;
    }

    private static final class Batch {

        private final TimeDiffDebugHelper mTimeDiffDebugHelper = new TimeDiffDebugHelper(Objects.defaultObjectTag(this));

        @NonNull
        private final SessionTcpClient mSessionTcpClient;
        private final long mSessionUserId;

        private final Object mJoinLock = new Object();
        private boolean mJoined;
        private final Object mEndLock = new Object();
        private boolean mEnd;
        private final Object mTargetListLock = new Object();
        private List<SessionProtoByteMessageWrapper> mTargetList = new ArrayList<>();
        private final TaskQueue mBatchQueue = new TaskQueue(1);

        private Batch(@NonNull SessionProtoByteMessageWrapper target) {
            mSessionTcpClient = target.getSessionTcpClient();
            mSessionUserId = target.getSessionUserId();
            mTargetList.add(target);
        }

        private boolean merge(@NonNull SessionProtoByteMessageWrapper target) {
            synchronized (mJoinLock) {
                if (!mJoined) {
                    if (mSessionTcpClient == target.getSessionTcpClient()
                            && mSessionUserId == target.getSessionUserId()) {
                        synchronized (mTargetListLock) {
                            mTargetList.add(target);
                        }
                        return true;
                    }
                    return false;
                }
            }
            return false;
        }

        private void doNextBatch() {
            mBatchQueue.skipQueue();
            mBatchQueue.enqueue(() -> {
                final List<SessionProtoByteMessageWrapper> targetList;
                synchronized (mTargetListLock) {
                    targetList = mTargetList;
                    mTargetList = new ArrayList<>();
                }
                final boolean noMoreTarget = targetList == null || targetList.isEmpty();
                synchronized (mJoinLock) {
                    if (mJoined && noMoreTarget) {
                        synchronized (mEndLock) {
                            mEnd = true;
                            mEndLock.notify();
                        }
                    }
                }
                if (noMoreTarget) {
                    return;
                }

                mTimeDiffDebugHelper.mark();
                // 批量处理
                try {
                    final TinyChatRNewMessageListProcessor processor = new TinyChatRNewMessageListProcessor();
                    processor.doProcess(targetList);
                } catch (Throwable e) {
                    IMLog.e(e);
                }
                mTimeDiffDebugHelper.mark();
                mTimeDiffDebugHelper.print("batch size:" + targetList.size());
            });
        }

        private void join() {
            synchronized (mJoinLock) {
                mJoined = true;
            }
            while (true) {
                synchronized (mEndLock) {
                    if (mEnd) {
                        return;
                    }
                    try {
                        mEndLock.wait(1000L);
                    } catch (Throwable e) {
                        // ignore
                    }
                }
            }
        }

    }

}
