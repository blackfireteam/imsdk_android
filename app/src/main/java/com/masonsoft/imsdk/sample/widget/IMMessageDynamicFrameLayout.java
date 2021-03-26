package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.lang.DisposableHolder;

public abstract class IMMessageDynamicFrameLayout extends FrameLayout {

    protected final boolean DEBUG = false;

    private final DisposableHolder mRequestHolder = new DisposableHolder();
    private long mTargetLocalMessageId;

    public IMMessageDynamicFrameLayout(Context context) {
        this(context, null);
    }

    public IMMessageDynamicFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMMessageDynamicFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public IMMessageDynamicFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        IMLocalEventMessageChanged.EVENT_PRO.addEventProListener(mIMLocalEventMessageChangedUiEventProListener);
    }

    public void setTargetLocalMessageId(long targetLocalMessageId) {
        if (mTargetLocalMessageId != targetLocalMessageId) {
            mTargetLocalMessageId = targetLocalMessageId;
            requestLoadDynamic(true);
        }
    }

    public long getTargetLocalMessageId() {
        return mTargetLocalMessageId;
    }

    private void requestLoadDynamic(boolean reset) {
        if (reset) {
            onDynamicUpdate(null);
        }
        mRequestHolder.set(Single.fromCallable(() -> ImManager.getInstance().getMessage(mTargetLocalMessageId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDynamicUpdate, Timber::e));
    }

    protected abstract void onDynamicUpdate(@Nullable ImMessage object);

    private final EventPro.UiEventProListener<IMLocalEventMessageChanged> mIMLocalEventMessageChangedUiEventProListener = event -> {
        if (mTargetLocalMessageId != event.localMessageId) {
            return;
        }
        requestLoadDynamic(false);
    };

}
