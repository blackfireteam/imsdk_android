package com.masonsoft.imsdk.sample.app.signup.avatar;

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

import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.signin.SignInViewPresenter;
import com.masonsoft.imsdk.sample.app.signup.SignUpArgument;
import com.masonsoft.imsdk.sample.app.signup.SignUpFragment;
import com.masonsoft.imsdk.sample.app.signup.SignUpView;
import com.masonsoft.imsdk.sample.common.imagepicker.ImageData;
import com.masonsoft.imsdk.sample.common.imagepicker.ImagePickerDialog;
import com.masonsoft.imsdk.sample.common.imagepicker.ImageSelector;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSignUpAvatarFragmentBinding;
import com.masonsoft.imsdk.sample.util.TipUtil;
import com.masonsoft.imsdk.util.Objects;
import com.masonsoft.imsdk.util.Preconditions;

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

    @Nullable
    private ImsdkSampleSignUpAvatarFragmentBinding mBinding;
    private ViewImpl mView;
    private SignUpAvatarFragmentPresenter mPresenter;

    private void validateSubmitState() {
        if (mBinding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
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

        validateSubmitState();

        ViewUtil.onClick(mBinding.pickAvatar, v -> onPickAvatar());
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

    private void onPickAvatar() {
        SampleLog.v("onPickAvatar");

        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        final ImagePickerDialog imagePickerDialog = new ImagePickerDialog(activity, activity.findViewById(Window.ID_ANDROID_CONTENT));
        imagePickerDialog.setImageSelector(new ImageSelector.SimpleImageSelector());
        imagePickerDialog.setOnImagePickListener(imageInfoList -> {
            if (imageInfoList.isEmpty()) {
                return false;
            }

            final ImageData.ImageInfo imageInfo = imageInfoList.get(0);
            onPickAvatarResult(imageInfo.uri);
            return true;
        });
        imagePickerDialog.show();
    }

    private void onPickAvatarResult(final Uri uri) {
        SampleLog.v("onPickAvatarResult uri:%s", uri);

        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
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

        mBinding.pickAvatar.setUrl(uri.toString());
        mPresenter.uploadAvatar(uri);
    }

    private void onSubmit() {
        SampleLog.v("onSubmit");

        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
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
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            }
            mBinding.progressView.setProgress(0f);
        }

        public void onAvatarUploadProgress(@IntRange(from = 0, to = 100) int percent) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAvatarUploadProgress percent:%s", percent);
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            }
            mBinding.progressView.setProgress(percent / 100f);
        }

        public void onAvatarUploadSuccess(String avatarUrl) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAvatarUploadSuccess avatarUrl:%s", avatarUrl);

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
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
                SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            TipUtil.showOrDefault(message);
        }

        public void onSignUpFail(Throwable e) {
            SampleLog.v(e, Objects.defaultObjectTag(this) + " onSignUpFail");

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            TipUtil.show(R.string.imsdk_sample_tip_text_error_unknown);
        }

        public void onSignUpSuccess(long userId) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onSignUpSuccess userId:%s", userId);

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
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
