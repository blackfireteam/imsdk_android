package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.user.UserInfo;

public class IMMessageRevokeTextView extends UserCacheDynamicTextView {

    public IMMessageRevokeTextView(Context context) {
        this(context, null);
    }

    public IMMessageRevokeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMMessageRevokeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    // 标记是接收的消息还是发送的消息
    private boolean mReceived;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IMMessageRevokeTextView, defStyleAttr,
                defStyleRes);
        mReceived = a.getBoolean(R.styleable.IMMessageRevokeTextView_received, mReceived);
        a.recycle();
    }

    private String buildRecallText(@Nullable UserInfo userInfo) {
        String username = null;
        if (userInfo != null) {
            username = userInfo.nickname.getOrDefault(null);
        }
        if (username == null) {
            username = "";
        }
        if (mReceived) {
            return I18nResources.getString(R.string.imsdk_sample_recall_received_message, username);
        } else {
            return I18nResources.getString(R.string.imsdk_sample_recall_send_message);
        }
    }

    @Override
    protected void onUserCacheUpdate(@Nullable UserInfo userInfo) {
        setText(buildRecallText(userInfo));
    }

}
