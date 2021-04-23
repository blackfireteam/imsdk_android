package com.masonsoft.imsdk.sample.app.mine;

import android.net.Uri;

import com.masonsoft.imsdk.core.FileUploadManager;
import com.masonsoft.imsdk.core.FileUploadProvider;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.api.DefaultApi;
import com.masonsoft.imsdk.user.UserInfoManager;
import com.masonsoft.imsdk.util.Preconditions;

import io.github.idonans.core.Progress;
import io.github.idonans.core.thread.Threads;
import io.github.idonans.dynamic.DynamicPresenter;
import io.github.idonans.lang.DisposableHolder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MineFragmentPresenter extends DynamicPresenter<MineFragment.ViewImpl> {

    private final DisposableHolder mRequestHolder = new DisposableHolder();
    private Object mLastUploadAvatarTag;

    public MineFragmentPresenter(MineFragment.ViewImpl view) {
        super(view);
    }

    public void uploadAvatar(Uri photoUri) {
        final Object uploadAvatarTag = new Object();
        mLastUploadAvatarTag = uploadAvatarTag;
        mRequestHolder.set(Single.just("")
                .map(input -> {
                    final Progress progress = new Progress() {
                        @Override
                        protected void onUpdate() {
                            super.onUpdate();
                            // notify avatar upload progress
                            Threads.postUi(() -> {
                                if (mLastUploadAvatarTag == uploadAvatarTag) {
                                    final MineFragment.ViewImpl view = getView();
                                    if (view == null) {
                                        return;
                                    }
                                    view.onAvatarUploadProgress(getPercent());
                                }
                            });
                        }
                    };
                    final FileUploadProvider fileUploadProvider = FileUploadManager.getInstance().getFileUploadProvider();
                    return fileUploadProvider.uploadFile(photoUri.toString(), progress);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(avatarUrl -> {
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }
                    view.onAvatarUploadSuccess(avatarUrl);
                }, e -> {
                    SampleLog.e(e);
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    view.onAvatarUploadFail(e);
                }));
    }

    public void submitAvatar(String avatarUrl) {
        mLastUploadAvatarTag = null;
        mRequestHolder.set(Single.just("")
                .map(input -> {
                    final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
                    Preconditions.checkArgument(sessionUserId > 0);
                    DefaultApi.updateAvatar(sessionUserId, avatarUrl);
                    UserInfoManager.getInstance().updateAvatar(sessionUserId, avatarUrl);
                    return input;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ignore -> {
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    view.onAvatarModifySuccess();
                }, e -> {
                    SampleLog.e(e);
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    view.onAvatarModifyFail(e);
                }));
    }

}
