package com.masonsoft.imsdk.sample;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.observable.KickedObservable;
import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.sample.app.main.MainActivity;
import com.masonsoft.imsdk.sample.common.TopActivity;
import com.masonsoft.imsdk.sample.common.simpledialog.SimpleContentNoticeDialog;
import com.masonsoft.imsdk.util.Objects;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.thread.Threads;

/**
 * 处理长连接踢下线通知
 */
public class IMKickedManager {

    private static final Singleton<IMKickedManager> INSTANCE = new Singleton<IMKickedManager>() {
        @Override
        protected IMKickedManager create() {
            return new IMKickedManager();
        }
    };

    public static IMKickedManager getInstance() {
        return INSTANCE.get();
    }

    @Nullable
    private Session mLastKickedSession;
    private int mLastKickedErrorCode;

    private boolean mAttached;

    @SuppressWarnings("FieldCanBeLocal")
    private final KickedObservable.KickedObserver mKickedObserver = (session, errorCode) -> {
        mLastKickedSession = session;
        mLastKickedErrorCode = errorCode;

        Threads.postUi(() -> {
            if (mAttached) {
                showKicked(session, errorCode);
            }
        });
    };

    @UiThread
    private void showKicked(@NonNull final Session session, int errorCode) {
        final Activity topActivity = TopActivity.getInstance().get();
        if (topActivity == null) {
            SampleLog.v(Constants.ErrorLog.ACTIVITY_IS_NULL);
            return;
        }

        if (topActivity.isFinishing()) {
            SampleLog.v(Constants.ErrorLog.ACTIVITY_IS_FINISHING);
            return;
        }

        final SimpleContentNoticeDialog dialog = new SimpleContentNoticeDialog(
                topActivity,
                I18nResources.getString(R.string.imsdk_sample_tip_kicked)
        );
        dialog.setOnHideListener(cancel -> MainActivity.start(topActivity, true));
        dialog.show();
    }

    private IMKickedManager() {
        KickedObservable.DEFAULT.registerObserver(mKickedObserver);
    }

    public void start() {
        SampleLog.v(Objects.defaultObjectTag(this) + " start");
    }

    @Nullable
    public Session getLastKickedSession() {
        return mLastKickedSession;
    }

    public int getLastKickedErrorCode() {
        return mLastKickedErrorCode;
    }

    public void clearLastKicked() {
        mLastKickedSession = null;
        mLastKickedErrorCode = 0;
    }

    public void attach() {
        mAttached = true;
    }

    public void detach() {
        mAttached = false;
    }

}
