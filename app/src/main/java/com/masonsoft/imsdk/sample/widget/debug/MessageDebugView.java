package com.masonsoft.imsdk.sample.widget.debug;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.MSIMMessage;
import com.masonsoft.imsdk.sample.widget.IMMessageChangedViewHelper;

public class MessageDebugView extends DebugTextView {

    public MessageDebugView(@NonNull Context context) {
        this(context, null);
    }

    public MessageDebugView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MessageDebugView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MessageDebugView(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private IMMessageChangedViewHelper mMessageChangedViewHelper;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mMessageChangedViewHelper = new IMMessageChangedViewHelper() {
            @Override
            protected void onMessageChanged(@Nullable MSIMMessage message, @Nullable Object customObject) {
                MessageDebugView.this.showMessageDebugInfo(message);
            }
        };
    }

    public void setMessage(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        mMessageChangedViewHelper.setMessage(sessionUserId, conversationType, targetUserId, localMessageId);
    }

    private void showMessageDebugInfo(@Nullable MSIMMessage message) {
        if (message == null) {
            setText(mMessageChangedViewHelper.getDebugString());
            return;
        }

        setText(message.toString());
    }

}
