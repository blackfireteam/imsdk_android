package com.masonsoft.imsdk.sample.app.chat;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.idonans.lang.util.ViewUtil;
import com.masonsoft.imsdk.core.IMConstants.ConversationType;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSingleChatFragmentBinding;
import com.masonsoft.imsdk.sample.util.BackStackUtil;

/**
 * 单聊页面
 *
 * @see ConversationType#C2C
 */
public class SingleChatFragment extends SystemInsetsFragment {

    public static SingleChatFragment newInstance(long targetUserId) {
        Bundle args = new Bundle();
        args.putLong(Constants.ExtrasKey.TARGET_USER_ID, targetUserId);
        SingleChatFragment fragment = new SingleChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private long mTargetUserId;
    @Nullable
    private ImsdkSampleSingleChatFragmentBinding mBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if (args != null) {
            mTargetUserId = args.getLong(Constants.ExtrasKey.TARGET_USER_ID, mTargetUserId);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = ImsdkSampleSingleChatFragmentBinding.inflate(inflater, container, false);

        ViewUtil.onClick(mBinding.topBarBack, v -> BackStackUtil.requestBackPressed(SingleChatFragment.this));
        mBinding.topBarTitle.setTargetUserId(mTargetUserId);

        mBinding.keyboardEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3000)});

        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

}
