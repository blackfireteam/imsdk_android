package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.sample.databinding.ImsdkSampleWidgetCustomKeyboardBinding;

public class CustomKeyboard extends FrameLayout {

    public CustomKeyboard(@NonNull Context context) {
        super(context);
        initFromAttributes(context, null, 0, 0);
    }

    public CustomKeyboard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs, 0, 0);
    }

    public CustomKeyboard(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public CustomKeyboard(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private ImsdkSampleWidgetCustomKeyboardBinding mBinding;

    private void initFromAttributes(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {

        mBinding = ImsdkSampleWidgetCustomKeyboardBinding.inflate(
                LayoutInflater.from(context),
                this,
                true);
    }

}
