package com.masonsoft.imsdk.sample.widget.debug;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.sample.widget.IMConversationChangedViewHelper;

public class ConversationDebugView extends AppCompatTextView {

    public ConversationDebugView(@NonNull Context context) {
        super(context);
        initFromAttributes(context, null, 0, 0);
    }

    public ConversationDebugView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs, 0, 0);
    }

    public ConversationDebugView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    private IMConversationChangedViewHelper mConversationChangedViewHelper;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mConversationChangedViewHelper = new IMConversationChangedViewHelper() {
            @Override
            protected void onConversationChanged(@Nullable IMConversation conversation) {
                ConversationDebugView.this.showConversationDebugInfo(conversation);
            }
        };

        setTextSize(12);
        setIncludeFontPadding(false);
        setTextColor(0x60000000);
    }

    public void setConversation(long sessionUserId, long conversationId) {
        mConversationChangedViewHelper.setConversation(sessionUserId, conversationId);
    }

    private void showConversationDebugInfo(@Nullable IMConversation conversation) {
        if (conversation == null) {
            setText("conversation is null");
            return;
        }

        setText(buildConversationDebugInfo(conversation));
    }

    private static String buildConversationDebugInfo(@NonNull IMConversation conversation) {
        final StringBuilder builder = new StringBuilder();
        if (conversation.id.isUnset()) {
            builder.append(" id:unset");
        } else {
            builder.append(" id:").append(conversation.id.get());
        }
        if (conversation.seq.isUnset()) {
            builder.append(" seq:unset");
        } else {
            builder.append(" seq:").append(conversation.seq.get());
        }
        if (conversation.lastModifyMs.isUnset()) {
            builder.append(" lastModifyMs:unset");
        } else {
            builder.append(" lastModifyMs:").append(conversation.lastModifyMs.get());
        }
        if (conversation.type.isUnset()) {
            builder.append(" type:unset");
        } else {
            builder.append(" type:").append(conversation.type.get());
        }
        return builder.toString();
    }

}
