package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.RuntimeMode;
import com.masonsoft.imsdk.core.message.SessionProtoByteMessageWrapper;
import com.masonsoft.imsdk.core.proto.ProtoMessage;
import com.masonsoft.imsdk.core.session.SessionTcpClient;
import com.masonsoft.imsdk.util.Objects;
import com.masonsoft.imsdk.util.TimeDiffDebugHelper;

import java.util.ArrayList;
import java.util.List;

import io.github.idonans.core.thread.BatchQueue;
import io.github.idonans.core.util.Preconditions;

/**
 * 收到一条新的聊天消息
 *
 * @since 1.0
 */
public class ReceivedProtoMessageProtoTypeChatRProcessor extends ReceivedProtoMessageProtoTypeProcessor<ProtoMessage.ChatR> {

    private final TimeDiffDebugHelper mTimeDiffDebugHelper = new TimeDiffDebugHelper(Objects.defaultObjectTag(this));
    private final BatchQueue<SessionProtoByteMessageWrapper> mBatchQueue = new BatchQueue<>();

    public ReceivedProtoMessageProtoTypeChatRProcessor() {
        super(ProtoMessage.ChatR.class);

        mBatchQueue.setConsumer(payloadList -> {
            if (payloadList == null || payloadList.isEmpty()) {
                return;
            }

            // 如果连续的 SessionTcpClient 与 session user id 相同，则合并处理
            SessionTcpClient currentSessionTcpClient = null;
            long currentSessionUserId = -1;
            List<SessionProtoByteMessageWrapper> currentPayloadList = null;
            for (SessionProtoByteMessageWrapper payload : payloadList) {
                if (currentSessionTcpClient == payload.getSessionTcpClient()
                        && currentSessionUserId == payload.getSessionUserId()) {
                    Preconditions.checkNotNull(currentPayloadList);
                } else {
                    if (currentPayloadList != null) {
                        processBatch(currentPayloadList);
                    }

                    currentSessionTcpClient = payload.getSessionTcpClient();
                    currentSessionUserId = payload.getSessionUserId();
                    currentPayloadList = new ArrayList<>();
                }
                currentPayloadList.add(payload);
            }

            Preconditions.checkNotNull(currentPayloadList);
            processBatch(currentPayloadList);
        });
    }

    private void processBatch(@NonNull List<SessionProtoByteMessageWrapper> targetList) {
        mTimeDiffDebugHelper.mark();
        // 批量处理
        try {
            final TinyChatRNewMessageListProcessor processor = new TinyChatRNewMessageListProcessor();
            processor.doProcess(targetList);
        } catch (Throwable e) {
            IMLog.e(e);
            RuntimeMode.fixme(e);
        }
        mTimeDiffDebugHelper.mark();
        mTimeDiffDebugHelper.print("batch size:" + targetList.size());
    }

    @Override
    protected boolean doNotNullProtoMessageObjectProcess(
            @NonNull SessionProtoByteMessageWrapper target,
            @NonNull ProtoMessage.ChatR protoMessageObject) {
        mBatchQueue.add(target);
        return true;
    }

}
