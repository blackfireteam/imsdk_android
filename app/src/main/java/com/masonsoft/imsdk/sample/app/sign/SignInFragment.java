package com.masonsoft.imsdk.sample.app.sign;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.main.MainActivity;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSignInFragmentBinding;

public class SignInFragment extends Fragment {

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
        return mBinding.getRoot();
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
