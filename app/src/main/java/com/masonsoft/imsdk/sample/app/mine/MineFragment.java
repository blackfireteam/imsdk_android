package com.masonsoft.imsdk.sample.app.mine;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.app.signin.SignInActivity;
import com.masonsoft.imsdk.sample.common.imagepicker.ImageData;
import com.masonsoft.imsdk.sample.common.imagepicker.ImagePickerDialog;
import com.masonsoft.imsdk.sample.common.simpledialog.SimpleContentConfirmDialog;
import com.masonsoft.imsdk.sample.common.simpledialog.SimpleContentInputDialog;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleMineFragmentBinding;
import com.masonsoft.imsdk.sample.util.TipUtil;
import com.masonsoft.imsdk.user.UserInfo;
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
        mBinding.modifyGoldSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onGoldChanged(isChecked));
        mBinding.modifyVerifiedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onVerifiedChanged(isChecked));
        ViewUtil.onClick(mBinding.actionSignOut, v -> requestSignOut());

        return mBinding.getRoot();
    }

    private void clearCheckedChangeListener() {
        if (mBinding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        mBinding.modifyGoldSwitch.setOnCheckedChangeListener(null);
        mBinding.modifyVerifiedSwitch.setOnCheckedChangeListener(null);
    }

    private void bindCheckedChangeListener() {
        if (mBinding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        mBinding.modifyGoldSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onGoldChanged(isChecked));
        mBinding.modifyVerifiedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onVerifiedChanged(isChecked));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        clearPresenter();
        mView = new ViewImpl();
        mPresenter = new MineFragmentPresenter(mView);
        mPresenter.requestSyncSessionUserInfo();
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

        if (mBinding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            return;
        }
        final String unsafeCacheUsername = mBinding.username.getText().toString();
        final SimpleContentInputDialog dialog = new SimpleContentInputDialog(activity, unsafeCacheUsername);
        dialog.setOnBtnRightClickListener(input -> {
            if (mPresenter == null) {
                SampleLog.e(Constants.ErrorLog.PRESENTER_IS_NULL);
                return;
            }

            final String nickname = input.trim();
            if (TextUtils.isEmpty(nickname)) {
                TipUtil.show(R.string.imsdk_sample_profile_modify_nickname_error_empty);
                return;
            }

            mPresenter.submitNickname(nickname);
        });
        dialog.show();
    }

    private void onGoldChanged(boolean isChecked) {
        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
            return;
        }
        if (isStateSaved()) {
            SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
            return;
        }

        if (mPresenter == null) {
            SampleLog.e(Constants.ErrorLog.PRESENTER_IS_NULL);
            return;
        }
        mPresenter.trySubmitGoldChanged(isChecked);
    }

    private void onVerifiedChanged(boolean isChecked) {
        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
            return;
        }
        if (isStateSaved()) {
            SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
            return;
        }

        if (mPresenter == null) {
            SampleLog.e(Constants.ErrorLog.PRESENTER_IS_NULL);
            return;
        }
        mPresenter.trySubmitVerifiedChanged(isChecked);
    }

    private void requestSignOut() {
        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
            return;
        }
        if (isStateSaved()) {
            SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
            return;
        }

        final SimpleContentConfirmDialog dialog = new SimpleContentConfirmDialog(
                activity,
                I18nResources.getString(R.string.imsdk_sample_sign_out_confirm_text));
        dialog.setOnBtnRightClickListener(this::onSignOutConfirm);
        dialog.show();
    }

    private void onSignOutConfirm() {
        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
            return;
        }
        if (isStateSaved()) {
            SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
            return;
        }

        if (mPresenter == null) {
            SampleLog.e(Constants.ErrorLog.PRESENTER_IS_NULL);
            return;
        }
        mPresenter.requestSignOut();
    }

    class ViewImpl implements DynamicView {

        public void showSessionUserInfo(@Nullable UserInfo userInfo) {
            SampleLog.v(Objects.defaultObjectTag(this) + " showSessionUserInfo userInfo:%s", userInfo);
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }

            final boolean gold = userInfo != null && (userInfo.gold.getOrDefault(IMConstants.FALSE) == IMConstants.TRUE);
            final boolean verified = userInfo != null && (userInfo.verified.getOrDefault(IMConstants.FALSE) == IMConstants.TRUE);

            clearCheckedChangeListener();
            mBinding.modifyGoldSwitch.setChecked(gold);
            mBinding.modifyVerifiedSwitch.setChecked(verified);
            bindCheckedChangeListener();

            mBinding.actionSignOut.setEnabled(userInfo != null);
        }

        public void onAvatarUploadFail(Throwable e) {
            SampleLog.v(e, Objects.defaultObjectTag(this) + " onAvatarUploadFail");
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            mBinding.avatarProgressView.setProgress(0f);
            TipUtil.show(R.string.imsdk_sample_profile_modify_avatar_fail);
        }

        public void onAvatarUploadProgress(@IntRange(from = 0, to = 100) int percent) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAvatarUploadProgress percent:%s", percent);
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
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
                return;
            }
            TipUtil.show(R.string.imsdk_sample_profile_modify_avatar_fail);
        }

        public void onAvatarModifySuccess() {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAvatarModifySuccess");
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            TipUtil.show(R.string.imsdk_sample_profile_modify_avatar_success);
        }

        public void onNicknameModifyFail(Throwable e) {
            SampleLog.v(e, Objects.defaultObjectTag(this) + " onNicknameModifyFail");
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            TipUtil.show(R.string.imsdk_sample_profile_modify_nickname_fail);
        }

        public void onNicknameModifySuccess() {
            SampleLog.v(Objects.defaultObjectTag(this) + " onNicknameModifySuccess");
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            TipUtil.show(R.string.imsdk_sample_profile_modify_nickname_success);
        }

        public void onGoldModifySuccess() {
            SampleLog.v(Objects.defaultObjectTag(this) + " onGoldModifySuccess");
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            TipUtil.show(R.string.imsdk_sample_tip_action_general_success);
        }

        public void onGoldModifyFail(Throwable e) {
            SampleLog.v(e, Objects.defaultObjectTag(this) + " onGoldModifyFail");
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            if (mPresenter == null) {
                SampleLog.e(Constants.ErrorLog.PRESENTER_IS_NULL);
                return;
            }
            TipUtil.show(R.string.imsdk_sample_tip_action_general_fail);
            mPresenter.requestSyncSessionUserInfo();
        }

        public void onVerifiedModifySuccess() {
            SampleLog.v(Objects.defaultObjectTag(this) + " onVerifiedModifySuccess");
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            TipUtil.show(R.string.imsdk_sample_tip_action_general_success);
        }

        public void onVerifiedModifyFail(Throwable e) {
            SampleLog.v(e, Objects.defaultObjectTag(this) + " onVerifiedModifyFail");
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            if (mPresenter == null) {
                SampleLog.e(Constants.ErrorLog.PRESENTER_IS_NULL);
                return;
            }
            TipUtil.show(R.string.imsdk_sample_tip_action_general_fail);
            mPresenter.requestSyncSessionUserInfo();
        }

        public void onSignOutSuccess() {
            SampleLog.v(Objects.defaultObjectTag(this) + " onSignOutSuccess");

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }

            SignInActivity.start(activity);
            activity.finish();
        }

        public void onSignOutFail(int code, String message) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onSignOutFail code:%s, message:%s", code, message);

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }

            TipUtil.showOrDefault(message);
        }

        public void onSignOutFail(Throwable e) {
            SampleLog.v(e, Objects.defaultObjectTag(this) + " onSignOutFail");

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }

            TipUtil.show(R.string.imsdk_sample_tip_action_general_fail);
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
