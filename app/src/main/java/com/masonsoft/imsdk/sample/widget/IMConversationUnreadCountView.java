package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.Nullable;

import com.idonans.core.util.DimenUtil;
import com.idonans.lang.util.ViewUtil;
import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.sample.Constants;

public class IMConversationUnreadCountView extends IMConversationDynamicFrameLayout {

    private final boolean DEBUG = Constants.DEBUG_WIDGET;

    public IMConversationUnreadCountView(Context context) {
        this(context, null);
    }

    public IMConversationUnreadCountView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMConversationUnreadCountView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    private Paint mBackgroundPaint;
    private Paint mTextPaint;

    private boolean mOnlyDrawableBackground = true;

    private long mUnreadCount;
    private String mUnreadCountText;
    private RectF mTextRect = new RectF();

    private final int mDefaultMeasureSize = DimenUtil.dp2px(18);
    private final int mAdjustPaddingLeftRight = DimenUtil.dp2px(5);

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setDither(true);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        mBackgroundPaint.setStrokeJoin(Paint.Join.ROUND);
        mBackgroundPaint.setColor(0xFFFF566F);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setDither(true);
        mTextPaint.setStrokeCap(Paint.Cap.ROUND);
        mTextPaint.setStrokeJoin(Paint.Join.ROUND);
        mTextPaint.setColor(0xFFFFFFFF);
        mTextPaint.setTextSize(DimenUtil.sp2px(11));

        setWillNotDraw(false);
    }

    public void setOnlyDrawableBackground(boolean onlyDrawableBackground) {
        if (mOnlyDrawableBackground != onlyDrawableBackground) {
            mOnlyDrawableBackground = onlyDrawableBackground;
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onConversationUpdate(@Nullable IMConversation imConversation) {
        if (imConversation == null) {
            setUnreadCount(0L);
        } else {
            setUnreadCount(imConversation.unreadCount.getOrDefault(0L));
        }
    }

    public void setUnreadCount(long unreadCount) {
        if (mUnreadCount != unreadCount) {
            mUnreadCount = unreadCount;
            mUnreadCountText = String.valueOf(mUnreadCount <= 99 ? mUnreadCount : "99+");
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);

        if (widthMode == View.MeasureSpec.EXACTLY && heightMode == View.MeasureSpec.EXACTLY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int adjustHeight;
        if (heightMode == View.MeasureSpec.EXACTLY) {
            adjustHeight = heightSize;
        } else {
            adjustHeight = mDefaultMeasureSize;
        }

        int adjustWidth;
        if (widthMode == View.MeasureSpec.EXACTLY) {
            adjustWidth = widthSize;
        } else {
            if (mUnreadCount <= 99) {
                adjustWidth = mDefaultMeasureSize;
            } else {
                float textWidth = 0f;
                if (mUnreadCountText != null) {
                    textWidth = mTextPaint.measureText(mUnreadCountText);
                }
                textWidth += 2 * mAdjustPaddingLeftRight;
                if (textWidth < mDefaultMeasureSize) {
                    textWidth = mDefaultMeasureSize;
                }
                adjustWidth = (int) textWidth;
            }
        }

        super.onMeasure(
                View.MeasureSpec.makeMeasureSpec(adjustWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(adjustHeight, View.MeasureSpec.EXACTLY)
        );
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mTextRect.set(0, 0, getWidth(), getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mUnreadCount <= 0) {
            return;
        }

        float circleCenterX = getWidth() / 2f;
        float circleCenterY = getHeight() / 2f;
        float circleR = Math.min(circleCenterX, circleCenterY);

        // 绘制背景
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), circleR, circleR, mBackgroundPaint);

        if (mOnlyDrawableBackground) {
            return;
        }

        // 绘制文字
        if (mUnreadCountText != null) {
            ViewUtil.drawText(canvas, mUnreadCountText, mTextPaint, mTextRect, Gravity.CENTER);
        }
    }

}
