package com.masonsoft.imsdk.sample.widget;

import androidx.annotation.Nullable;

import com.idonans.core.thread.Threads;
import com.idonans.lang.DisposableHolder;
import com.masonsoft.imsdk.core.observable.UserInfoObservable;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.common.ObjectWrapper;
import com.masonsoft.imsdk.user.UserInfo;
import com.masonsoft.imsdk.user.UserInfoCacheManager;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public abstract class UserCacheChangedViewHelper {

    private final DisposableHolder mRequestHolder = new DisposableHolder();

    private long mTargetUserId = Long.MIN_VALUE / 2;

    public UserCacheChangedViewHelper() {
        UserInfoObservable.DEFAULT.registerObserver(mUserInfoObserver);
    }

    public void setTargetUserId(long targetUserId) {
        if (mTargetUserId != targetUserId) {
            mTargetUserId = targetUserId;
            requestLoadData(true);
        }
    }

    public long getTargetUserId() {
        return mTargetUserId;
    }

    private void requestLoadData(boolean reset) {
        // abort last
        mRequestHolder.set(null);

        if (reset) {
            onUserCacheChanged(null);
        }
        mRequestHolder.set(Single.fromCallable(
                () -> {
                    final UserInfo userInfo = UserInfoCacheManager.getInstance().getByUserId(mTargetUserId);
                    return new ObjectWrapper(userInfo);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(objectWrapper -> onUserCacheChanged((UserInfo) objectWrapper.getObject()), SampleLog::e));
    }

    protected abstract void onUserCacheChanged(@Nullable UserInfo userInfo);

    @SuppressWarnings("FieldCanBeLocal")
    private final UserInfoObservable.UserInfoObserver mUserInfoObserver = userId -> {
        if (mTargetUserId == userId) {
            Threads.postUi(() -> {
                requestLoadData(false);
            });
        }
    };

}
