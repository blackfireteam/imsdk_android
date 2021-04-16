package com.masonsoft.imsdk.sample.app.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleHomeFragmentBinding;

import io.github.idonans.dynamic.page.UnionTypeStatusPageView;
import io.github.idonans.systeminsets.SystemInsetsLayout;
import io.github.idonans.uniontype.UnionTypeAdapter;

/**
 * 首页
 */
public class HomeFragment extends SystemInsetsFragment {

    public static HomeFragment newInstance() {
        Bundle args = new Bundle();
        HomeFragment fragment = new HomeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private ImsdkSampleHomeFragmentBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SampleLog.v("onCreateView %s", getClass());

        mBinding = ImsdkSampleHomeFragmentBinding.inflate(inflater, container, false);
        mBinding.topSystemInsets.setOnSystemInsetsListener(new SystemInsetsLayout.OnSystemInsetsListener() {
            @Override
            public void onSystemInsets(int left, int top, int right, int bottom) {
                SampleLog.v("HomeFragment topSystemInsets onSystemInsets left:%s, top:%s, right:%s, bottom:%s", left, top, right, bottom);
            }
        });
        mBinding.bottomSystemInsets.setOnSystemInsetsListener(new SystemInsetsLayout.OnSystemInsetsListener() {
            @Override
            public void onSystemInsets(int left, int top, int right, int bottom) {
                SampleLog.v("HomeFragment bottomSystemInsets onSystemInsets left:%s, top:%s, right:%s, bottom:%s", left, top, right, bottom);
            }
        });

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final RecyclerView recyclerView = mBinding.recyclerView;

    }

    class ViewImpl extends UnionTypeStatusPageView {

        public ViewImpl(@NonNull UnionTypeAdapter adapter) {
            super(adapter, true);
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

}
