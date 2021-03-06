package com.masonsoft.imsdk.sample.app.signup.avatar;

import android.Manifest;
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
import androidx.fragment.app.FragmentActivity;

import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.signin.SignInViewPresenter;
import com.masonsoft.imsdk.sample.app.signup.SignUpArgument;
import com.masonsoft.imsdk.sample.app.signup.SignUpFragment;
import com.masonsoft.imsdk.sample.app.signup.SignUpView;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSignUpAvatarFragmentBinding;
import com.masonsoft.imsdk.uikit.IMUIKitConstants;
import com.masonsoft.imsdk.uikit.common.mediapicker.MediaData;
import com.masonsoft.imsdk.uikit.common.mediapicker.MediaPickerDialog;
import com.masonsoft.imsdk.uikit.common.mediapicker.MediaSelector;
import com.masonsoft.imsdk.uikit.util.TipUtil;
import com.masonsoft.imsdk.util.Objects;
import com.tbruyelle.rxpermissions3.RxPermissions;

import io.github.idonans.core.util.Preconditions;
import io.github.idonans.lang.DisposableHolder;
import io.github.idonans.lang.util.ViewUtil;

public class SignUpAvatarFragment extends SignUpFragment {

    public static SignUpAvatarFragment newInstance(@Nullable SignUpArgument signUpArgument) {
        Bundle args = new Bundle();
        if (signUpArgument != null) {
            signUpArgument.writeTo(args);
        }
        SignUpAvatarFragment fragment = new SignUpAvatarFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private final DisposableHolder mPermissionRequest = new DisposableHolder();
    private static final String[] IMAGE_PICKER_PERMISSION = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Nullable
    private ImsdkSampleSignUpAvatarFragmentBinding mBinding;
    private ViewImpl mView;
    private SignUpAvatarFragmentPresenter mPresenter;

    private void validateSubmitState() {
        if (mBinding == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        final SignUpArgument signUpArgument = getSignUpArgument();
        if (TextUtils.isEmpty(signUpArgument.avatar)) {
            mBinding.submit.setEnabled(false);
        } else {
            mBinding.submit.setEnabled(true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = ImsdkSampleSignUpAvatarFragmentBinding.inflate(inflater, container, false);
        // mBinding.pickAvatar.setUrl("res://app/" + R.drawable.imsdk_sample_ic_add_center_inside);

        validateSubmitState();

        ViewUtil.onClick(mBinding.pickAvatar, v -> requestPickAvatarPermission());
        ViewUtil.onClick(mBinding.submit, v -> onSubmit());

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Preconditions.checkNotNull(mBinding);
        mView = new ViewImpl();
        clearPresenter();
        mPresenter = new SignUpAvatarFragmentPresenter(mView);
    }

    private void requestPickAvatarPermission() {
        SampleLog.v("requestPickAvatarPermission");

        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        //noinspection CastCanBeRemovedNarrowingVariableType
        final RxPermissions rxPermissions = new RxPermissions((FragmentActivity) activity);
        mPermissionRequest.set(
                rxPermissions.request(IMAGE_PICKER_PERMISSION)
                        .subscribe(granted -> {
                            if (granted) {
                                onPickAvatarPermissionGranted();
                            } else {
                                SampleLog.e(IMUIKitConstants.ErrorLog.PERMISSION_REQUIRED);
                            }
                        }));
    }

    private void onPickAvatarPermissionGranted() {
        SampleLog.v("onPickAvatarPermissionGranted");

        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        final MediaPickerDialog mediaPickerDialog = new MediaPickerDialog(activity, activity.findViewById(Window.ID_ANDROID_CONTENT));
        mediaPickerDialog.setMediaSelector(new MediaSelector.SimpleMediaSelector());
        mediaPickerDialog.setOnMediaPickListener(imageInfoList -> {
            if (imageInfoList.isEmpty()) {
                return false;
            }

            final MediaData.MediaInfo mediaInfo = imageInfoList.get(0);
            onPickAvatarResult(mediaInfo.uri);
            return true;
        });
        mediaPickerDialog.show();
    }

    private void onPickAvatarResult(final Uri uri) {
        SampleLog.v("onPickAvatarResult uri:%s", uri);

        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.BINDING_IS_NULL);
            return;
        }
        if (mPresenter == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.PRESENTER_IS_NULL);
            return;
        }

        mBinding.pickAvatar.setImageUrl(null, uri.toString());
        mPresenter.uploadAvatar(uri);
    }

    private void onSubmit() {
        SampleLog.v("onSubmit");

        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        final SignUpArgument signUpArgument = getSignUpArgument();
        saveSignUpArgument();

        if (TextUtils.isEmpty(signUpArgument.avatar)) {
            TipUtil.show(R.string.imsdk_sample_input_error_avatar_empty);
            return;
        }

        SampleLog.v(
                "submit with userId:%s, nickname:%s, avatar:%s",
                signUpArgument.userId,
                signUpArgument.nickname,
                signUpArgument.avatar);

        mPresenter.requestSignUp(
                signUpArgument.userId,
                signUpArgument.nickname,
                signUpArgument.avatar
        );
    }

    class ViewImpl extends SignUpView {

        @Nullable
        @Override
        protected Activity getActivity() {
            return SignUpAvatarFragment.this.getActivity();
        }

        @Nullable
        @Override
        protected SignInViewPresenter<?> getPresenter() {
            return SignUpAvatarFragment.this.mPresenter;
        }

        @NonNull
        @Override
        public SignUpArgument getSignUpArgument() {
            return SignUpAvatarFragment.this.getSignUpArgument();
        }

        public void onAvatarUploadFail(Throwable e) {
            SampleLog.v(e, Objects.defaultObjectTag(this) + " onAvatarUploadFail");
            if (mBinding == null) {
                SampleLog.e(IMUIKitConstants.ErrorLog.BINDING_IS_NULL);
            }
            mBinding.progressView.setProgress(0f);
        }

        public void onAvatarUploadProgress(@IntRange(from = 0, to = 100) int percent) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAvatarUploadProgress percent:%s", percent);
            if (mBinding == null) {
                SampleLog.e(IMUIKitConstants.ErrorLog.BINDING_IS_NULL);
            }
            mBinding.progressView.setProgress(percent / 100f);
        }

        public void onAvatarUploadSuccess(String avatarUrl) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAvatarUploadSuccess avatarUrl:%s", avatarUrl);

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            final SignUpArgument signUpArgument = getSignUpArgument();
            signUpArgument.avatar = avatarUrl;
            saveSignUpArgument();

            validateSubmitState();
        }

        public void onSignUpFail(int code, String message) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onSignUpFail code:%s, message:%s", code, message);

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            TipUtil.showOrDefault(message);
        }

        public void onSignUpFail(Throwable e) {
            SampleLog.v(e, Objects.defaultObjectTag(this) + " onSignUpFail");

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            TipUtil.show(R.string.imsdk_sample_tip_text_error_unknown);
        }

        public void onSignUpSuccess(long userId) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onSignUpSuccess userId:%s", userId);

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            mPresenter.requestToken(userId);
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
