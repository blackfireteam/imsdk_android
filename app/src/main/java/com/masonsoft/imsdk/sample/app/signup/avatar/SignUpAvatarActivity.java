package com.masonsoft.imsdk.sample.app.signup.avatar;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.sample.app.FragmentDelegateActivity;
import com.masonsoft.imsdk.sample.app.signup.SignUpArgument;

import io.github.idonans.systeminsets.SystemUiHelper;

/**
 * 注册-设置头像
 */
public class SignUpAvatarActivity extends FragmentDelegateActivity {

    public static void start(Context context, @Nullable SignUpArgument signUpArgument) {
        Intent starter = new Intent(context, SignUpAvatarActivity.class);
        starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (signUpArgument != null) {
            signUpArgument.writeTo(starter);
        }
        context.startActivity(starter);
    }

    private static final String FRAGMENT_TAG_SIGN_UP_AVATAR = "fragment_sign_up_avatar_20210421";

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

        setFragmentDelegate(FRAGMENT_TAG_SIGN_UP_AVATAR,
                () -> SignUpAvatarFragment.newInstance(SignUpArgument.valueOf(getIntent())));
    }

}
