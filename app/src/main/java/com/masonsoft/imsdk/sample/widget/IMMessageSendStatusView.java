package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;

import io.github.idonans.core.util.DimenUtil;
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

    private RectF mContentRect = new RectF();
    private RectF mSquareRect = new RectF();

    private float mPointSize = DimenUtil.dp2px(4);
    private float mPointR = mPointSize / 2;
    private Paint mPointPaint;
    private float[] mPointsLocation = new float[6]; // 发送中绘制的三个点的坐标

    private Drawable mSendFailDrawable;

    private long mMessageSendTimeMs = 0L;
    private int mMessageSendStatus = -1;

    @Nullable
    private IMMessage mImMessageUnsafe;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setWillNotDraw(false);

        mPointPaint = new Paint();
        mPointPaint.setAntiAlias(true);
        mPointPaint.setDither(true);
        mPointPaint.setColor(0xFFFE486C);
        mPointPaint.setStrokeCap(Paint.Cap.ROUND);
        mPointPaint.setStrokeJoin(Paint.Join.ROUND);
        mPointPaint.setStrokeWidth(mPointSize);

        mSendFailDrawable = ContextCompat.getDrawable(context, R.drawable.imsdk_sample_ic_message_send_fail);

        ViewUtil.onClick(this, v -> {
            final IMMessage imMessage = mImMessageUnsafe;
            if (imMessage != null
                    && !imMessage.sendState.isUnset()) {
                if (imMessage.sendState.get() == IMConstants.SendStatus.FAIL) {
                    IMMessageQueueManager.getInstance().enqueueResendSessionMessage(imMessage);
                }
            }
        });
    }

    @Override
    protected void onMessageUpdate(@Nullable IMMessage message) {
        if (DEBUG) {
            SampleLog.v("onMessageUpdate %s", message);
        }
        mImMessageUnsafe = message;
        if (message == null) {
            mMessageSendStatus = -1;
            mMessageSendTimeMs = 0L;
            postInvalidate();
            return;
        }

        mMessageSendTimeMs = message.timeMs.getOrDefault(0L);

        if (message.sendState.isUnset()) {
            mMessageSendStatus = -1;
            postInvalidate();
            return;
        }

        if (message.sendState.get() != mMessageSendStatus) {
            mMessageSendStatus = message.sendState.get();
            postInvalidate();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mContentRect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        float squareSize = Math.min(mContentRect.width(), mContentRect.height());
        float halfSquareSize = squareSize / 2;
        float centerX = mContentRect.centerX();
        float centerY = mContentRect.centerY();
        mSquareRect.set(centerX - halfSquareSize,
                centerY - halfSquareSize,
                centerX + halfSquareSize,
                centerY + halfSquareSize);

        mPointsLocation[0] = centerX - halfSquareSize + mPointR;
        mPointsLocation[1] = centerY;
        mPointsLocation[2] = centerX;
        mPointsLocation[3] = centerY;
        mPointsLocation[4] = centerX + halfSquareSize - mPointR;
        mPointsLocation[5] = centerY;

        mSendFailDrawable.setBounds(
                ((int) mSquareRect.left),
                ((int) mSquareRect.top),
                ((int) mSquareRect.right),
                ((int) mSquareRect.bottom));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mMessageSendStatus == IMConstants.SendStatus.IDLE
                || mMessageSendStatus == IMConstants.SendStatus.SENDING) {
            // 发送中
            // 绘画三个点
            final long delayTimeMs = 700L;
            boolean drawPoint = true;
            long invalidateDelay = 0;
            if (mMessageSendTimeMs > 0) {
                final long diff = System.currentTimeMillis() - mMessageSendTimeMs;
                if (diff >= 0 && diff < delayTimeMs) {
                    drawPoint = false;
                    invalidateDelay = delayTimeMs - diff;
                }
            }
            if (drawPoint) {
                canvas.drawPoint(mPointsLocation[0], mPointsLocation[1], mPointPaint);
                canvas.drawPoint(mPointsLocation[2], mPointsLocation[3], mPointPaint);
                canvas.drawPoint(mPointsLocation[4], mPointsLocation[5], mPointPaint);
            } else {
                postInvalidateDelayed(invalidateDelay);
            }
        } else if (mMessageSendStatus == IMConstants.SendStatus.FAIL) {
            // 发送失败
            mSendFailDrawable.draw(canvas);
        }
    }

}
