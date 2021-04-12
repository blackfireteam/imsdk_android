package com.masonsoft.imsdk.sample.common.impopup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;

import io.github.idonans.core.util.DimenUtil;
import io.github.idonans.lang.util.ViewUtil;

/**
 * 长按一个会话的弹框视图, 此 View 需要添加在与所在 Window 同样大小的空间上才能显示正确.
 */
public class IMChatConversationPopupView extends ViewGroup {

    private final boolean DEBUG = Constants.DEBUG_WIDGET;

    public IMChatConversationPopupView(Context context) {
        this(context, null);
    }

    public IMChatConversationPopupView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMChatConversationPopupView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public IMChatConversationPopupView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private int mMenuTextSize = DimenUtil.dp2px(12);
    private int mMenuTextColor = 0xFFffffff;
    private int mMenuTextPaddingLeft = DimenUtil.dp2px(12);
    private int mMenuTextPaddingRight = DimenUtil.dp2px(12);
    private int mMenuTextPaddingTop = DimenUtil.dp2px(9);
    private int mMenuTextPaddingBottom = DimenUtil.dp2px(9);
    private int mMenuAnchorSpace = DimenUtil.dp2px(0); // 菜单与 AnchorView 之间的间距
    private LinearLayout mMenuLayout;

    @Nullable
    private View mAnchorView;
    @Nullable
    private Drawable mAnchorViewCover;

