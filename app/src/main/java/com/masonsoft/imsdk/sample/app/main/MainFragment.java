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

import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.conversation.ConversationFragment;
import com.masonsoft.imsdk.sample.app.discover.DiscoverFragment;
import com.masonsoft.imsdk.sample.app.mine.MineFragment;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleMainFragmentBinding;

public class MainFragment extends Fragment {

    public static MainFragment newInstance() {
        Bundle args = new Bundle();
        MainFragment fragment = new MainFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private ImsdkSampleMainFragmentBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = ImsdkSampleMainFragmentBinding.inflate(inflater, container, false);
        mBinding.pager.setAdapter(new DataAdapter());
        mBinding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                onMainPageSelected(position);
            }
        });
        mBinding.mainBottomBar.setOnTabClickListener(this::onMainTabClick);

        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mBinding = null;
    }

    private void onMainPageSelected(int position) {
        SampleLog.v("onMainPageSelected position:%s", position);
        if (mBinding != null) {
            mBinding.mainBottomBar.setCurrentItem(position);
        }
    }

    private void onMainTabClick(int index) {
        SampleLog.v("onMainTabClick index:%s", index);
        if (mBinding != null) {
            mBinding.pager.setCurrentItem(index, false);
            mBinding.mainBottomBar.setCurrentItem(index);
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
