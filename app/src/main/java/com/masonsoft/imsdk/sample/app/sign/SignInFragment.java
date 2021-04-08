package com.masonsoft.imsdk.sample.app.sign;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.SimpleAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.core.FormValidator;
import com.idonans.core.util.ToastUtil;
import com.idonans.lang.util.ViewUtil;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.app.main.MainActivity;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSignInFragmentBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignInFragment extends SystemInsetsFragment {

    public static SignInFragment newInstance() {
        Bundle args = new Bundle();
        SignInFragment fragment = new SignInFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private static final String TEST_LOCAL_HOST = "192.168.50.254";

    private static final String[] TEST_TOKEN_ARRAY = new String[]{
            "mz4ERZKaqS+Exjie4R5BsQ==",
            "jCTYRM47p2PrZljH2tT4rw==",
            "89g3Is+0vDBz7grDz95N4A==",
            "knZxEr48p+kb70wIHxin9A==",
            "lxmxSxIG9jIJWyruS08tsg==",
            "jVUJjCbO+FyIYjv1PxWhmg==",
            "M5nMDVU9Qe76BJQcTMP8lA==",
            "d1rpzZ7RnvGyToVDws6cTQ==",
            "fc2d4I046hsRSF9WpQ9NnA==",
            "oL/jnE5vydk4a2ixCjHrTg==",
            "CxY7P2HxAwxQc6Hue1Z3mg==",
            "wlQeVQUdXZV0GZmzAzbi6g==",
            "oywBohgtkR3zy4DxxH3LbA==",
            "KMpJ5ze/J8hhrGqqGYrY6Q==",
            "O7T63s8SwMY15f8RWAEuDQ==",
            "VBXDOtpNJj8qp9n31RBdTg==",
            "xJcfcPTLTYFCsabUVsDvLA==",
            "rg3Pdti9iIWYc6NGL9VO+g==",
            "rdb3ga+2v7fhKhztucl35A==",
            "JmokHbYSC5b4VhyayugQ6g==",
            "5qht/6ypb3K6xufiyxuIyQ==",
            "kdkwm3bSpeBPjrBfDP0E0g==",
            "T3AmDPOTp7smtGUElDRw/A==",
            "xE07ucLL8oqSjyJp/GCihw==",
            "9iKKSMm03UCXuzK5s1QlDw==",
            "qZIEE8URcxOrvx9TVvfb4Q==",
            "t/Ko8M0d5TVQwepmxJ9WlA==",
            "AhybVVgbCrzFqvJFcQX1rQ==",
            "agVLNAtKJr6Mbp3WkAxKCA==",
            "DDn2LtKrC2jKRgZP6UAjXg==",
            "QiV7xXHsLoSv4XRf+kUdhg==",
            "HMJqyPTi17ZH5zJ5eKBPwA==",
            "VBvAHrJxQIO49qFQg9CmbQ==",
            "9aITYzqDL+da+hp2cY8ekA==",
            "XKULrMkiqHUgPSx3B6mrTQ==",
            "AK7yTvHuta4JhbCyAsjKsA==",
            "p+u1Y/Jlh3krLrIQX9b4Hg==",
            "p5t9wEXTfrOhNvpa6AK5IA==",
    };
    private static final List<Map<String, Object>> TEST_TOKEN_MAP_LIST = new ArrayList<>();

    static {
        int index = 0;
        for (String token : TEST_TOKEN_ARRAY) {
            final Map<String, Object> map = new HashMap<>();
            map.put("token", token);
            map.put("id", ++index);
            TEST_TOKEN_MAP_LIST.add(map);
        }
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
                        FormValidator.InputViewFactory.create(mBinding.localHost),
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
        mBinding.localHost.setText(TEST_LOCAL_HOST);
        mBinding.tokenSpinner.setAdapter(new SimpleAdapter(
                mBinding.tokenSpinner.getContext(),
                TEST_TOKEN_MAP_LIST,
                android.R.layout.simple_list_item_2,
                new String[]{"token", "id",},
                new int[]{android.R.id.text1, android.R.id.text2,}
        ));
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

        final String phone = mBinding.editText.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            ToastUtil.show(I18nResources.getString(R.string.imsdk_sample_input_error_empty_phone));
            return;
        }
        final String localHost = mBinding.localHost.getText().toString().trim();
        if (TextUtils.isEmpty(localHost)) {
            ToastUtil.show(I18nResources.getString(R.string.imsdk_sample_input_error_empty_local_host));
            return;
        }
        final boolean internetSwitch = mBinding.internetSwitch.isChecked();
        final String token = (String) ((Map<?, ?>) mBinding.tokenSpinner.getSelectedItem()).get("token");

        SampleLog.v(
                "submit with phone:%s, internetSwitch:%s, token:%s",
                phone,
                internetSwitch,
                token);

        //////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////
        // TODO 临时使用固定的测试信息登录
        //noinspection UnnecessaryLocalVariable
        final String debugToken = token;
        final String debugAesKey = null;
        final String debugHost = internetSwitch ? "im.ekfree.com" : localHost;
        final int debugPort = 18888;
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
            SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
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
