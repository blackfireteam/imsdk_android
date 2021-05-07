package com.masonsoft.imsdk.sample.widget.debug;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.sample.widget.IMMessageChangedViewHelper;

public class MessageDebugView extends DebugTextView {

    public MessageDebugView(@NonNull Context context) {
        super(context);
        initFromAttributes(context, null, 0, 0);
    }

    public MessageDebugView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs, 0, 0);
    }

    public MessageDebugView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    private IMMessageChangedViewHelper mMessageChangedViewHelper;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mMessageChangedViewHelper = new IMMessageChangedViewHelper() {
            @Override
            protected void onMessageChanged(@Nullable IMMessage message, @Nullable Object customObject) {
                MessageDebugView.this.showMessageDebugInfo(message);
            }
        };
    }

    public void setMessage(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        mMessageChangedViewHelper.setMessage(sessionUserId, conversationType, targetUserId, localMessageId);
    }

    private void showMessageDebugInfo(@Nullable IMMessage message) {
        if (message == null) {
            setText(mMessageChangedViewHelper.getDebugString());
            return;
        }

        setText(message.toShortString());
    }

}
