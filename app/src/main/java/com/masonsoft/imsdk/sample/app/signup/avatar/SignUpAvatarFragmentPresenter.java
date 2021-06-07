package com.masonsoft.imsdk.sample.app.signup.avatar;

import android.net.Uri;

import com.masonsoft.imsdk.core.FileUploadManager;
import com.masonsoft.imsdk.core.FileUploadProvider;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.api.ApiResponseException;
import com.masonsoft.imsdk.sample.api.DefaultApi;
import com.masonsoft.imsdk.sample.app.signup.SignUpViewPresenter;

import io.github.idonans.core.Progress;
import io.github.idonans.core.thread.Threads;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SignUpAvatarFragmentPresenter extends SignUpViewPresenter<SignUpAvatarFragment.ViewImpl> {

    private Object mLastUploadAvatarTag;

    public SignUpAvatarFragmentPresenter(SignUpAvatarFragment.ViewImpl view) {
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
                                    final SignUpAvatarFragment.ViewImpl view = getView();
                                    if (view == null) {
                                        return;
                                    }
                                    view.onAvatarUploadProgress(getPercent());
                                }
                            });
                        }
                    };
                    final FileUploadProvider fileUploadProvider = FileUploadManager.getInstance().getFileUploadProvider();
                    return fileUploadProvider.uploadFile(photoUri.toString(), null, progress);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(avatarUrl -> {
                    final SignUpAvatarFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }
                    view.onAvatarUploadSuccess(avatarUrl);
                }, e -> {
                    SampleLog.e(e);
                    final SignUpAvatarFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    view.onAvatarUploadFail(e);
                }));
    }

    public void requestSignUp(long userId, String nickname, String avatar) {
        mRequestHolder.set(Single.just("")
                .map(input -> DefaultApi.reg(userId, nickname, avatar))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(avatarUrl -> {
                    final SignUpAvatarFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }
                    view.onSignUpSuccess(userId);
                }, e -> {
                    SampleLog.e(e);
                    final SignUpAvatarFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    if (e instanceof ApiResponseException) {
                        view.onSignUpFail(((ApiResponseException) e).code, ((ApiResponseException) e).message);
                        return;
                    }
                    view.onSignUpFail(e);
                }));
    }

}
