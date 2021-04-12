package com.masonsoft.imsdk.sample.widget.debug;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

class DebugTextView extends AppCompatTextView {

    public DebugTextView(@NonNull Context context) {
        super(context);
        initFromAttributes(context, null, 0, 0);
    }

    public DebugTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs, 0, 0);
    }

    public DebugTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.setTextSize(12);
        this.setIncludeFontPadding(false);
        this.setTextColor(0xffff0000);
        this.setBackgroundColor(0x90999999);
    }

}