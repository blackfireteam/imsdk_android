package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.sample.SampleLog;

public abstract class IMMessageDynamicFrameLayout extends FrameLayout {

    protected final boolean DEBUG = true;

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

    protected IMMessageChangedViewHelper mMessageChangedViewHelper;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mMessageChangedViewHelper = new IMMessageChangedViewHelper() {
            @Override
            protected void onMessageChanged(@Nullable IMMessage message) {
                IMMessageDynamicFrameLayout.this.onMessageUpdate(message);
            }
        };
    }

    public void setMessage(@NonNull IMMessage imMessage) {
        if (imMessage._sessionUserId.isUnset()) {
            SampleLog.e(new IllegalArgumentException("unexpected imMessage._sessionUserId.isUnset()"));
            return;
        }
        if (imMessage._conversationType.isUnset()) {
            SampleLog.e(new IllegalArgumentException("unexpected imMessage._conversationType.isUnset()"));
            return;
        }
        if (imMessage._targetUserId.isUnset()) {
            SampleLog.e(new IllegalArgumentException("unexpected imMessage._targetUserId.isUnset()"));
            return;
        }
        if (imMessage.id.isUnset()) {
            SampleLog.e(new IllegalArgumentException("unexpected imMessage.id.isUnset()"));
            return;
        }
        setMessage(imMessage._sessionUserId.get(),
                imMessage._conversationType.get(),
                imMessage._targetUserId.get(),
                imMessage.id.get());
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

    protected abstract void onMessageUpdate(@Nullable IMMessage imMessage);

}
