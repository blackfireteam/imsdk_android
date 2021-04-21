package com.masonsoft.imsdk.sample.app.signup.nickname;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.app.signup.SignUpArgument;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSignUpNicknameFragmentBinding;

import io.github.idonans.core.FormValidator;
import io.github.idonans.core.util.ToastUtil;
import io.github.idonans.lang.util.ViewUtil;

public class SignUpNicknameFragment extends SystemInsetsFragment {

    public static SignUpNicknameFragment newInstance(@Nullable SignUpArgument signUpArgument) {
        Bundle args = new Bundle();
        if (signUpArgument != null) {
            signUpArgument.writeTo(args);
        }
        SignUpNicknameFragment fragment = new SignUpNicknameFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private SignUpArgument mSignUpArgument;

    @Nullable
    private ImsdkSampleSignUpNicknameFragmentBinding mBinding;

    private void saveSignUpArgument() {
        if (isStateSaved()) {
            SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
            return;
        }

        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        if (mSignUpArgument != null) {
            mSignUpArgument.writeTo(args);
        }
        setArguments(args);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSignUpArgument = SignUpArgument.valueOf(getArguments());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = ImsdkSampleSignUpNicknameFragmentBinding.inflate(inflater, container, false);
        FormValidator.bind(
                new FormValidator.InputView[]{
                        FormValidator.InputViewFactory.create(mBinding.editText),
                },
                new FormValidator.SubmitView[]{
                        FormValidator.SubmitViewFactory.create(mBinding.submit),
                }
        );
        mBinding.editText.setOnEditorActionListener((v, actionId, event) -> {
            SampleLog.v("onEditorAction actionId:%s, event:%s", actionId, event);
            if (actionId == EditorInfo.IME_ACTION_GO) {
                onSubmit();
                return true;
            }
            return false;
        });

        if (mSignUpArgument != null) {
            mBinding.editText.setText(mSignUpArgument.nickname);
        }
        ViewUtil.onClick(mBinding.submit, v -> onSubmit());

        return mBinding.getRoot();
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

        final String nickname = mBinding.editText.getText().toString().trim();
        final int minLength = 3;
        final int maxLength = 20;
        if (TextUtils.isEmpty(nickname)
                || nickname.length() < minLength
                || nickname.length() > maxLength) {
            ToastUtil.show(I18nResources.getString(R.string.imsdk_sample_input_error_nickname_error));
            return;
        }

        if (mSignUpArgument == null) {
            mSignUpArgument = new SignUpArgument();
        }
        mSignUpArgument.nickname = nickname;
        saveSignUpArgument();

        // TODO
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

}
