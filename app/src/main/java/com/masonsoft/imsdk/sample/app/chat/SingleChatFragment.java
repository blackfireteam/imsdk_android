package com.masonsoft.imsdk.sample.app.chat;

import android.Manifest;
import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.masonsoft.imsdk.EnqueueCallback;
import com.masonsoft.imsdk.EnqueueCallbackAdapter;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMMessageFactory;
import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.WeakEnqueueCallbackAdapter;
import com.masonsoft.imsdk.core.IMConstants.ConversationType;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.SystemInsetsFragment;
import com.masonsoft.imsdk.sample.common.mediapicker.MediaData;
import com.masonsoft.imsdk.sample.common.media.audio.AudioRecordManager;
import com.masonsoft.imsdk.sample.common.microlifecycle.MicroLifecycleComponentManager;
import com.masonsoft.imsdk.sample.common.microlifecycle.MicroLifecycleComponentManagerHost;
import com.masonsoft.imsdk.sample.common.microlifecycle.VisibleRecyclerViewMicroLifecycleComponentManager;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleSingleChatFragmentBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;
import com.masonsoft.imsdk.sample.util.ActivityUtil;
import com.masonsoft.imsdk.sample.util.EditTextUtil;
import com.masonsoft.imsdk.sample.util.TipUtil;
import com.masonsoft.imsdk.sample.widget.CustomSoftKeyboard;
import com.masonsoft.imsdk.util.Objects;
import com.tbruyelle.rxpermissions3.RxPermissions;

import java.util.Collection;
import java.util.List;

