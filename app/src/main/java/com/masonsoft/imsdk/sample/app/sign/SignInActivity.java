package com.masonsoft.imsdk.sample.app.sign;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.idonans.systeminsets.SystemUiHelper;
import com.masonsoft.imsdk.sample.app.FragmentDelegateActivity;

/**
 * 登录
 */
public class SignInActivity extends FragmentDelegateActivity {

    public static void start(Context context) {
        Intent starter = new Intent(context, SignInActivity.class);
        context.startActivity(starter);
    }

    private static final String FRAGMENT_TAG_SIGN_IN = "fragment_sign_in_20210322";

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

        setFragmentDelegate(FRAGMENT_TAG_SIGN_IN, SignInFragment::newInstance);
    }

}
