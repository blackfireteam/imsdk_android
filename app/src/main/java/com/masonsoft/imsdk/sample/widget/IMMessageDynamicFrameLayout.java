package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.db.Message;

public abstract class IMMessageDynamicFrameLayout extends FrameLayout {

    protected final boolean DEBUG = false;

    private MessageChangedViewHelper mMessageChangedViewHelper;

    public IMMessageDynamicFrameLayout(Context context) {
        this(context, null);
    }

    public IMMessageDynamicFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMMessageDynamicFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public IMMessageDynamicFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mMessageChangedViewHelper = new MessageChangedViewHelper() {
            @Override
            protected void onMessageChanged(@Nullable Message message) {
                IMMessageDynamicFrameLayout.this.onMessageUpdate(message);
            }
        };
    }

    public void setMessage(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        mMessageChangedViewHelper.setMessage(sessionUserId, conversationType, targetUserId, localMessageId);
    }

    public long getSessionUserId() {
        return mMessageChangedViewHelper.getSessionUserId();
    }

    public int getConversationType() {
        return mMessageChangedViewHelper.getConversationType();
    }

    public long getTargetUserId() {
        return mMessageChangedViewHelper.getTargetUserId();
    }

    public long getLocalMessageId() {
        return mMessageChangedViewHelper.getLocalMessageId();
    }

    protected abstract void onMessageUpdate(@Nullable Message message);

}
