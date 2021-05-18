package com.masonsoft.imsdk.core.processor;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.EnqueueCallback;
import com.masonsoft.imsdk.core.IMActionMessage;
import com.masonsoft.imsdk.core.IMConversation;
import com.masonsoft.imsdk.R;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.IMActionMessageManager;
import com.masonsoft.imsdk.core.db.Conversation;
import com.masonsoft.imsdk.core.db.ConversationDatabaseProvider;

/**
 * 删除一个会话
 *
 * @since 1.0
 */
public class SendActionTypeDeleteConversationValidateProcessor extends SendActionTypeValidateProcessor {

    public SendActionTypeDeleteConversationValidateProcessor() {
        super(IMActionMessage.ACTION_TYPE_DELETE_CONVERSATION);
    }

    @Override
    protected boolean doActionTypeProcess(@NonNull IMActionMessage target, int actionType) {
        final Object actionObject = target.getActionObject();
        if (!(actionObject instanceof IMConversation)) {
            return false;
        }

        final IMConversation conversation = (IMConversation) actionObject;
        if (conversation.id.isUnset()
                || conversation.id.get() == null
                || conversation.id.get() <= 0) {
            // 会话没有入库，不支持删除
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    EnqueueCallback.ERROR_CODE_INVALID_CONVERSATION_ID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_conversation_id));
            return true;
        }

        final Conversation dbConversation = ConversationDatabaseProvider.getInstance().getConversation(
                conversation._sessionUserId.get(),
                conversation.id.get());
        if (dbConversation == null) {
            // 会话没有找到
            target.getEnqueueCallback().onEnqueueFail(
                    target,
                    EnqueueCallback.ERROR_CODE_INVALID_CONVERSATION_ID,
                    I18nResources.getString(R.string.msimsdk_enqueue_callback_error_invalid_conversation_id));
            return true;
        }

        // 派发到指令发送队列
        IMActionMessageManager.getInstance().enqueueActionMessage(
                target.getSign(),
                target);
        return true;
    }

}
