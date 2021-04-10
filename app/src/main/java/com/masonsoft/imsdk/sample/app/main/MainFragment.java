package com.masonsoft.imsdk.sample.app.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.app.conversation.ConversationFragment;
import com.masonsoft.imsdk.sample.app.discover.DiscoverFragment;
import com.masonsoft.imsdk.sample.app.mine.MineFragment;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleMainFragmentBinding;

import io.github.idonans.systeminsets.SystemInsetsLayout;

public class MainFragment extends SystemInsetsFragment {

    public static MainFragment newInstance() {
        Bundle args = new Bundle();
        MainFragment fragment = new MainFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private ImsdkSampleMainFragmentBinding mBinding;
    private final int[] mTitleResIds = {
            R.string.imsdk_sample_tab_discover,
            R.string.imsdk_sample_tab_conversation,
            R.string.imsdk_sample_tab_mine
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = ImsdkSampleMainFragmentBinding.inflate(inflater, container, false);
        mBinding.pager.setOffscreenPageLimit(3);
        mBinding.pager.setAdapter(new DataAdapter());
        mBinding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                syncTabSelected(position);
            }
        });
        mBinding.mainBottomBar.setOnTabClickListener(this::syncTabSelected);
        syncTabSelected(0);

        mBinding.mainTopBarContainer.setOnSystemInsetsListener(new SystemInsetsLayout.OnSystemInsetsListener() {
            @Override
            public void onSystemInsets(int left, int top, int right, int bottom) {
                SampleLog.v("mainTopBarContainer onSystemInsets: left:%s, top:%s, right:%s, bottom:%s", left, top, right, bottom);
            }
        });
        mBinding.mainBottomBarContainer.setOnSystemInsetsListener((left, top, right, bottom) ->
                SampleLog.v("mainBottomBarContainer onSystemInsets: left:%s, top:%s, right:%s, bottom:%s", left, top, right, bottom)
        );

        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mBinding = null;
    }

    private void syncTabSelected(int index) {
        SampleLog.v("syncTabSelected index:%s", index);
        if (mBinding != null) {
            mBinding.mainTopBar.setTitle(getString(mTitleResIds[index]));
            if (mBinding.pager.getCurrentItem() != index) {
                mBinding.pager.setCurrentItem(index, false);
            }
            if (mBinding.mainBottomBar.getCurrentItem() != index) {
                mBinding.mainBottomBar.setCurrentItem(index);
            }
        }
    }

    private class DataAdapter extends FragmentStateAdapter {
        public DataAdapter() {
            super(MainFragment.this);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return DiscoverFragment.newInstance();
            } else if (position == 1) {
                return ConversationFragment.newInstance();
            } else if (position == 2) {
                return MineFragment.newInstance();
            } else {
                throw new IllegalArgumentException("unexpected position:" + position);
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }

}
