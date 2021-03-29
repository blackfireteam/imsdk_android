package com.masonsoft.imsdk.sample.app.chat;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.idonans.dynamic.page.UnionTypeStatusPageView;
import com.idonans.lang.util.ViewUtil;
import com.idonans.uniontype.Host;
import com.idonans.uniontype.UnionTypeAdapter;
import com.idonans.uniontype.UnionTypeItemObject;
import com.masonsoft.imsdk.core.IMConstants.ConversationType;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.common.microlifecycle.MicroLifecycleComponentManager;
import com.masonsoft.imsdk.sample.common.microlifecycle.MicroLifecycleComponentManagerHost;
import com.masonsoft.imsdk.sample.common.microlifecycle.VisibleRecyclerViewMicroLifecycleComponentManager;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSingleChatFragmentBinding;
import com.masonsoft.imsdk.sample.util.BackStackUtil;

import java.util.Collection;

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
    @Nullable
    private SoftKeyboardHelper mSoftKeyboardHelper;

    private ImSingleFragmentPresenter mPresenter;
    private ViewImpl mViewImpl;
    private MicroLifecycleComponentManager mMicroLifecycleComponentManager;

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

        final RecyclerView recyclerView = mBinding.recyclerView;
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                recyclerView.getContext(),
                RecyclerView.VERTICAL,
                false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);
        recyclerView.setHasFixedSize(true);
        mMicroLifecycleComponentManager = new VisibleRecyclerViewMicroLifecycleComponentManager(recyclerView, getLifecycle());

        UnionTypeAdapter adapter = new UnionTypeAdapterImpl();
        adapter.setHost(Host.Factory.create(this, recyclerView, adapter));
        adapter.setUnionTypeMapper(new UnionTypeAppImplMapper());
        mDataAdapter = adapter;
        mViewImpl = new ViewImpl(adapter);
        clearPresenter();

        mBinding.keyboardEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3000)});

        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private class UnionTypeAdapterImpl extends UnionTypeAdapter implements MicroLifecycleComponentManagerHost {
        @Override
        public MicroLifecycleComponentManager getMicroLifecycleComponentManager() {
            return mMicroLifecycleComponentManager;
        }
    }

    class ViewImpl extends UnionTypeStatusPageView {

        public ViewImpl(@NonNull UnionTypeAdapter adapter) {
            super(adapter);
            setAlwaysHideNoMoreData(true);
        }

        public void hideAllSoftKeyboard() {
            if (mSoftKeyboardHelper != null) {
                mSoftKeyboardHelper.requestHideAllSoftKeyboard();
            }
        }

        public void showCustomGiftInputBoard() {
            if (mSoftKeyboardHelper != null) {
                mSoftKeyboardHelper.requestShowCustomGiftInputBoard();
            }
        }

        private SimpleLoadingDialog mLoadingDialog;

        public void showLoading(String text) {
            Activity innerActivity = getActivity();
            if (innerActivity == null) {
                Timber.e(Constants.Tip.ACTIVITY_NULL);
                return;
            }
            if (mLoadingDialog == null) {
                mLoadingDialog = new SimpleLoadingDialog(innerActivity, text);
                mLoadingDialog.show();
            } else {
                mLoadingDialog.setText(text);
            }
        }

        public void hideLoading() {
            if (mLoadingDialog != null) {
                mLoadingDialog.hide();
                mLoadingDialog = null;
            }
        }

        public long getTargetUserId() {
            return mTargetUserId;
        }

        @Override
        public void onInitDataLoad(@NonNull Collection<UnionTypeItemObject> items) {
            super.onInitDataLoad(items);
            if (mRecyclerView != null && !items.isEmpty()) {
                mRecyclerView.postOnAnimation(() -> {
                    if (mRecyclerView != null && mDataAdapter != null) {
                        int count = mDataAdapter.getItemCount();
                        if (count > 0) {
                            mRecyclerView.scrollToPosition(count - 1);
                        }
                    }
                });
            }
        }

        @Override
        public void onNextPageDataLoad(@NonNull Collection<UnionTypeItemObject> items) {
            super.onNextPageDataLoad(items);
            if (mRecyclerView != null && !items.isEmpty()) {
                mRecyclerView.postOnAnimation(() -> {
                    if (mRecyclerView != null && mDataAdapter != null) {
                        int count = mDataAdapter.getItemCount();
                        if (count > 0) {
                            boolean autoScroll = false;
                            int lastPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findLastVisibleItemPosition();
                            if (lastPosition >= count - 1 - items.size()) {
                                // 当前滚动到最后
                                autoScroll = true;
                            }
                            if (autoScroll) {
                                mRecyclerView.smoothScrollToPosition(count - 1);
                            } else {
                                // 显示向下的箭头
                                showNewMessagesTipView();
                            }
                        }
                    }
                });
            }
        }

        public Activity getActivity() {
            return ImSingleFragment.this.getActivity();
        }
    }

}
