package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMConversationManager;
import com.masonsoft.imsdk.core.IMMessageManager;
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
        this(context, attrs, defStyleAttr, 0);
    }

    public IMMessageReadStatusView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private IMConversationChangedViewHelper mConversationChangedViewHelper;
    private TextView mReadTextView;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mConversationChangedViewHelper = new IMConversationChangedViewHelper() {
            @Nullable
            @Override
            protected IMMessage loadCustomObject() {
                final long localMessageId = getLocalMessageId();
                if (localMessageId > 0) {
                    return IMMessageManager.getInstance().getMessage(getSessionUserId(), getConversationType(), getTargetUserId(), localMessageId);
                }
                return null;
            }

            @Override
            protected void onConversationChanged(@Nullable IMConversation conversation, @Nullable Object customObject) {
                onConversationOrMessageChanged(conversation, (IMMessage) customObject);
            }
        };

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

        if (isInEditMode()) {
            mReadTextView.setText(R.string.imsdk_sample_tip_message_read);
        }
    }

    @Override
    public void setMessage(@NonNull IMMessage message) {
        super.setMessage(message);
        mConversationChangedViewHelper.setConversationByTargetUserId(
                getSessionUserId(),
                getConversationType(),
                getTargetUserId()
        );
    }

    @Override
    public void setMessage(long sessionUserId, int conversationType, long targetUserId, long localMessageId) {
        super.setMessage(sessionUserId, conversationType, targetUserId, localMessageId);
        mConversationChangedViewHelper.setConversationByTargetUserId(
                getSessionUserId(),
                getConversationType(),
                getTargetUserId()
        );
    }

    @Nullable
    @Override
    protected IMConversation loadCustomObject() {
        return IMConversationManager.getInstance().getConversationByTargetUserId(getSessionUserId(), getConversationType(), getTargetUserId());
    }

    @Override
    protected void onMessageChanged(@Nullable IMMessage message, @Nullable Object customObject) {
        onConversationOrMessageChanged((IMConversation) customObject, message);
    }

    private void onConversationOrMessageChanged(@Nullable IMConversation conversation, @Nullable IMMessage message) {
        if (DEBUG) {
            SampleLog.v("onConversationOrMessageChanged conversation:%s message:%s", conversation, message);
        }

        if (message == null || conversation == null) {
            mReadTextView.setText(null);
            return;
        }

        int messageSendStatus = IMConstants.SendStatus.SUCCESS;
        boolean read = false;
        if (!message.sendState.isUnset()) {
            messageSendStatus = message.sendState.get();
        }

        if (!conversation.messageLastRead.isUnset()) {
            read = message.id.get() <= conversation.messageLastRead.get();
        }

        if (messageSendStatus == IMConstants.SendStatus.SUCCESS) {
            if (read) {
                mReadTextView.setText(R.string.imsdk_sample_tip_message_read);
            } else {
                mReadTextView.setText(R.string.imsdk_sample_tip_message_delivered);
            }
        } else {
            mReadTextView.setText(null);
        }
    }

}