    private Rect mAnchorViewRect;
    private AnchorSpaceDrawable mAnchorSpaceDrawable = new AnchorSpaceDrawable();

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mMenuLayout = new LinearLayout(context);
        mMenuLayout.setBackgroundResource(R.drawable.ic_common_rect_corners_8dp_333333);
        mMenuLayout.setOrientation(LinearLayout.HORIZONTAL);
        mMenuLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);

        final int dividerWidth = DimenUtil.dp2px(1);
        ColorDrawable dividerDrawable = new ColorDrawable(0x33FFFFFF) {
            @Override
            public int getIntrinsicWidth() {
                return dividerWidth;
            }
        };
        mMenuLayout.setDividerDrawable(dividerDrawable);
        mMenuLayout.setDividerPadding(DimenUtil.dp2px(7));
        mMenuLayout.setMinimumHeight(DimenUtil.dp2px(35));
        mMenuLayout.setMinimumWidth(DimenUtil.dp2px(35));
        mMenuLayout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(mMenuLayout);
        setWillNotDraw(false);
    }

    /**
     * @param anchorView         显示在目标 view 的顶部或者底部，顶部优先。
     * @param coverDrawableResId 覆盖在目标 view 上遮盖层
     * @param menus              需要显示的菜单项
     */
    public void showForAnchorView(View anchorView, @DrawableRes int coverDrawableResId, String[] menus) {
        mAnchorViewRect = new Rect();
        anchorView.getGlobalVisibleRect(mAnchorViewRect);

        mAnchorView = anchorView;
        if (coverDrawableResId != 0) {
            mAnchorViewCover = getResources().getDrawable(coverDrawableResId, null);
        }
        updateMenus(menus);
        requestLayout();
        invalidate();
    }

    private void updateMenus(String[] menus) {
        mMenuLayout.removeAllViews();
        if (menus != null) {
            int size = menus.length;
            for (int i = 0; i < size; i++) {
                final int menuIndex = i;
                final String menuText = menus[i];
                TextView textView = new TextView(getContext());
                textView.setText(menuText);
                textView.setIncludeFontPadding(false);
                textView.setMaxLines(1);
                textView.setSingleLine();
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mMenuTextSize);
                textView.setTextColor(mMenuTextColor);
                textView.setPadding(mMenuTextPaddingLeft, mMenuTextPaddingTop, mMenuTextPaddingRight, mMenuTextPaddingBottom);

                ViewUtil.onClick(textView, v -> dispatchMenuClick(textView, menuText, menuIndex));

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.CENTER_VERTICAL;
                textView.setLayoutParams(layoutParams);
                mMenuLayout.addView(textView);
            }
        }
    }

    private void dispatchMenuClick(View menuView, String menuText, int menuIndex) {
        if (DEBUG) {
            SampleLog.v("dispatchMenuClick menuText:%s, menuIndex:%s", menuText, menuIndex);
        }
        if (mOnIMMenuClickListener != null) {
            mOnIMMenuClickListener.onItemMenuClick(menuText, menuIndex);
        }
    }

    private OnIMMenuClickListener mOnIMMenuClickListener;

    public void setOnIMMenuClickListener(OnIMMenuClickListener onIMMenuClickListener) {
        mOnIMMenuClickListener = onIMMenuClickListener;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int menuWidth = mMenuLayout.getMeasuredWidth();
        int menuHalfWidth = menuWidth / 2;
        int menuHeight = mMenuLayout.getMeasuredHeight();

        // 在 mAnchorViewRect 居中显示 menu
        final int topSpace = mAnchorViewRect.top;
        final int bottomSpace = b - mAnchorViewRect.bottom;

        int menuTop = mAnchorViewRect.top - mMenuAnchorSpace - menuHeight + mAnchorViewRect.height() / 2;
        final int anchorCenterX = mAnchorViewRect.left + (mAnchorViewRect.width() / 2);
        int menuLeft = anchorCenterX - menuHalfWidth;
        mMenuLayout.layout(menuLeft, menuTop, menuLeft + menuWidth, menuTop + menuHeight);

        int anchorSpaceDrawableWidth = mAnchorSpaceDrawable.getIntrinsicWidth();
        int anchorSpaceDrawableHeight = mAnchorSpaceDrawable.getIntrinsicHeight();
        int anchorSpaceDrawableLeft = anchorCenterX - anchorSpaceDrawableWidth / 2;
        int anchorSpaceDrawableTop = menuTop + menuHeight;

        mAnchorSpaceDrawable.setBounds(anchorSpaceDrawableLeft,
                anchorSpaceDrawableTop,
                anchorSpaceDrawableLeft + anchorSpaceDrawableWidth,
                anchorSpaceDrawableTop + anchorSpaceDrawableHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int menuWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int menuHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        mMenuLayout.measure(menuWidthMeasureSpec, menuHeightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw arrow space
        if (mAnchorSpaceDrawable != null) {
            mAnchorSpaceDrawable.draw(canvas);
        }
    }

    public void showAnchorViewCover() {
        if (mAnchorView != null && mAnchorViewCover != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mAnchorView.setForeground(mAnchorViewCover);
            }
        }
    }

    public void hideAnchorViewCover() {
        if (mAnchorView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mAnchorView.setForeground(null);
            }
        }
    }

    private class AnchorSpaceDrawable extends Drawable {

        private Paint mPaint;
        private int mWidth = DimenUtil.dp2px(5);
        private int mHeight = DimenUtil.dp2px(3);
        private Path mPath = new Path();
        private boolean mReverse; // 上下翻转显示

        public AnchorSpaceDrawable() {
            mPaint = new Paint();
            mPaint.setDither(true);
            mPaint.setAntiAlias(true);
            mPaint.setColor(0xFF333333);
            mPaint.setStyle(Paint.Style.FILL);
        }

        public void setReverse(boolean reverse) {
            mReverse = reverse;
        }

        @Override
        public int getIntrinsicWidth() {
            return mWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mHeight;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();

            if (mReverse) {
                canvas.save();
                // 中心旋转 180 度
                canvas.rotate(180, bounds.left + bounds.width() / 2f, bounds.top + bounds.height() / 2f);
            }

            mPath.reset();
            mPath.moveTo(bounds.left, bounds.top);
            mPath.lineTo(bounds.right, bounds.top);
            mPath.lineTo(bounds.left + bounds.width() / 2f, bounds.bottom);
            mPath.close();
            canvas.drawPath(mPath, mPaint);

            if (mReverse) {
                canvas.restore();
            }
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

}