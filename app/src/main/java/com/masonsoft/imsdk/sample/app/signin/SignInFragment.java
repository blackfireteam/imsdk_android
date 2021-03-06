package com.masonsoft.imsdk.sample.app.signin;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.lang.GeneralResult;
import com.masonsoft.imsdk.sample.LocalSettingsManager;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.uikit.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSignInFragmentBinding;
import com.masonsoft.imsdk.uikit.IMUIKitConstants;
import com.masonsoft.imsdk.uikit.common.simpledialog.SimpleContentConfirmDialog;
import com.masonsoft.imsdk.uikit.common.simpledialog.SimpleLoadingDialog;
import com.masonsoft.imsdk.uikit.util.TipUtil;
import com.masonsoft.imsdk.util.Objects;

import io.github.idonans.core.FormValidator;
import io.github.idonans.core.util.Preconditions;
import io.github.idonans.core.util.SystemUtil;
import io.github.idonans.core.util.ToastUtil;
import io.github.idonans.lang.util.ViewUtil;

public class SignInFragment extends SystemInsetsFragment {

    public static SignInFragment newInstance() {
        Bundle args = new Bundle();
        SignInFragment fragment = new SignInFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private static final String DEFAULT_API_SERVER_INTERNET = "https://im.ekfree.com:18788";
    private static final String DEFAULT_IM_SERVER_INTERNET = "im.ekfree.com:18888";
    private static final String DEFAULT_API_SERVER_LOCAL = "https://192.168.50.189:18788";
    private static final String DEFAULT_IM_SERVER_LOCAL = "192.168.50.189:18888";
    private boolean mCurrentApiServerInternet = true;

    @Nullable
    private ImsdkSampleSignInFragmentBinding mBinding;
    private ViewImpl mView;
    private SignInFragmentPresenter mPresenter;
    @Nullable
    private SimpleLoadingDialog mSignInLoadingDialog;

    private void showSignInLoadingDialog() {
        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_IS_NULL);
            return;
        }
        if (isStateSaved()) {
            SampleLog.e(IMUIKitConstants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
            return;
        }
        if (mSignInLoadingDialog == null) {
            mSignInLoadingDialog = new SimpleLoadingDialog(activity);
        }
        mSignInLoadingDialog.show();
    }

    private void hideSignInLoadingDialog() {
        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_IS_NULL);
            return;
        }
        if (isStateSaved()) {
            SampleLog.e(IMUIKitConstants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
            return;
        }
        if (mSignInLoadingDialog != null) {
            mSignInLoadingDialog.hide();
        }
    }

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
        if (!TextUtils.isEmpty(settings.apiServer)) {
            // 猜测内外网
            mCurrentApiServerInternet = !settings.apiServer.contains("192.168");
        }
        refreshShowCurrentApiServer();
        mBinding.settingsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ViewUtil.setVisibilityIfChanged(mBinding.resetServer, View.VISIBLE);
                ViewUtil.setVisibilityIfChanged(mBinding.apiServer, View.VISIBLE);
                ViewUtil.setVisibilityIfChanged(mBinding.imServer, View.VISIBLE);
            } else {
                ViewUtil.setVisibilityIfChanged(mBinding.resetServer, View.GONE);
                ViewUtil.setVisibilityIfChanged(mBinding.apiServer, View.GONE);
                ViewUtil.setVisibilityIfChanged(mBinding.imServer, View.GONE);
            }
        });

        ViewUtil.onClick(mBinding.resetServer, v -> {
            mCurrentApiServerInternet = !mCurrentApiServerInternet;
            refreshShowCurrentApiServer();
        });

        ViewUtil.onClick(mBinding.submit, v -> onSubmit());
        ViewUtil.onClick(mBinding.openAppSettings, v -> openAppSettings());

        return mBinding.getRoot();
    }

    private void refreshShowCurrentApiServer() {
        if (mBinding == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.BINDING_IS_NULL);
            return;
        }
        if (mCurrentApiServerInternet) {
            mBinding.apiServer.setText(DEFAULT_API_SERVER_INTERNET);
            mBinding.imServer.setText(DEFAULT_IM_SERVER_INTERNET);
        } else {
            mBinding.apiServer.setText(DEFAULT_API_SERVER_LOCAL);
            mBinding.imServer.setText(DEFAULT_IM_SERVER_LOCAL);
        }
    }

    private void openAppSettings() {
        try {
            SampleLog.v("openAppSettings");

            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getContext().getPackageName()));
            startActivity(intent);
        } catch (Throwable e) {
            SampleLog.e(e);
        }
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
            SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        final String phone = mBinding.editText.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            TipUtil.show(R.string.imsdk_sample_input_error_empty_phone);
            return;
        }
        final long phoneAsLong;
        try {
            phoneAsLong = Long.parseLong(phone);
            Preconditions.checkArgument(phoneAsLong > 0);
        } catch (Throwable e) {
            TipUtil.show(R.string.imsdk_sample_input_error_invalid_phone);
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

        showSignInLoadingDialog();
        mPresenter.requestToken(phoneAsLong);
    }

    private void showAutoRegConfirmDialog(final long userId) {
        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(IMUIKitConstants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        hideSignInLoadingDialog();
        SystemUtil.hideSoftKeyboard(mBinding.editText);
        final SimpleContentConfirmDialog dialog = new SimpleContentConfirmDialog(
                activity,
                I18nResources.getString(R.string.imsdk_sample_dialog_confirm_user_not_found_and_auto_reg)
        );
        dialog.setOnBtnRightClickListener(() -> {
            if (mView != null) {
                mView.onRequestSignUp(userId);
            }
        });
        dialog.show();
    }

    class ViewImpl extends SignInView {

        @Nullable
        @Override
        protected Activity getActivity() {
            return SignInFragment.this.getActivity();
        }

        @Nullable
        @Override
        protected SignInViewPresenter<?> getPresenter() {
            return SignInFragment.this.mPresenter;
        }

        @Override
        public void onTcpSignInFail(GeneralResult result) {
            super.onTcpSignInFail(result);
            hideSignInLoadingDialog();
        }

        @Override
        public void onTcpSignInFail(Throwable e) {
            super.onTcpSignInFail(e);
            hideSignInLoadingDialog();
        }

        @Override
        public void onFetchTokenFail(Throwable e, long userId) {
            super.onFetchTokenFail(e, userId);
            hideSignInLoadingDialog();
        }

        @Override
        public void onFetchTokenFail(long userId, int code, String message) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onFetchTokenFail userId:%s, code:%s, message:%s", userId, code, message);

            final Activity activity = getActivity();
            if (activity == null) {
                SampleLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
                return;
            }

            if (code == 9) {
                // 用户未注册
                showAutoRegConfirmDialog(userId);
                return;
            }

            hideSignInLoadingDialog();
            TipUtil.showOrDefault(message);
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
