package com.masonsoft.imsdk.sample.app.mine;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.common.imagepicker.ImageData;
import com.masonsoft.imsdk.sample.common.imagepicker.ImagePickerDialog;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleMineFragmentBinding;
import com.masonsoft.imsdk.sample.util.TipUtil;
import com.masonsoft.imsdk.util.Objects;

import io.github.idonans.dynamic.DynamicView;
import io.github.idonans.lang.util.ViewUtil;

/**
 * 我的
 */
public class MineFragment extends SystemInsetsFragment {

    public static MineFragment newInstance() {
        Bundle args = new Bundle();
        MineFragment fragment = new MineFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private ImsdkSampleMineFragmentBinding mBinding;
    private MineFragmentPresenter mPresenter;
    private ViewImpl mView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SampleLog.v("onCreateView %s", getClass());

        mBinding = ImsdkSampleMineFragmentBinding.inflate(inflater, container, false);
        ViewUtil.onClick(mBinding.avatar, v -> startModifyAvatar());
        ViewUtil.onClick(mBinding.modifyUsername, v -> startModifyUsername());

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        clearPresenter();
        mView = new ViewImpl();
        mPresenter = new MineFragmentPresenter(mView);
    }

    private void startModifyAvatar() {
        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
            return;
        }
        if (isStateSaved()) {
            SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
            return;
        }

        final ImagePickerDialog dialog = new ImagePickerDialog(activity, activity.findViewById(Window.ID_ANDROID_CONTENT));
        dialog.setOnImagePickListener(imageInfoList -> {
            if (imageInfoList.isEmpty()) {
                return false;
            }

            final ImageData.ImageInfo imageInfo = imageInfoList.get(0);
            onPickAvatarResult(imageInfo.uri);
            return true;
        });
        dialog.show();
    }

    private void onPickAvatarResult(final Uri uri) {
        SampleLog.v("onPickAvatarResult uri:%s", uri);

        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }
        if (isStateSaved()) {
            SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            return;
        }
        if (mPresenter == null) {
            SampleLog.e(Constants.ErrorLog.PRESENTER_IS_NULL);
            return;
        }

        mPresenter.uploadAvatar(uri);
    }

    private void startModifyUsername() {
        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
            return;
        }
        if (isStateSaved()) {
            SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
            return;
        }

        // TODO
    }

    class ViewImpl implements DynamicView {

        public void onAvatarUploadFail(Throwable e) {
            SampleLog.v(e, Objects.defaultObjectTag(this) + " onAvatarUploadFail");
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            }
            mBinding.avatarProgressView.setProgress(0f);
            TipUtil.show(R.string.imsdk_sample_profile_modify_avatar_fail);
        }

        public void onAvatarUploadProgress(@IntRange(from = 0, to = 100) int percent) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAvatarUploadProgress percent:%s", percent);
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            }
            mBinding.avatarProgressView.setProgress(percent / 100f);
        }

        public void onAvatarUploadSuccess(String avatarUrl) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAvatarUploadSuccess avatarUrl:%s", avatarUrl);

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            if (mPresenter == null) {
                SampleLog.e(Constants.ErrorLog.PRESENTER_IS_NULL);
                return;
            }
            mPresenter.submitAvatar(avatarUrl);
        }

        public void onAvatarModifyFail(Throwable e) {
            SampleLog.v(e, Objects.defaultObjectTag(this) + " onAvatarModifyFail");
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            }
            TipUtil.show(R.string.imsdk_sample_profile_modify_avatar_fail);
        }

        public void onAvatarModifySuccess() {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAvatarModifySuccess");
        }

    }

    private void clearPresenter() {
        if (mPresenter != null) {
            mPresenter.setAbort();
            mPresenter = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearPresenter();
        mBinding = null;
        mView = null;
    }

}
