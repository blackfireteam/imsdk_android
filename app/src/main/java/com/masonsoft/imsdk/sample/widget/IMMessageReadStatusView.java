package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;

public class IMMessageReadStatusView extends IMMessageDynamicFrameLayout {

    public IMMessageReadStatusView(Context context) {
        this(context, null);
    }

    public IMMessageReadStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMMessageReadStatusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public IMMessageReadStatusView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private TextView mReadTextView;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setWillNotDraw(false);
        {
            mReadTextView = new AppCompatTextView(context);
            mReadTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            mReadTextView.setTextColor(0xff666666);
            mReadTextView.setIncludeFontPadding(false);
            LayoutParams layoutParams = generateDefaultLayoutParams();
            layoutParams.width = LayoutParams.WRAP_CONTENT;
            layoutParams.height = LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.CENTER;
            addView(mReadTextView, layoutParams);
        }
    }

    @Override
    protected void onMessageChanged(@Nullable IMMessage message, @Nullable Object customObject) {
        if (DEBUG) {
            SampleLog.v("onMessageChanged %s", message);
        }

        if (message == null) {
            mReadTextView.setText(null);
            return;
        }

        int messageSendStatus = IMConstants.SendStatus.SUCCESS;
        if (!message.sendState.isUnset()) {
            messageSendStatus = message.sendState.get();
        }

        if (messageSendStatus == IMConstants.SendStatus.SUCCESS) {
            mReadTextView.setText(R.string.imsdk_sample_tip_message_delivered);
        } else {
            mReadTextView.setText(null);
        }
    }

}