import io.github.idonans.core.AbortSignal;
import io.github.idonans.core.FormValidator;
import io.github.idonans.core.util.PermissionUtil;
import io.github.idonans.dynamic.page.UnionTypeStatusPageView;
import io.github.idonans.lang.DisposableHolder;
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

    private final DisposableHolder mPermissionRequest = new DisposableHolder();
    private static final String[] VOICE_RECORD_PERMISSION = {
            Manifest.permission.RECORD_AUDIO,
    };

    private long mTargetUserId;
    @Nullable
    private ImsdkSampleSingleChatFragmentBinding mBinding;
    @Nullable
    private SoftKeyboardHelper mSoftKeyboardHelper;
    private LocalEnqueueCallback mEnqueueCallback;
    private VoiceRecordGestureHelper mVoiceRecordGestureHelper;
    private final AudioRecordManager.OnAudioRecordListener mOnAudioRecordListener = new OnAudioRecordListenerImpl();

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
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int lastPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                    if (mDataAdapter != null && lastPosition >= 0) {
                        if (lastPosition == mDataAdapter.getItemCount() - 1) {
                            // 滚动到最底部
                            hideNewMessagesTipView();
                            sendMarkAsRead();
                        }
                    }
                }
            }
        });
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
                        final int firstPosition = ((LinearLayoutManager) binding.recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                        final int lastPosition = ((LinearLayoutManager) binding.recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                        final int archPosition = Math.max(0, count - 3);

                        boolean scrollWithAnimation = false;
                        if (archPosition >= firstPosition && archPosition <= lastPosition) {
                            scrollWithAnimation = true;
                        }

                        SampleLog.v("onSoftKeyboardLayoutShown scrollWithAnimation:%s, firstPosition:%s, count:%s",
                                scrollWithAnimation, firstPosition, count);
                        if (scrollWithAnimation) {
                            binding.recyclerView.smoothScrollToPosition(count - 1);
                        } else {
                            binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                                @Override
                                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                                    super.onScrolled(recyclerView, dx, dy);
                                    binding.recyclerView.removeOnScrollListener(this);
                                    SampleLog.v("onSoftKeyboardLayoutShown scrollWithAnimation:false addOnScrollListener onScrolled");
                                    binding.recyclerView.smoothScrollToPosition(count - 1);
                                }
                            });
                            binding.recyclerView.scrollToPosition(archPosition);
                        }
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

                ViewUtil.setVisibilityIfChanged(mBinding.keyboardVoice, View.VISIBLE);
                ViewUtil.setVisibilityIfChanged(mBinding.keyboardVoiceSystemSoftKeyboard, View.GONE);
                ViewUtil.setVisibilityIfChanged(mBinding.keyboardEditText, View.VISIBLE);
                ViewUtil.setVisibilityIfChanged(mBinding.keyboardVoiceRecordText, View.GONE);
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
        mVoiceRecordGestureHelper = new VoiceRecordGestureHelper(mBinding.keyboardVoiceRecordText) {
            @Override
            protected void onVoiceRecordGestureStart() {
                SampleLog.v(Objects.defaultObjectTag(this) + " onVoiceRecordGestureStart");
                if (hasVoiceRecordPermission()) {
                    AudioRecordManager.getInstance().startAudioRecord();
                } else {
                    requestVoiceRecordPermission();
                }
            }

            @Override
            protected void onVoiceRecordGestureMove(boolean inside) {
                SampleLog.v(Objects.defaultObjectTag(this) + " onVoiceRecordGestureMove inside:%s", inside);
                if (mViewImpl != null) {
                    mViewImpl.updateAudioRecording(inside);
                }
            }

            @Override
            protected void onVoiceRecordGestureEnd(boolean inside) {
                SampleLog.v(Objects.defaultObjectTag(this) + " onVoiceRecordGestureEnd inside:%s", inside);
                if (inside) {
                    AudioRecordManager.getInstance().stopAudioRecord();
                } else {
                    AudioRecordManager.getInstance().cancelAudioRecord();
                }
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
        ViewUtil.onClick(mBinding.keyboardVoice, v -> {
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            if (mSoftKeyboardHelper == null) {
                SampleLog.e(Constants.ErrorLog.SOFT_KEYBOARD_HELPER_IS_NULL);
                return;
            }

            ViewUtil.setVisibilityIfChanged(mBinding.keyboardVoice, View.GONE);
            ViewUtil.setVisibilityIfChanged(mBinding.keyboardVoiceSystemSoftKeyboard, View.VISIBLE);
            ViewUtil.setVisibilityIfChanged(mBinding.keyboardEditText, View.GONE);
            ViewUtil.setVisibilityIfChanged(mBinding.keyboardVoiceRecordText, View.VISIBLE);
            mSoftKeyboardHelper.requestHideAllSoftKeyboard();
        });
        ViewUtil.onClick(mBinding.keyboardVoiceSystemSoftKeyboard, v -> {
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }
            if (mSoftKeyboardHelper == null) {
                SampleLog.e(Constants.ErrorLog.SOFT_KEYBOARD_HELPER_IS_NULL);
                return;
            }

            ViewUtil.setVisibilityIfChanged(mBinding.keyboardVoice, View.VISIBLE);
            ViewUtil.setVisibilityIfChanged(mBinding.keyboardVoiceSystemSoftKeyboard, View.GONE);
            ViewUtil.setVisibilityIfChanged(mBinding.keyboardEditText, View.VISIBLE);
            ViewUtil.setVisibilityIfChanged(mBinding.keyboardVoiceRecordText, View.GONE);
            mSoftKeyboardHelper.requestShowSystemSoftKeyboard();
        });
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
            public void onImagePicked(@NonNull List<MediaData.MediaInfo> mediaInfoList) {
                SampleLog.v("onImagePicked size:%s", mediaInfoList.size());
                if (mBinding == null) {
                    SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                    return;
                }
                if (mSoftKeyboardHelper == null) {
                    SampleLog.e(Constants.ErrorLog.SOFT_KEYBOARD_HELPER_IS_NULL);
                    return;
                }
                mSoftKeyboardHelper.requestHideAllSoftKeyboard();
                submitImageMessage(mediaInfoList);
            }
        });

        AudioRecordManager.getInstance().setOnAudioRecordListener(mOnAudioRecordListener);

        ViewUtil.setVisibilityIfChanged(mBinding.keyboardVoice, View.VISIBLE);
        ViewUtil.setVisibilityIfChanged(mBinding.keyboardVoiceSystemSoftKeyboard, View.GONE);
        ViewUtil.setVisibilityIfChanged(mBinding.keyboardEditText, View.VISIBLE);
        ViewUtil.setVisibilityIfChanged(mBinding.keyboardVoiceRecordText, View.GONE);
        ViewUtil.setVisibilityIfChanged(mBinding.keyboardEmoji, View.VISIBLE);
        ViewUtil.setVisibilityIfChanged(mBinding.keyboardEmojiSystemSoftKeyboard, View.GONE);

        mPresenter.requestInit();

        return mBinding.getRoot();
    }

    private boolean hasVoiceRecordPermission() {
        return PermissionUtil.isAllGranted(VOICE_RECORD_PERMISSION);
    }

    private void requestVoiceRecordPermission() {
        SampleLog.v("requestVoiceRecordPermission");

        final Activity activity = getActivity();
        if (activity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_NOT_FOUND_IN_FRAGMENT);
            return;
        }

        if (mBinding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        //noinspection CastCanBeRemovedNarrowingVariableType
        final RxPermissions rxPermissions = new RxPermissions((FragmentActivity) activity);
        mPermissionRequest.set(
                rxPermissions.request(VOICE_RECORD_PERMISSION)
                        .subscribe(granted -> {
                            if (granted) {
                                onVoiceRecordPermissionGranted();
                            } else {
                                SampleLog.e(Constants.ErrorLog.PERMISSION_REQUIRED);
                            }
                        }));
    }

    private void onVoiceRecordPermissionGranted() {
        SampleLog.v("onVoiceRecordPermissionGranted");
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
        IMMessageQueueManager.getInstance().enqueueSendSessionMessage(
                imMessage,
                mTargetUserId,
                new WeakEnqueueCallbackAdapter<>(mEnqueueCallback, true)
        );
    }

    private void submitImageMessage(@NonNull List<MediaData.MediaInfo> mediaInfoList) {
        final ImsdkSampleSingleChatFragmentBinding binding = mBinding;
        if (binding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            return;
        }

        for (MediaData.MediaInfo mediaInfo : mediaInfoList) {
            final IMMessage message = IMMessageFactory.createImageMessage(mediaInfo.uri);
            IMMessageQueueManager.getInstance().enqueueSendSessionMessage(
                    message,
                    mTargetUserId,
                    new EnqueueCallbackAdapter<IMSessionMessage>() {
                        @Override
                        public void onEnqueueFail(@NonNull IMSessionMessage imSessionMessage, int errorCode, String errorMessage) {
                            super.onEnqueueFail(imSessionMessage, errorCode, errorMessage);
                            TipUtil.show(errorMessage);
                        }
                    }
            );
        }
    }

    private void submitVoiceMessage(final String voiceFilePath) {
        final ImsdkSampleSingleChatFragmentBinding binding = mBinding;
        if (binding == null) {
            SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
            return;
        }
        final IMMessage message = IMMessageFactory.createAudioMessage(voiceFilePath);
        IMMessageQueueManager.getInstance().enqueueSendSessionMessage(
                message,
                mTargetUserId,
                new EnqueueCallbackAdapter<IMSessionMessage>() {
                    @Override
                    public void onEnqueueFail(@NonNull IMSessionMessage imSessionMessage, int errorCode, String errorMessage) {
                        super.onEnqueueFail(imSessionMessage, errorCode, errorMessage);
                        TipUtil.show(errorMessage);
                    }
                }
        );
    }

    private void showNewMessagesTipView() {
        // TODO
        // ViewUtil.setVisibilityIfChanged(mActionNewMessages, View.VISIBLE);
    }

    private void hideNewMessagesTipView() {
        // TODO
        // ViewUtil.setVisibilityIfChanged(mActionNewMessages, View.GONE);
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
        if (AudioRecordManager.getInstance().getOnAudioRecordListener() == mOnAudioRecordListener) {
            AudioRecordManager.getInstance().setOnAudioRecordListener(null);
        }
        mVoiceRecordGestureHelper = null;
    }

    private class UnionTypeAdapterImpl extends UnionTypeAdapter implements MicroLifecycleComponentManagerHost {
        @Override
        public MicroLifecycleComponentManager getMicroLifecycleComponentManager() {
            return mMicroLifecycleComponentManager;
        }
    }

    private void sendMarkAsRead() {
        SampleLog.v(Objects.defaultObjectTag(this) + " sendMarkAsRead");
        if (mDataAdapter != null) {
            final List<UnionTypeItemObject> items = mDataAdapter.getData().getGroupItems(ViewImpl.GROUP_DEFAULT);
            if (items != null && items.size() > 0) {
                final UnionTypeItemObject lastItem = items.get(items.size() - 1);
                if (lastItem.itemObject instanceof DataObject) {
                    final DataObject<?> dataObject = (DataObject<?>) lastItem.itemObject;
                    if (dataObject.object instanceof IMMessage) {
                        final IMMessage message = (IMMessage) dataObject.object;
                        SampleLog.v(Objects.defaultObjectTag(this) + " sendMarkAsRead message:%s", message);
                        IMMessageQueueManager.getInstance().enqueueMarkAsReadActionMessage(message);
                    }
                }
            }
        }
    }

    private class OnAudioRecordListenerImpl implements AudioRecordManager.OnAudioRecordListener {

        @Override
        public void onAudioRecordStart() {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAudioRecordStart");
            if (mViewImpl != null) {
                mViewImpl.showAudioRecording();
            }
        }

        @Override
        public void onAudioRecordProgress(long duration) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAudioRecordProgress duration:%s", duration);
        }

        @Override
        public void onAudioRecordError() {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAudioRecordError");
            if (mViewImpl != null) {
                mViewImpl.hideAudioRecoding(false, true);
            }
        }

        @Override
        public void onAudioRecordCancel(boolean lessThanMinDuration) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAudioRecordCancel lessThanMinDuration:%s", lessThanMinDuration);
            if (mViewImpl != null) {
                mViewImpl.hideAudioRecoding(lessThanMinDuration, false);
            }
        }

        @Override
        public void onAudioRecordCompletedSuccess(@NonNull String audioRecorderFile, boolean reachMaxDuration) {
            SampleLog.v(Objects.defaultObjectTag(this) + " onAudioRecordCompletedSuccess audioRecorderFile:%s, reachMaxDuration:%s", audioRecorderFile, reachMaxDuration);
            if (mViewImpl != null) {
                mViewImpl.hideAudioRecoding(false, false);

                // 发送语音消息
                submitVoiceMessage(audioRecorderFile);
            }
        }
    }

    class ViewImpl extends UnionTypeStatusPageView {

        public ViewImpl(@NonNull UnionTypeAdapter adapter) {
            super(adapter);
            setAlwaysHideNoMoreData(true);
        }

        private void showAudioRecording() {
            if (getChildFragmentManager().isStateSaved()) {
                SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
                return;
            }
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }

            ViewUtil.setVisibilityIfChanged(mBinding.recordingVolumeLayer, View.VISIBLE);
            mBinding.recordingVolumeIcon.setImageResource(R.drawable.imsdk_sample_recording_volume);
            final Drawable drawable = mBinding.recordingVolumeIcon.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                ((AnimationDrawable) drawable).start();
            }
            mBinding.recordingVolumeTip.setText(R.string.imsdk_sample_voice_record_down_cancel_send);
        }

        private void updateAudioRecording(boolean inside) {
            if (getChildFragmentManager().isStateSaved()) {
                SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
                return;
            }
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }

            if (inside) {
                Drawable drawable = mBinding.recordingVolumeIcon.getDrawable();
                if (!(drawable instanceof AnimationDrawable)) {
                    mBinding.recordingVolumeIcon.setImageResource(R.drawable.imsdk_sample_recording_volume);
                    drawable = mBinding.recordingVolumeIcon.getDrawable();
                }

                if (drawable instanceof AnimationDrawable) {
                    ((AnimationDrawable) drawable).start();
                }
                mBinding.recordingVolumeTip.setText(R.string.imsdk_sample_voice_record_down_cancel_send);
            } else {
                mBinding.recordingVolumeIcon.setImageResource(R.drawable.imsdk_sample_ic_volume_dialog_cancel);
                mBinding.recordingVolumeTip.setText(R.string.imsdk_sample_voice_record_up_cancel_send);
            }
        }

        private void hideAudioRecoding(final boolean tooShort, final boolean fail) {
            if (getChildFragmentManager().isStateSaved()) {
                SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
                return;
            }
            if (mBinding == null) {
                SampleLog.e(Constants.ErrorLog.BINDING_IS_NULL);
                return;
            }

            if (mBinding.recordingVolumeLayer.getVisibility() == View.GONE) {
                SampleLog.w("unexpected. hideAudioRecoding recordingVolumeLayer already gone");
                return;
            }

            final Drawable drawable = mBinding.recordingVolumeIcon.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                ((AnimationDrawable) drawable).stop();
            }

            if (tooShort || fail) {
                mBinding.recordingVolumeIcon.setImageResource(R.drawable.imsdk_sample_ic_volume_dialog_length_short);
                if (tooShort) {
                    mBinding.recordingVolumeTip.setText(R.string.imsdk_sample_voice_record_say_time_short);
                } else {
                    mBinding.recordingVolumeTip.setText(R.string.imsdk_sample_voice_record_fail);
                }

                final ImsdkSampleSingleChatFragmentBinding unsafeBinding = mBinding;
                unsafeBinding.getRoot().postDelayed(() -> ViewUtil.setVisibilityIfChanged(unsafeBinding.recordingVolumeLayer, View.GONE), 800L);
            } else {
                final ImsdkSampleSingleChatFragmentBinding unsafeBinding = mBinding;
                unsafeBinding.getRoot().postDelayed(() -> ViewUtil.setVisibilityIfChanged(unsafeBinding.recordingVolumeLayer, View.GONE), 300L);
            }
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
                            sendMarkAsRead();
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
                                sendMarkAsRead();
                            } else {
                                // 显示向下的箭头
                                showNewMessagesTipView();
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

    private class LocalEnqueueCallback implements EnqueueCallback<IMSessionMessage>, AbortSignal {

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
