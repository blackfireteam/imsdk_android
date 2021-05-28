package com.masonsoft.imsdk.sample.widget.debug;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.masonsoft.imsdk.sample.Constants;

import io.github.idonans.lang.util.ViewUtil;

class DebugTextView extends AppCompatTextView {

    public DebugTextView(@NonNull Context context) {
        this(context, null);
    }

    public DebugTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DebugTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DebugTextView(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initFromAttributes(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.setTextSize(12);
        this.setIncludeFontPadding(false);
        this.setTextColor(0xffff0000);
        this.setBackgroundColor(0x90999999);

        if (!Constants.DEBUG_WIDGET) {
            ViewUtil.setVisibilityIfChanged(this, View.GONE);
        }
    }

}
