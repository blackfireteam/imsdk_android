package com.masonsoft.imsdk.sample.widget.debug;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.MSIMConversation;
import com.masonsoft.imsdk.sample.widget.MSIMConversationChangedViewHelper;

public class ConversationDebugView extends DebugTextView {

    public ConversationDebugView(@NonNull Context context) {
        this(context, null);
    }

    public ConversationDebugView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConversationDebugView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ConversationDebugView(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private MSIMConversationChangedViewHelper mConversationChangedViewHelper;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mConversationChangedViewHelper = new MSIMConversationChangedViewHelper() {
            @Override
            protected void onConversationChanged(@Nullable MSIMConversation conversation, @Nullable Object customObject) {
                ConversationDebugView.this.showConversationDebugInfo(conversation);
            }
        };
    }

    public void setConversation(long sessionUserId, long conversationId) {
        mConversationChangedViewHelper.setConversation(sessionUserId, conversationId);
    }

    private void showConversationDebugInfo(@Nullable MSIMConversation conversation) {
        if (conversation == null) {
            setText(mConversationChangedViewHelper.getDebugString());
            return;
        }

        setText(conversation.toString());
    }

}
