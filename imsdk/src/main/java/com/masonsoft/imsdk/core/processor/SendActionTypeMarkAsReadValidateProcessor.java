package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMActionMessage;
import com.masonsoft.imsdk.core.IMActionMessageManager;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.db.Conversation;
import com.masonsoft.imsdk.core.db.ConversationDatabaseProvider;
import com.masonsoft.imsdk.lang.GeneralResult;

/**
 * 回执消息已读
 *
 * @since 1.0
 */
public class SendActionTypeMarkAsReadValidateProcessor extends SendActionTypeValidateProcessor {

    public SendActionTypeMarkAsReadValidateProcessor() {
        super(IMActionMessage.ACTION_TYPE_MARK_AS_READ);
    }

    @Override
    protected boolean doActionTypeProcess(@NonNull IMActionMessage target, int actionType) {
        final Object actionObject = target.getActionObject();
        if (!(actionObject instanceof Long)) {
            return false;
        }

        final long targetUserId = (long) actionObject;
        if (targetUserId <= 0) {
            target.getEnqueueCallback().onCallback(
                    GeneralResult.valueOf(GeneralResult.ERROR_CODE_INVALID_TO_USER_ID)
                            .withPayload(target)
            );
            return true;
        }

        {
            // 手动清空本地未读消息数
            final Conversation conversation = ConversationDatabaseProvider.getInstance().getConversationByTargetUserId(target.getSessionUserId(), IMConstants.ConversationType.C2C, targetUserId);
            if (conversation != null) {
                final Conversation conversationUpdate = new Conversation();
                conversationUpdate.localId.set(conversation.localId.get());
                conversationUpdate.localUnreadCount.set(0L);
                ConversationDatabaseProvider.getInstance().updateConversation(target.getSessionUserId(), conversationUpdate);
            }
        }

        // 提示成功入队
        target.getEnqueueCallback().onCallback(GeneralResult.success().withPayload(target));

        // 派发到指令发送队列
        IMActionMessageManager.getInstance().enqueueActionMessage(
                target.getSign(),
                target);
        return true;
    }

}
