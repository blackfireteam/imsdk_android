package com.masonsoft.imsdk.sample.app.discover;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.idonans.lang.util.ViewUtil;
import com.idonans.systeminsets.SystemInsetsLayout;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.app.chat.SingleChatActivity;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleDiscoverFragmentBinding;

/**
 * 发现
 */
public class DiscoverFragment extends SystemInsetsFragment {

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
        SampleLog.v("onCreateView %s", getClass());

        mBinding = ImsdkSampleDiscoverFragmentBinding.inflate(inflater, container, false);
        mBinding.topSystemInsets.setOnSystemInsetsListener(new SystemInsetsLayout.OnSystemInsetsListener() {
            @Override
            public void onSystemInsets(int left, int top, int right, int bottom) {
                SampleLog.v("DiscoverFragment topSystemInsets onSystemInsets left:%s, top:%s, right:%s, bottom:%s", left, top, right, bottom);
            }
        });
        testInflateTargetUsers(mBinding.scrollContent);

        return mBinding.getRoot();
    }

    private void testInflateTargetUsers(ViewGroup parent) {
        for (int i = 1; i < 101; i++) {
            testInflateTargetUsers(parent, i);
        }
    }

    @SuppressLint("SetTextI18n")
    private void testInflateTargetUsers(ViewGroup parent, long userId) {
        final AppCompatButton button = new AppCompatButton(parent.getContext());
        button.setText("user:" + userId);
        parent.addView(button);
        ViewUtil.onClick(button, v -> startSingleChat(userId));
    }

    private void startSingleChat(long targetUserId) {
        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
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
