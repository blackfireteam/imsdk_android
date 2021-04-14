package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;

import io.github.idonans.lang.util.ViewUtil;

/**
 * 如果消息不存在，则都不可见。<br>
 * 如果消息已撤回，则第一个 child 可见，否则第二个 child 可见。
 *
 * @since 1.0
 */
public class IMMessageRevokeStateFrameLayout extends IMMessageDynamicFrameLayout {

    protected final boolean DEBUG = Constants.DEBUG_WIDGET;

    public IMMessageRevokeStateFrameLayout(Context context) {
        super(context);
    }

    public IMMessageRevokeStateFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IMMessageRevokeStateFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public IMMessageRevokeStateFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        int childCount = getChildCount();
        if (childCount != 2) {
            throw new IllegalStateException("only support 2 child. current child count:" + childCount);
        }

        final View firstChild = getChildAt(0);
        final View secondChild = getChildAt(1);

        // 默认都不可见
        ViewUtil.setVisibilityIfChanged(firstChild, View.GONE);
        ViewUtil.setVisibilityIfChanged(secondChild, View.GONE);

        if (isInEditMode()) {
            ViewUtil.setVisibilityIfChanged(secondChild, View.VISIBLE);
        }

        if (mMessageChangedViewHelper != null) {
            mMessageChangedViewHelper.requestLoadData(false);
        }
    }

    @Override
    protected void onMessageUpdate(@Nullable IMMessage imMessage) {
        int childCount = getChildCount();
        if (childCount != 2) {
            final Throwable e = new IllegalStateException("only support 2 child. current child count:" + childCount);
            SampleLog.e(e);
            return;
        }

        final View firstChild = getChildAt(0);
        final View secondChild = getChildAt(1);
        final boolean isRevoked = imMessage != null && imMessage.type.get() == IMConstants.MessageType.REVOKED;
        if (isRevoked) {
            ViewUtil.setVisibilityIfChanged(firstChild, View.VISIBLE);
            ViewUtil.setVisibilityIfChanged(secondChild, View.GONE);
        } else {
            ViewUtil.setVisibilityIfChanged(firstChild, View.GONE);
            ViewUtil.setVisibilityIfChanged(secondChild, View.VISIBLE);
        }
    }

}
