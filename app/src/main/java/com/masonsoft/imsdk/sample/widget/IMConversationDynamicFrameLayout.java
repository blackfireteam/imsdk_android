package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMConversation;

public abstract class IMConversationDynamicFrameLayout extends FrameLayout {

    private final boolean DEBUG = true;

    public IMConversationDynamicFrameLayout(Context context) {
        this(context, null);
    }

    public IMConversationDynamicFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMConversationDynamicFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public IMConversationDynamicFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private IMConversationChangedViewHelper mConversationChangedViewHelper;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mConversationChangedViewHelper = new IMConversationChangedViewHelper() {
            @Override
            protected void onConversationChanged(@Nullable IMConversation conversation) {
                IMConversationDynamicFrameLayout.this.onConversationUpdate(conversation);
            }
        };
    }

    public void setConversation(long sessionUserId, long conversationId) {
        mConversationChangedViewHelper.setConversation(sessionUserId, conversationId);
    }

    public long getSessionUserId() {
        return mConversationChangedViewHelper.getSessionUserId();
    }

    public long getConversationId() {
        return mConversationChangedViewHelper.getConversationId();
    }

    protected abstract void onConversationUpdate(@Nullable IMConversation conversation);

}
