package com.masonsoft.imsdk.sample.app.sign;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.FormValidator;
import com.idonans.core.util.ToastUtil;
import com.idonans.lang.util.ViewUtil;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.app.main.MainActivity;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSignInFragmentBinding;

public class SignInFragment extends SystemInsetsFragment {

    public static SignInFragment newInstance() {
        Bundle args = new Bundle();
        SignInFragment fragment = new SignInFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private ImsdkSampleSignInFragmentBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = ImsdkSampleSignInFragmentBinding.inflate(inflater, container, false);
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
        ViewUtil.onClick(mBinding.submit, v -> onSubmit());

        return mBinding.getRoot();
    }

    private void onSubmit() {
        SampleLog.v("onSubmit");

        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.Tip.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(Constants.Tip.BINDING_IS_NULL);
            return;
        }

        final String phone = mBinding.editText.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            ToastUtil.show(getString(R.string.imsdk_sample_input_error_empty_phone));
            return;
        }

        SampleLog.v("submit with phone:%s", phone);

        //////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////
        // TODO 临时使用固定的测试信息登录
        final String debugToken = "9999999999999";
        final String debugAesKey = null;
        final String debugHost = "192.168.50.254";
        final int debugPort = 18788;
        final Session session = new Session(
                debugToken,
                debugAesKey,
                debugHost,
                debugPort
        );
        IMSessionManager.getInstance().setSession(session);

        onSignInSuccess();
    }

    private void onSignInSuccess() {
        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.Tip.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        MainActivity.start(activity);
        activity.finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

}
