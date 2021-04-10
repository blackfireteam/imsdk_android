package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.sample.databinding.ImsdkSampleWidgetMainBottomBarBinding;

import io.github.idonans.lang.util.ViewUtil;

public class MainBottomBar extends FrameLayout {

    public MainBottomBar(@NonNull Context context) {
        super(context);
        initFromAttributes(context, null, 0, 0);
    }

    public MainBottomBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs, 0, 0);
    }

    public MainBottomBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public MainBottomBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private static final int TAB_DISCOVER = 0;
    private static final int TAB_CONVERSATION = 1;
    private static final int TAB_MINE = 2;

    private int mCurrentItem = 0;
    private ImsdkSampleWidgetMainBottomBarBinding mBinding;
    private OnTabClickListener mOnTabClickListener;

    private void initFromAttributes(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {

        mBinding = ImsdkSampleWidgetMainBottomBarBinding.inflate(
                LayoutInflater.from(context),
                this,
                true);
        syncCurrentItem();

        ViewUtil.onClick(mBinding.tabDiscoverText, v -> {
            if (mOnTabClickListener != null) {
                mOnTabClickListener.onTabClick(TAB_DISCOVER);
            }
        });
        ViewUtil.onClick(mBinding.tabConversationText, v -> {
            if (mOnTabClickListener != null) {
                mOnTabClickListener.onTabClick(TAB_CONVERSATION);
            }
        });
        ViewUtil.onClick(mBinding.tabMineText, v -> {
            if (mOnTabClickListener != null) {
                mOnTabClickListener.onTabClick(TAB_MINE);
            }
        });
    }

    public int getCurrentItem() {
        return mCurrentItem;
    }

    public void setCurrentItem(int currentItem) {
        if (mCurrentItem != currentItem) {
            mCurrentItem = currentItem;

            syncCurrentItem();
        }
    }

    private void syncCurrentItem() {
        mBinding.tabDiscoverText.setSelected(mCurrentItem == 0);
        mBinding.tabConversationText.setSelected(mCurrentItem == 1);
        mBinding.tabMineText.setSelected(mCurrentItem == 2);
    }

    public void setOnTabClickListener(OnTabClickListener onTabClickListener) {
        mOnTabClickListener = onTabClickListener;
    }

    public interface OnTabClickListener {
        void onTabClick(int index);
    }

}
