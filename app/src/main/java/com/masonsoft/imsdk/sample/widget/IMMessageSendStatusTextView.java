package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.util.Objects;

import io.github.idonans.core.util.Preconditions;

public class IMMessageSendStatusTextView extends IMMessageDynamicFrameLayout {

    public IMMessageSendStatusTextView(Context context) {
        this(context, null);
    }

    public IMMessageSendStatusTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMMessageSendStatusTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IMMessageSendStatusTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private Drawable mSendFailDrawable;
    private Drawable mSendingDrawable;
    private TextView mTextView;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mSendFailDrawable = ContextCompat.getDrawable(context, R.drawable.imsdk_sample_ic_conversation_send_status_small_fail);
        Preconditions.checkNotNull(mSendFailDrawable);
        mSendFailDrawable.setBounds(0, 0, mSendFailDrawable.getIntrinsicWidth(), mSendFailDrawable.getIntrinsicHeight());
        mSendingDrawable = ContextCompat.getDrawable(context, R.drawable.imsdk_sample_ic_conversation_send_status_small_sending);
        Preconditions.checkNotNull(mSendingDrawable);
        mSendingDrawable.setBounds(0, 0, mSendingDrawable.getIntrinsicWidth(), mSendingDrawable.getIntrinsicHeight());

        mTextView = new AppCompatTextView(context);
        mTextView.setSingleLine(true);
        mTextView.setMaxLines(1);
        mTextView.setIncludeFontPadding(false);
        mTextView.setTextSize(13);
        mTextView.setTextColor(0xFF999999);
        mTextView.setGravity(Gravity.CENTER_VERTICAL);
        mTextView.setEllipsize(TextUtils.TruncateAt.END);
        addView(mTextView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onMessageChanged(@Nullable IMMessage message, @Nullable Object customObject) {
        if (DEBUG) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onMessageChanged %s", message);
        }
        if (message == null) {
            mTextView.setText(null);
        } else {
            mTextView.setText(buildStatusText(message));
        }
    }

    private CharSequence buildStatusText(@NonNull IMMessage message) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (!message.sendState.isUnset()) {
            // 消息发送状态
            switch (message.sendState.get()) {
                case IMConstants.SendStatus.IDLE:
                case IMConstants.SendStatus.SENDING:
                    SpannableString sendingSpan = new SpannableString("[sending]");
                    sendingSpan.setSpan(new AlignImageSpan(mSendingDrawable), 0, sendingSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.append(sendingSpan);
                    break;
                case IMConstants.SendStatus.FAIL:
                    SpannableString failSpan = new SpannableString("[fail]");
                    failSpan.setSpan(new AlignImageSpan(mSendFailDrawable), 0, failSpan.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    builder.append(failSpan);
                    break;
            }
        }

        final int type = message.type.getOrDefault(-1);
        String msgText;
        switch (type) {
            case IMConstants.MessageType.IMAGE:
                msgText = "[图片]";
                break;
            case IMConstants.MessageType.AUDIO:
                msgText = "[语音]";
                break;
            case IMConstants.MessageType.VIDEO:
                msgText = "[视频]";
                break;
            case IMConstants.MessageType.LOCATION:
                msgText = "[位置]";
                break;
            case IMConstants.MessageType.REAL_TIME_LOCATION:
                msgText = "[共享位置]";
                break;
            case IMConstants.MessageType.REVOKED:
                msgText = "[已撤回]";
                break;
            default:
                msgText = message.body.getOrDefault(null);
        }

        if (IMConstants.MessageType.isCustomMessage(type)) {
            msgText = "[自定义消息]";
        }

        if (msgText != null) {
            msgText = msgText.trim();
            builder.append(msgText);
        }
        return builder;
    }

}
