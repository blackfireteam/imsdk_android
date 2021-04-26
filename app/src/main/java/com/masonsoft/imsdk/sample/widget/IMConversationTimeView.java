package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.util.FormatUtil;

public class IMConversationTimeView extends IMConversationDynamicFrameLayout {

    private final boolean DEBUG = Constants.DEBUG_WIDGET;

    public IMConversationTimeView(Context context) {
        this(context, null);
    }

    public IMConversationTimeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMConversationTimeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public IMConversationTimeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private TextView mTimeTextView;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mTimeTextView = new AppCompatTextView(context);
        mTimeTextView.setSingleLine(true);
        mTimeTextView.setMaxLines(1);
        mTimeTextView.setIncludeFontPadding(false);
        mTimeTextView.setTextSize(12);
        mTimeTextView.setTextColor(0xFF999999);
        mTimeTextView.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams params = generateDefaultLayoutParams();
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
        mTimeTextView.setLayoutParams(params);
        addView(mTimeTextView);
    }

    @Override
    protected void onConversationChanged(@Nullable IMConversation conversation, @Nullable Object customObject) {
        if (conversation == null) {
            setTime(null);
        } else {
            setTime(buildConversationTime(conversation));
        }
    }

    private String buildConversationTime(@NonNull IMConversation conversation) {
        // 用会话的更新时间
        final long timeMs = conversation.timeMs.getOrDefault(0L);
        if (timeMs > 0) {
            return FormatUtil.getHumanTimeDistance(timeMs, new FormatUtil.DefaultShortDateFormatOptions());
        }

        return null;
    }

    public void setTime(String time) {
        mTimeTextView.setText(time);
    }

}
