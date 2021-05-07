package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;

import io.github.idonans.lang.util.ViewUtil;

public class IMMessageSendStatusView extends IMMessageDynamicFrameLayout {

    public IMMessageSendStatusView(Context context) {
        this(context, null);
    }

    public IMMessageSendStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMMessageSendStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public IMMessageSendStatusView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private ImageView mSendFailView;
    private ProgressBar mSendingView;

    private long mMessageSendTimeMs = 0L;
    private int mMessageSendStatus = -1;

    @Nullable
    private IMMessage mMessageUnsafe;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setWillNotDraw(false);

        {
            mSendFailView = new ImageView(context);
            mSendFailView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            mSendFailView.setImageResource(R.drawable.imsdk_sample_ic_message_send_fail);
            LayoutParams layoutParams = generateDefaultLayoutParams();
            layoutParams.width = LayoutParams.WRAP_CONTENT;
            layoutParams.height = LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;
            addView(mSendFailView, layoutParams);
        }

        {
            mSendingView = new ProgressBar(context);
            mSendingView.setIndeterminate(true);
            LayoutParams layoutParams = generateDefaultLayoutParams();
            layoutParams.width = LayoutParams.WRAP_CONTENT;
            layoutParams.height = LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;
            addView(mSendingView, layoutParams);
        }

        ViewUtil.setVisibilityIfChanged(mSendFailView, View.GONE);
        ViewUtil.setVisibilityIfChanged(mSendingView, View.GONE);

        ViewUtil.onClick(this, v -> {
            final IMMessage message = mMessageUnsafe;
            if (message != null
                    && !message.sendState.isUnset()) {
                if (message.sendState.get() == IMConstants.SendStatus.FAIL) {
                    IMMessageQueueManager.getInstance().enqueueResendSessionMessage(message);
                }
            }
        });
    }

    @Override
    protected void onMessageChanged(@Nullable IMMessage message, @Nullable Object customObject) {
        if (DEBUG) {
            SampleLog.v("onMessageChanged %s", message);
        }
        mMessageUnsafe = message;
        if (message == null) {
            mMessageSendStatus = -1;
            mMessageSendTimeMs = 0L;
        } else {
            mMessageSendTimeMs = message.timeMs.getOrDefault(0L);
            if (message.sendState.isUnset()) {
                mMessageSendStatus = -1;
            } else {
                mMessageSendStatus = message.sendState.get();
            }
        }

        syncState();
    }

    private void syncState() {
        if (mMessageSendStatus == IMConstants.SendStatus.IDLE
                || mMessageSendStatus == IMConstants.SendStatus.SENDING) {
            // 发送中
            final long delayTimeMs = 700L;
            boolean showSending = true;
            long invalidateDelay = 0;
            if (mMessageSendTimeMs > 0) {
                final long diff = System.currentTimeMillis() - mMessageSendTimeMs;
                if (diff >= 0 && diff < delayTimeMs) {
                    showSending = false;
                    invalidateDelay = delayTimeMs - diff;
                }
            }

            ViewUtil.setVisibilityIfChanged(mSendFailView, View.GONE);
            if (showSending) {
                ViewUtil.setVisibilityIfChanged(mSendingView, View.VISIBLE);
            } else {
                ViewUtil.setVisibilityIfChanged(mSendingView, View.GONE);
                postDelayed(this::syncState, invalidateDelay);
            }
        } else if (mMessageSendStatus == IMConstants.SendStatus.FAIL) {
            // 发送失败
            ViewUtil.setVisibilityIfChanged(mSendFailView, View.VISIBLE);
            ViewUtil.setVisibilityIfChanged(mSendingView, View.GONE);
        }
    }

}
