package com.masonsoft.imsdk.sample.app.mine;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.FileUploadManager;
import com.masonsoft.imsdk.core.FileUploadProvider;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.sample.LocalSettingsManager;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.api.DefaultApi;
import com.masonsoft.imsdk.sample.widget.SessionUserIdChangedViewHelper;
import com.masonsoft.imsdk.sample.widget.UserCacheChangedViewHelper;
import com.masonsoft.imsdk.user.UserInfo;
import com.masonsoft.imsdk.user.UserInfoManager;

import io.github.idonans.core.Progress;
import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.Preconditions;
import io.github.idonans.dynamic.DynamicPresenter;
import io.github.idonans.lang.DisposableHolder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MineFragmentPresenter extends DynamicPresenter<MineFragment.ViewImpl> {

    private final DisposableHolder mRequestHolder = new DisposableHolder();
    private Object mLastUploadAvatarTag;
    @SuppressWarnings("unused")
    private final SessionUserCacheChangedViewHelper mSessionUserCacheChangedViewHelper = new SessionUserCacheChangedViewHelper();

    public MineFragmentPresenter(MineFragment.ViewImpl view) {
        super(view);
    }

    private class SessionUserCacheChangedViewHelper extends UserCacheChangedViewHelper {

        @SuppressWarnings("FieldCanBeLocal")
        private final SessionUserIdChangedViewHelper mSessionUserIdChangedViewHelper = new SessionUserIdChangedViewHelper() {
            @Override
            protected void onSessionUserIdChanged(long sessionUserId) {
                SessionUserCacheChangedViewHelper.this.setTargetUserId(sessionUserId);
            }
        };

        private SessionUserCacheChangedViewHelper() {
            setTargetUserId(mSessionUserIdChangedViewHelper.getSessionUserId());
        }

        @Override
        protected void onUserCacheChanged(@Nullable UserInfo userInfo) {
            Threads.postUi(() -> showSessionUserInfo(userInfo));
        }
    }

    private void showSessionUserInfo(@Nullable UserInfo userInfo) {
        final MineFragment.ViewImpl view = getView();
        if (view != null) {
            view.showSessionUserInfo(userInfo);
        }
    }

    public void requestSyncSessionUserInfo() {
        mRequestHolder.set(Single.just("")
                .map(input -> {
                    final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
                    Preconditions.checkArgument(sessionUserId > 0);
                    return UserInfoManager.getInstance().getByUserId(sessionUserId);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sessionUserInfo -> {
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    view.showSessionUserInfo(sessionUserInfo);
                }, SampleLog::e));
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

    public void submitNickname(String nickname) {
        mRequestHolder.set(Single.just("")
                .map(input -> {
                    final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
                    Preconditions.checkArgument(sessionUserId > 0);
                    DefaultApi.updateNickname(sessionUserId, nickname);
                    UserInfoManager.getInstance().updateNickname(sessionUserId, nickname);
                    return input;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ignore -> {
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    view.onNicknameModifySuccess();
                }, e -> {
                    SampleLog.e(e);
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    view.onNicknameModifyFail(e);
                }));
    }

    public void trySubmitGoldChanged(boolean isChecked) {
        mRequestHolder.set(Single.just("")
                .map(input -> {
                    final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
                    Preconditions.checkArgument(sessionUserId > 0);
                    final UserInfo sessionUserInfo = UserInfoManager.getInstance().getByUserId(sessionUserId);
                    Preconditions.checkNotNull(sessionUserInfo);

                    // 是否提交更改
                    boolean submit = false;
                    if (sessionUserInfo.gold.isUnset()) {
                        submit = true;
                    } else {
                        boolean currentChecked = sessionUserInfo.gold.get() == IMConstants.TRUE;
                        if (currentChecked != isChecked) {
                            submit = true;
                        }
                    }
                    return submit;
                })
                .map(submit -> {
                    if (!submit) {
                        return new Object();
                    }

                    final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
                    Preconditions.checkArgument(sessionUserId > 0);
                    DefaultApi.updateGold(sessionUserId, isChecked);
                    UserInfoManager.getInstance().updateGold(sessionUserId, isChecked);
                    return new Object();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ignore -> {
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    view.onGoldModifySuccess();
                }, e -> {
                    SampleLog.e(e);
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    view.onGoldModifyFail(e);
                }));
    }

    public void trySubmitVerifiedChanged(boolean isChecked) {
        mRequestHolder.set(Single.just("")
                .map(input -> {
                    final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
                    Preconditions.checkArgument(sessionUserId > 0);
                    final UserInfo sessionUserInfo = UserInfoManager.getInstance().getByUserId(sessionUserId);
                    Preconditions.checkNotNull(sessionUserInfo);

                    // 是否提交更改
                    boolean submit = false;
                    if (sessionUserInfo.verified.isUnset()) {
                        submit = true;
                    } else {
                        boolean currentChecked = sessionUserInfo.verified.get() == IMConstants.TRUE;
                        if (currentChecked != isChecked) {
                            submit = true;
                        }
                    }
                    return submit;
                })
                .map(submit -> {
                    if (!submit) {
                        return new Object();
                    }

                    final long sessionUserId = IMSessionManager.getInstance().getSessionUserId();
                    Preconditions.checkArgument(sessionUserId > 0);
                    DefaultApi.updateVerified(sessionUserId, isChecked);
                    UserInfoManager.getInstance().updateVerified(sessionUserId, isChecked);
                    return new Object();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ignore -> {
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    view.onVerifiedModifySuccess();
                }, e -> {
                    SampleLog.e(e);
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    view.onVerifiedModifyFail(e);
                }));
    }

    public void requestSignOut() {
        mRequestHolder.set(Single.just("")
                .map(input -> IMSessionManager.getInstance().signOutWithBlockOrTimeout())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    // clear settings token
                    final LocalSettingsManager.Settings settings = LocalSettingsManager.getInstance().getSettings();
                    settings.imToken = null;
                    LocalSettingsManager.getInstance().setSettings(settings);

                    if (result.isSuccess()) {
                        view.onSignOutSuccess();
                    } else {
                        if (result.other != null) {
                            view.onSignOutFail(result.other.code, result.other.message);
                        } else {
                            view.onSignOutFail(result.code, result.message);
                        }
                    }
                }, e -> {
                    SampleLog.e(e);
                    final MineFragment.ViewImpl view = getView();
                    if (view == null) {
                        return;
                    }

                    // clear settings token
                    final LocalSettingsManager.Settings settings = LocalSettingsManager.getInstance().getSettings();
                    settings.imToken = null;
                    LocalSettingsManager.getInstance().setSettings(settings);

                    view.onSignOutFail(e);
                }));
    }

}
