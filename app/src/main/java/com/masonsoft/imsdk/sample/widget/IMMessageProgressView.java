package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;

import io.github.idonans.lang.util.ViewUtil;

public class IMMessageProgressView extends ProgressView {

    public IMMessageProgressView(Context context) {
        this(context, null);
    }

    public IMMessageProgressView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMMessageProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public IMMessageProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private IMMessageChangedViewHelper mMessageChangedViewHelper;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mMessageChangedViewHelper = new IMMessageChangedViewHelper() {
            @Override
            protected void onMessageChanged(@Nullable IMMessage message) {
                IMMessageProgressView.this.updateProgress(message);
            }
        };
    }

    private void updateProgress(@Nullable IMMessage message) {
        boolean showProgress = false;
        float progress = 0f;
        if (message != null) {
            if (!message.sendState.isUnset()) {
                final int sendState = message.sendState.get();
                if (sendState == IMConstants.SendStatus.IDLE
                        || sendState == IMConstants.SendStatus.SENDING) {
                    showProgress = true;
                    progress = message.sendProgress.getOrDefault(0f);
                }
            }
        }
        if (showProgress) {
            showProgress(progress);
        } else {
            hideProgress();
        }
    }

    private void showProgress(float progress) {
        ViewUtil.setVisibilityIfChanged(this, View.VISIBLE);
        setProgress(progress);
    }

    private void hideProgress() {
        ViewUtil.setVisibilityIfChanged(this, View.GONE);
    }

    public void setMessage(@NonNull IMMessage message) {
        mMessageChangedViewHelper.setMessage(message);
    }

    public void setMessage(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        mMessageChangedViewHelper.setMessage(sessionUserId, conversationType, targetUserId, localMessageId);
    }

}
