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

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMMessageFactory;
import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.core.IMConstants.ConversationType;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.common.imagepicker.ImageData;
import com.masonsoft.imsdk.sample.common.microlifecycle.MicroLifecycleComponentManager;
import com.masonsoft.imsdk.sample.common.microlifecycle.MicroLifecycleComponentManagerHost;
import com.masonsoft.imsdk.sample.common.microlifecycle.VisibleRecyclerViewMicroLifecycleComponentManager;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSingleChatFragmentBinding;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;
import com.masonsoft.imsdk.sample.util.ActivityUtil;
import com.masonsoft.imsdk.sample.util.EditTextUtil;
import com.masonsoft.imsdk.sample.util.TipUtil;
import com.masonsoft.imsdk.sample.widget.CustomSoftKeyboard;
import com.masonsoft.imsdk.util.Objects;

import java.util.Collection;
import java.util.List;

import io.github.idonans.core.AbortSignal;
import io.github.idonans.core.FormValidator;
import io.github.idonans.dynamic.page.UnionTypeStatusPageView;
import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;
import io.github.idonans.uniontype.UnionTypeAdapter;
import io.github.idonans.uniontype.UnionTypeItemObject;

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

        ViewUtil.onClick(mBinding.topBarBack, v -> ActivityUtil.requestBackPressed(SingleChatFragment.this));
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
                    SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                    return;
                }

                binding.recyclerView.post(() -> {
                    int count = mDataAdapter.getItemCount();
                    if (count > 0) {
                        binding.recyclerView.smoothScrollToPosition(count - 1);
                    }
                });

                if (customSoftKeyboard) {
                    if (binding.customSoftKeyboard.isLayerEmojiShown()) {
                        ViewUtil.setVisibilityIfChanged(binding.keyboardEmoji, View.GONE);
                        ViewUtil.setVisibilityIfChanged(binding.keyboardEmojiSystemSoftKeyboard, View.VISIBLE);
                    } else if (binding.customSoftKeyboard.isLayerMoreShown()) {
                        ViewUtil.setVisibilityIfChanged(binding.keyboardEmoji, View.VISIBLE);
                        ViewUtil.setVisibilityIfChanged(binding.keyboardEmojiSystemSoftKeyboard, View.GONE);
                    } else {
                        final Throwable e = new IllegalStateException();
                        SampleLog.e(e);
                    }
                } else {
                    ViewUtil.setVisibilityIfChanged(binding.keyboardEmoji, View.VISIBLE);
                    ViewUtil.setVisibilityIfChanged(binding.keyboardEmojiSystemSoftKeyboard, View.GONE);
                }
            }

            @Override
            protected void onAllSoftKeyboardLayoutHidden() {
                final ImsdkSampleSingleChatFragmentBinding binding = mBinding;
                if (binding == null) {
                    SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                    return;
                }
                ViewUtil.setVisibilityIfChanged(binding.keyboardEmoji, View.VISIBLE);
                ViewUtil.setVisibilityIfChanged(binding.keyboardEmojiSystemSoftKeyboard, View.GONE);
            }
        };

        final EditText keyboardEditText = mBinding.keyboardEditText;
        final View keyboardSubmit = mBinding.keyboardSubmit;
        final View keyboardMore = mBinding.keyboardMore;
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
                                ViewUtil.setVisibilityIfChanged(keyboardMore, enable ? View.GONE : View.VISIBLE);
                            }
                        }});
        ViewUtil.onClick(mBinding.keyboardSubmit, v -> submitTextMessage());
        ViewUtil.onClick(mBinding.keyboardEmoji, v -> {
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            if (mSoftKeyboardHelper == null) {
                SampleLog.e(Constants.ErrorLog.SOFT_KEYBOARD_HELPER_IS_NULL);
                return;
            }
            mBinding.customSoftKeyboard.showLayerEmoji();
            mSoftKeyboardHelper.requestShowCustomSoftKeyboard();
        });
        ViewUtil.onClick(mBinding.keyboardEmojiSystemSoftKeyboard, v -> {
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            if (mSoftKeyboardHelper == null) {
                SampleLog.e(Constants.ErrorLog.SOFT_KEYBOARD_HELPER_IS_NULL);
                return;
            }
            mSoftKeyboardHelper.requestShowSystemSoftKeyboard();
        });
        ViewUtil.onClick(mBinding.keyboardMore, v -> {
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            if (mSoftKeyboardHelper == null) {
                SampleLog.e(Constants.ErrorLog.SOFT_KEYBOARD_HELPER_IS_NULL);
                return;
            }
            mBinding.customSoftKeyboard.showLayerMore();
            mSoftKeyboardHelper.requestShowCustomSoftKeyboard();
        });
        mBinding.customSoftKeyboard.setOnInputListener(new CustomSoftKeyboard.OnInputListener() {
            @Override
            public void onInputText(CharSequence text) {
                if (mBinding == null) {
                    SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                    return;
                }
                EditTextUtil.insertText(mBinding.keyboardEditText, text);
            }

            @Override
            public void onDeleteOne() {
                if (mBinding == null) {
                    SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                    return;
                }
                EditTextUtil.deleteOne(mBinding.keyboardEditText);
            }

            @Override
            public void onImagePicked(@NonNull List<ImageData.ImageInfo> imageInfoList) {
                SampleLog.v("onImagePicked size:%s", imageInfoList.size());
                if (mBinding == null) {
                    SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                    return;
                }
                if (mSoftKeyboardHelper == null) {
                    SampleLog.e(Constants.ErrorLog.SOFT_KEYBOARD_HELPER_IS_NULL);
                    return;
                }
                mSoftKeyboardHelper.requestHideAllSoftKeyboard();
                submitImageMessage(imageInfoList);
            }
        });

        ViewUtil.setVisibilityIfChanged(mBinding.keyboardEmoji, View.VISIBLE);
        ViewUtil.setVisibilityIfChanged(mBinding.keyboardEmojiSystemSoftKeyboard, View.GONE);

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

    private void submitImageMessage(@NonNull List<ImageData.ImageInfo> imageInfoList) {
        final ImsdkSampleSingleChatFragmentBinding binding = mBinding;
        if (binding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        for (ImageData.ImageInfo imageInfo : imageInfoList) {
            final IMMessage imMessage = IMMessageFactory.createImageMessage(imageInfo.uri);
            IMMessageQueueManager.getInstance().enqueueSendMessage(
                    imMessage,
                    mTargetUserId,
                    new IMSessionMessage.EnqueueCallbackAdapter() {
                        @Override
                        public void onEnqueueFail(@NonNull IMSessionMessage imSessionMessage, int errorCode, String errorMessage) {
                            super.onEnqueueFail(imSessionMessage, errorCode, errorMessage);
                            TipUtil.show(errorMessage);
                        }
                    }
            );
        }
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
                binding.recyclerView.post(() -> {
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
                binding.recyclerView.post(() -> {
                    if (mDataAdapter != null) {
                        int count = mDataAdapter.getItemCount();
                        if (count > 0) {
                            boolean autoScroll = false;
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
                                SampleLog.v("require showNewMessagesTipView");
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
