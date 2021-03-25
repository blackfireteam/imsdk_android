package com.masonsoft.imsdk.sample.widget;

import androidx.annotation.Nullable;

import com.idonans.core.thread.Threads;
import com.idonans.lang.DisposableHolder;
import com.masonsoft.imsdk.core.observable.UserInfoObservable;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.user.UserCacheManager;
import com.masonsoft.imsdk.user.UserInfo;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public abstract class UserCacheChangedViewHelper {

    private final DisposableHolder mRequestHolder = new DisposableHolder();

    private long mTargetUserId;

    public UserCacheChangedViewHelper() {
        UserInfoObservable.DEFAULT.registerObserver(mUserInfoObserver);
    }

    public void setTargetUserId(long targetUserId) {
        if (mTargetUserId != targetUserId || targetUserId <= 0) {
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
                    UserInfo userInfo = UserCacheManager.getInstance().getByUserId(mTargetUserId);
                    if (userInfo == null) {
                        userInfo = UserInfo.valueOf(mTargetUserId);
                    }
                    return userInfo;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUserCacheChanged, SampleLog::e));
    }

    protected abstract void onUserCacheChanged(@Nullable UserInfo userInfo);

    @SuppressWarnings("FieldCanBeLocal")
    private final UserInfoObservable.UserInfoObserver mUserInfoObserver = new UserInfoObservable.UserInfoObserver() {
        @Override
        public void onUserInfoChanged(long userId) {
            if (mTargetUserId == userId) {
                Threads.postUi(() -> {
                    requestLoadData(false);
                });
            }
        }
    };

}
