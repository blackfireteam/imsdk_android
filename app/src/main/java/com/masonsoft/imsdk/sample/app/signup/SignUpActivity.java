package com.masonsoft.imsdk.sample.app.signup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.app.FragmentDelegateActivity;

import io.github.idonans.systeminsets.SystemUiHelper;

/**
 * 注册
 */
public class SignUpActivity extends FragmentDelegateActivity {

    public static void start(Context context, long targetUserId) {
        Intent starter = new Intent(context, SignUpActivity.class);
        starter.putExtra(Constants.ExtrasKey.TARGET_USER_ID, targetUserId);
        starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(starter);
    }

    private static final String FRAGMENT_TAG_SIGN_UP = "fragment_sign_up_20210421";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SystemUiHelper.from(getWindow())
                .layoutStatusBar()
                .layoutNavigationBar()
                .layoutStable()
                .setLightStatusBar()
                .setLightNavigationBar()
                .apply();

        final long targetUserId = getIntent().getLongExtra(Constants.ExtrasKey.TARGET_USER_ID, 0);
        setFragmentDelegate(FRAGMENT_TAG_SIGN_UP, () -> SignUpFragment.newInstance(targetUserId));
    }

}
