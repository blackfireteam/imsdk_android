package com.masonsoft.imsdk.sample.app.chat;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.idonans.core.AbortSignal;
import com.idonans.core.FormValidator;
import com.idonans.dynamic.page.UnionTypeStatusPageView;
import com.idonans.lang.util.ViewUtil;
import com.idonans.uniontype.Host;
import com.idonans.uniontype.UnionTypeAdapter;
import com.idonans.uniontype.UnionTypeItemObject;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMMessageFactory;
import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.core.IMConstants.ConversationType;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.common.microlifecycle.MicroLifecycleComponentManager;
import com.masonsoft.imsdk.sample.common.microlifecycle.MicroLifecycleComponentManagerHost;
import com.masonsoft.imsdk.sample.common.microlifecycle.VisibleRecyclerViewMicroLifecycleComponentManager;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSingleChatFragmentBinding;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;
import com.masonsoft.imsdk.sample.util.BackStackUtil;
import com.masonsoft.imsdk.sample.util.TipUtil;
import com.masonsoft.imsdk.util.Objects;

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
    private LocalEnqueueCallback mEnqueueCallback;

    private UnionTypeAdapter mDataAdapter;
    private SingleChatFragmentPresenter mPresenter;
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
        adapter.setUnionTypeMapper(new UnionTypeMapperImpl());
        mDataAdapter = adapter;
        mViewImpl = new ViewImpl(adapter);
        clearPresenter();
        mPresenter = new SingleChatFragmentPresenter(mViewImpl);
        mViewImpl.setPresenter(mPresenter);
        recyclerView.setAdapter(adapter);

        mBinding.keyboardEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3000)});
        mSoftKeyboardHelper = new SoftKeyboardHelper(
                mBinding.softKeyboardListenerLayout,
                mBinding.keyboardEditText,
                mBinding.customSoftKeyboard) {
            @Override
            protected boolean isTouchOutside(float rawX, float rawY) {
                final ImsdkSampleSingleChatFragmentBinding binding = mBinding;
                if (binding == null) {
                    SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                    return false;
                }

                int[] outLocation = new int[2];
                binding.keyboardTopLine.getLocationInWindow(outLocation);
                boolean isTouchOutside = rawY <= outLocation[1];

                SampleLog.v("isTouchOutside touch raw:[%s,%s], keyboard top line location:[%s,%s], isTouchOutside:%s",
                        rawX, rawY, outLocation[0], outLocation[1], isTouchOutside);

                return isTouchOutside;
            }

            @Override
            protected void onSoftKeyboardLayoutShown(boolean customSoftKeyboard, boolean systemSoftKeyboard) {
                final ImsdkSampleSingleChatFragmentBinding binding = mBinding;
                if (binding == null) {
                    return;
                }

                binding.recyclerView.postOnAnimation(() -> {
                    int count = mDataAdapter.getItemCount();
                    if (count > 0) {
                        binding.recyclerView.smoothScrollToPosition(count - 1);
                    }
                });

                /*
                TODO
                if (customSoftKeyboard) {
                    if (mCustomSoftKeyboard.isAudioShown()) {
                        mItemKeyEmotion.setSelected(false);
                        mItemKeyVoice.setSelected(true);
                    } else if (mCustomSoftKeyboard.isPagerShown()) {
                        mItemKeyEmotion.setSelected(true);
                        mItemKeyVoice.setSelected(false);
                    } else {
                        mItemKeyEmotion.setSelected(false);
                        mItemKeyVoice.setSelected(false);
                    }
                } else {
                    mItemKeyEmotion.setSelected(false);
                    mItemKeyVoice.setSelected(false);
                }

                mItemKeyGift.setSelected(customGiftInputBoard);

                if (!customKeyboard && customGiftInputBoard) {
                    // 手动清除文本输入框的焦点，以免该输入框上的系统弹层(如：复制菜单)显示到了礼物面板之上
                    mItemKeyEditText.clearFocus();
                }
                */
            }

            @Override
            protected void onAllSoftKeyboardLayoutHidden() {
                /*
                TODO
                mItemKeyEmotion.setSelected(false);
                mItemKeyVoice.setSelected(false);
                mItemKeyGift.setSelected(false);*/
            }
        };

        final EditText keyboardEditText = mBinding.keyboardEditText;
        final View keyboardSubmit = mBinding.keyboardSubmit;
        FormValidator.bind(
                new FormValidator.InputView[]{
                        new FormValidator.InputViewFactory.TextViewInputView(keyboardEditText) {
                            @Override
                            public boolean isContentEnable() {
                                final Editable editable = keyboardEditText.getText();
                                if (editable == null) {
                                    SampleLog.e(Constants.ErrorLog.EDITABLE_IS_NULL);
                                    return false;
                                }
                                final String content = editable.toString();
                                return content.trim().length() > 0;
                            }
                        }
                },
                new FormValidator.SubmitView[]{
                        new FormValidator.SubmitViewFactory.SimpleSubmitView(keyboardSubmit) {
                            @Override
                            public void setSubmitEnable(boolean enable) {
                                ViewUtil.setVisibilityIfChanged(keyboardSubmit, enable ? View.VISIBLE : View.GONE);
                            }
                        }});
        ViewUtil.onClick(mBinding.keyboardSubmit, v -> submitTextMessage());

        mPresenter.requestInit();

        return mBinding.getRoot();
    }

    private void submitTextMessage() {
        final ImsdkSampleSingleChatFragmentBinding binding = mBinding;
        if (binding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        final Editable editable = binding.keyboardEditText.getText();
        if (editable == null) {
            SampleLog.e(Constants.ErrorLog.EDITABLE_IS_NULL);
            return;
        }

        final String text = editable.toString().trim();
        final IMMessage imMessage = IMMessageFactory.createTextMessage(text);
        mEnqueueCallback = new LocalEnqueueCallback();
        IMMessageQueueManager.getInstance().enqueueSendMessage(
                imMessage,
                mTargetUserId,
                new IMSessionMessage.WeakEnqueueCallbackAdapter(mEnqueueCallback, true)
        );
    }

    private void clearPresenter() {
        if (mPresenter != null) {
            mPresenter.setAbort();
            mPresenter = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearPresenter();
        mBinding = null;
        mViewImpl = null;
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

        public long getTargetUserId() {
            return mTargetUserId;
        }

        @Override
        public void onInitDataEmpty() {
            SampleLog.v(Objects.defaultObjectTag(this) + " onInitDataEmpty");
            super.onInitDataEmpty();
        }

        @Override
        public void onInitDataLoad(@NonNull Collection<UnionTypeItemObject> items) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onInitDataLoad items size:" + items.size());
            super.onInitDataLoad(items);

            final ImsdkSampleSingleChatFragmentBinding binding = mBinding;
            if (binding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }

            if (!items.isEmpty()) {
                binding.recyclerView.postOnAnimation(() -> {
                    if (mDataAdapter != null) {
                        int count = mDataAdapter.getItemCount();
                        if (count > 0) {
                            binding.recyclerView.scrollToPosition(count - 1);
                        }
                    }
                });
            }
        }

        @Override
        public void onPrePageDataLoad(@NonNull Collection<UnionTypeItemObject> items) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onPrePageDataLoad items size:" + items.size());
            super.onPrePageDataLoad(items);
        }

        @Override
        public void onNextPageDataLoad(@NonNull Collection<UnionTypeItemObject> items) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onNextPageDataLoad items size:" + items.size());
            super.onNextPageDataLoad(items);

            final ImsdkSampleSingleChatFragmentBinding binding = mBinding;
            if (binding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }

            if (!items.isEmpty()) {
                binding.recyclerView.postOnAnimation(() -> {
                    if (mDataAdapter != null) {
                        int count = mDataAdapter.getItemCount();
                        if (count > 0) {
                            boolean autoScroll = false;
                            //noinspection ConstantConditions
                            int lastPosition = ((LinearLayoutManager) binding.recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                            if (lastPosition >= count - 1 - items.size()) {
                                // 当前滚动到最后
                                autoScroll = true;
                            }
                            if (autoScroll) {
                                binding.recyclerView.smoothScrollToPosition(count - 1);
                            } else {
                                // 显示向下的箭头
                                // TODO
                                /*
                                showNewMessagesTipView();
                                */
                            }
                        }
                    }
                });
            }
        }

        // TODO
        public Activity getActivity() {
            return SingleChatFragment.this.getActivity();
        }
    }

    private class LocalEnqueueCallback implements IMSessionMessage.EnqueueCallback, AbortSignal {

        @Override
        public void onEnqueueSuccess(@NonNull IMSessionMessage imSessionMessage) {
            if (isAbort()) {
                return;
            }
            final ImsdkSampleSingleChatFragmentBinding binding = mBinding;
            if (binding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }

            SampleLog.v("onEnqueueSuccess %s", imSessionMessage);
            // 消息发送成功之后，清空输入框
            binding.keyboardEditText.setText(null);
        }

        @Override
        public void onEnqueueFail(@NonNull IMSessionMessage imSessionMessage, int errorCode, String errorMessage) {
            if (isAbort()) {
                return;
            }
            final ImsdkSampleSingleChatFragmentBinding binding = mBinding;
            if (binding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }

            SampleLog.v("onEnqueueFail %s, errorCode:%s, errorMessage:%s", imSessionMessage, errorCode, errorMessage);
            TipUtil.show(errorMessage);
        }

        @Override
        public boolean isAbort() {
            return mEnqueueCallback != this;
        }
    }

}
