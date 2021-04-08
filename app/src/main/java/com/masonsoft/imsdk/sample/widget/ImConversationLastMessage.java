package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.lang.util.ViewUtil;
import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.sample.Constants;

public class ImConversationLastMessage extends IMConversationDynamicFrameLayout {

    private final boolean DEBUG = Constants.DEBUG_WIDGET;

    public ImConversationLastMessage(Context context) {
        this(context, null);
    }

    public ImConversationLastMessage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImConversationLastMessage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public ImConversationLastMessage(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private IMMessageSendStatusTextView mLastMessageView;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mLastMessageView = new IMMessageSendStatusTextView(context);
        addView(mLastMessageView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onConversationUpdate(@Nullable IMConversation imConversation) {
        if (imConversation == null) {
            ViewUtil.setVisibilityIfChanged(mLastMessageView, View.GONE);
        } else {
            ViewUtil.setVisibilityIfChanged(mLastMessageView, View.VISIBLE);
            mLastMessageView.setMessage(
                    imConversation._sessionUserId.get(),
                    imConversation.type.get(),
                    imConversation.targetUserId.get(),
                    imConversation.showMessageId.getOrDefault(0L)
            );
        }
    }

}
