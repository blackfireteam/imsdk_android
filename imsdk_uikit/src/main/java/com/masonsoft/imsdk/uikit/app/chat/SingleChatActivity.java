package com.masonsoft.imsdk.uikit.app.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.masonsoft.imsdk.uikit.IMUIKitConstants;
import com.masonsoft.imsdk.uikit.app.FragmentDelegateActivity;

import io.github.idonans.systeminsets.SystemUiHelper;

public class SingleChatActivity extends FragmentDelegateActivity {

    public static void start(Context context, long targetUserId) {
        Intent starter = new Intent(context, SingleChatActivity.class);
        starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        starter.putExtra(IMUIKitConstants.ExtrasKey.TARGET_USER_ID, targetUserId);
        context.startActivity(starter);
    }

    private static final String FRAGMENT_TAG_SINGLE_CHAT = "fragment_single_chat_20210323";

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

        final long targetUserId = getIntent().getLongExtra(IMUIKitConstants.ExtrasKey.TARGET_USER_ID, 0L);
        setFragmentDelegate(FRAGMENT_TAG_SINGLE_CHAT, () -> SingleChatFragment.newInstance(targetUserId));
    }

}
