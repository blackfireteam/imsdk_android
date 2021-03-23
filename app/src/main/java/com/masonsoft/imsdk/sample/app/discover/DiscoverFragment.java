package com.masonsoft.imsdk.sample.app.discover;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.idonans.lang.util.ViewUtil;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.chat.SingleChatActivity;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleDiscoverFragmentBinding;

/**
 * 发现
 */
public class DiscoverFragment extends Fragment {

    public static DiscoverFragment newInstance() {
        Bundle args = new Bundle();
        DiscoverFragment fragment = new DiscoverFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private ImsdkSampleDiscoverFragmentBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = ImsdkSampleDiscoverFragmentBinding.inflate(inflater, container, false);

        ViewUtil.onClick(mBinding.user100, v -> {
            startSingleChat(100L);
        });
        ViewUtil.onClick(mBinding.user101, v -> {
            startSingleChat(101L);
        });
        ViewUtil.onClick(mBinding.user102, v -> {
            startSingleChat(102L);
        });

        return mBinding.getRoot();
    }

    private void startSingleChat(long targetUserId) {
        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.Tip.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(Constants.Tip.BINDING_IS_NULL);
            return;
        }

        SingleChatActivity.start(activity, targetUserId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

}
