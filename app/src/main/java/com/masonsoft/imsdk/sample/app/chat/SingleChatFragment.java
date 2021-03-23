package com.masonsoft.imsdk.sample.app.chat;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.masonsoft.imsdk.core.IMConstants.ConversationType;
import com.masonsoft.imsdk.sample.Constants;

/**
 * 单聊页面
 *
 * @see ConversationType#C2C
 */
public class SingleChatFragment extends Fragment {

    public static SingleChatFragment newInstance(long targetUserId) {
        Bundle args = new Bundle();
        args.putLong(Constants.ExtrasKey.TARGET_USER_ID, targetUserId);
        SingleChatFragment fragment = new SingleChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private long mTargetUserId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if (args != null) {
            mTargetUserId = args.getLong(Constants.ExtrasKey.TARGET_USER_ID, mTargetUserId);
        }
    }

}
