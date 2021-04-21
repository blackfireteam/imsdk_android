package com.masonsoft.imsdk.sample.app.signin;

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
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.LocalSettingsManager;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.app.main.MainActivity;
import com.masonsoft.imsdk.sample.app.signup.SignUpActivity;
import com.masonsoft.imsdk.sample.app.signup.SignUpArgument;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSignInFragmentBinding;
import com.masonsoft.imsdk.sample.util.TipUtil;
import com.masonsoft.imsdk.util.Objects;
import com.masonsoft.imsdk.util.Preconditions;

import io.github.idonans.core.FormValidator;
import io.github.idonans.core.util.ToastUtil;
import io.github.idonans.dynamic.DynamicView;
import io.github.idonans.lang.util.ViewUtil;

public class SignInFragment extends SystemInsetsFragment {

    public static SignInFragment newInstance() {
        Bundle args = new Bundle();
        SignInFragment fragment = new SignInFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private ImsdkSampleSignInFragmentBinding mBinding;
    private ViewImpl mView;
    private SignInFragmentPresenter mPresenter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = ImsdkSampleSignInFragmentBinding.inflate(inflater, container, false);
        FormValidator.bind(
                new FormValidator.InputView[]{
                        FormValidator.InputViewFactory.create(mBinding.editText),
                        FormValidator.InputViewFactory.create(mBinding.apiServer),
                        FormValidator.InputViewFactory.create(mBinding.imServer),
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

        final LocalSettingsManager.Settings settings = LocalSettingsManager.getInstance().getSettings();
        mBinding.apiServer.setText(settings.apiServer);
        mBinding.imServer.setText(String.valueOf(settings.imServer));
        mBinding.settingsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ViewUtil.setVisibilityIfChanged(mBinding.apiServer, View.VISIBLE);
                ViewUtil.setVisibilityIfChanged(mBinding.imServer, View.VISIBLE);
            } else {
                ViewUtil.setVisibilityIfChanged(mBinding.apiServer, View.GONE);
                ViewUtil.setVisibilityIfChanged(mBinding.imServer, View.GONE);
            }
        });
        ViewUtil.onClick(mBinding.submit, v -> onSubmit());

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Preconditions.checkNotNull(mBinding);
        mView = new ViewImpl();
        clearPresenter();
        mPresenter = new SignInFragmentPresenter(mView);
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

        final String phone = mBinding.editText.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            ToastUtil.show(I18nResources.getString(R.string.imsdk_sample_input_error_empty_phone));
            return;
        }
        final String apiServer = mBinding.apiServer.getText().toString().trim().toLowerCase();
        if (TextUtils.isEmpty(apiServer)
                || !apiServer.startsWith("https://")
                || !apiServer.contains(":")) {
            ToastUtil.show(I18nResources.getString(R.string.imsdk_sample_input_error_api_server_error));
            return;
        }
        final String imServer = mBinding.imServer.getText().toString().trim().toLowerCase();
        if (TextUtils.isEmpty(imServer)
                || imServer.contains("://")
                || !imServer.contains(":")) {
            ToastUtil.show(I18nResources.getString(R.string.imsdk_sample_input_error_im_server_error));
        }

        final LocalSettingsManager.Settings settings = LocalSettingsManager.getInstance().getSettings();
        try {
            settings.apiServer = apiServer;
            settings.imServer = imServer;
            settings.imToken = null;
            LocalSettingsManager.getInstance().setSettings(settings);
        } catch (Throwable e) {
            SampleLog.e(e);
            ToastUtil.show(e.getMessage());
            return;
        }

        SampleLog.v(
                "submit with phone:%s, apiServer:%s, imServer:%s",
                phone,
                settings.apiServer,
                settings.imServer);

        mPresenter.requestToken(phone);
    }

    class ViewImpl implements DynamicView {
        private void onRequestSignUp(final String phone) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onRequestSignUp phone:%s", phone);
            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            final long targetUserId = Long.parseLong(phone);
            final SignUpArgument signUpArgument = new SignUpArgument();
            signUpArgument.targetUserId = targetUserId;
            SignUpActivity.start(activity, signUpArgument);
        }

        public void onFetchTokenSuccess(String token) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onFetchTokenSuccess token:%s", token);

            mPresenter.requestTcpSignIn(token);
        }

        public void onFetchTokenFail(String phone, int code, String message) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onFetchTokenFail phone:%s, code:%s, message:%s", phone, code, message);

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            if (code == 9) {
                // 用户未注册
                onRequestSignUp(phone);
                return;
            }

            TipUtil.showOrDefault(message);
        }

        public void onFetchTokenFail(Throwable e, String phone) {
            SampleLog.v(e, Objects.defaultObjectTag(this) + " onFetchTokenFail phone:%s", phone);

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            TipUtil.show(R.string.imsdk_sample_tip_text_error_unknown);
        }

        public void onTcpSignInSuccess() {
            SampleLog.v(Objects.defaultObjectTag(this) + " onTcpSignInSuccess");

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            MainActivity.start(activity);
            activity.finish();
        }

        public void onTcpSignInFail(GeneralResult result) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onTcpSignInFail GeneralResult:%s", result);

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            if (result.subResult != null) {
                TipUtil.showOrDefault(result.subResult.message);
            } else {
                TipUtil.showOrDefault(result.message);
            }
        }

        public void onTcpSignInFail(Throwable e) {
            SampleLog.v(e, Objects.defaultObjectTag(this) + " onTcpSignInFail");

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            TipUtil.show(R.string.imsdk_sample_tip_text_error_unknown);
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
