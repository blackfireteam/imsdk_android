package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.sample.Constants;

import io.github.idonans.lang.util.ViewUtil;

public class IMConversationLastMessage extends IMConversationDynamicFrameLayout {

    private final boolean DEBUG = Constants.DEBUG_WIDGET;

    public IMConversationLastMessage(Context context) {
        this(context, null);
    }

    public IMConversationLastMessage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMConversationLastMessage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public IMConversationLastMessage(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private IMMessageSendStatusTextView mLastMessageView;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mLastMessageView = new IMMessageSendStatusTextView(context);
        addView(mLastMessageView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onConversationUpdate(@Nullable IMConversation conversation) {
        if (conversation == null) {
            ViewUtil.setVisibilityIfChanged(mLastMessageView, View.GONE);
        } else {
            ViewUtil.setVisibilityIfChanged(mLastMessageView, View.VISIBLE);
            mLastMessageView.setMessage(
                    conversation._sessionUserId.get(),
                    conversation.type.get(),
                    conversation.targetUserId.get(),
                    conversation.showMessageId.getOrDefault(0L)
            );
        }
    }

}
