package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.core.IMMessageManager;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.common.TopActivity;
import com.masonsoft.imsdk.sample.common.impopup.IMChatMessageMenuDialog;
import com.masonsoft.imsdk.sample.common.impreview.IMImageOrVideoPreviewDialog;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;
import com.masonsoft.imsdk.sample.util.ClipboardUtil;
import com.masonsoft.imsdk.sample.util.FileDownloadHelper;
import com.masonsoft.imsdk.sample.util.FormatUtil;
import com.masonsoft.imsdk.sample.util.TipUtil;
import com.masonsoft.imsdk.sample.widget.IMMessageRevokeStateFrameLayout;
import com.masonsoft.imsdk.sample.widget.IMMessageRevokeTextView;
import com.masonsoft.imsdk.sample.widget.debug.MessageDebugView;
import com.tbruyelle.rxpermissions3.RxPermissions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.NetUtil;
import io.github.idonans.core.util.SystemUtil;
import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;
import io.github.idonans.uniontype.UnionTypeItemObject;
import io.github.idonans.uniontype.UnionTypeViewHolder;

public abstract class IMMessageViewHolder extends UnionTypeViewHolder {

    protected static final boolean DEBUG = true;

    private static final FileDownloadHelper FILE_DOWNLOAD_HELPER = new FileDownloadHelper();

    static {
        FILE_DOWNLOAD_HELPER.setOnFileDownloadListener(new FileDownloadHelper.OnSampleFileDownloadListener(true, new FileDownloadHelper.OnFileDownloadListener() {
            @Override
            public void onDownloadSuccess(String id, String localFilePath, String serverUrl) {
                if (SystemUtil.addToMediaStore(new File(localFilePath))) {
                    TipUtil.show(R.string.imsdk_sample_tip_success_add_to_media_store);
                } else {
                    TipUtil.show(R.string.imsdk_sample_tip_download_success);
                }
            }

            @Override
            public void onDownloadFail(String id, String serverUrl, Throwable e) {
                if (!NetUtil.hasActiveNetwork()) {
                    TipUtil.showNetworkError();
                    return;
                }
                TipUtil.show(R.string.imsdk_sample_tip_download_fail);
            }
        }));
    }

    @SuppressLint("CheckResult")
    private static void download(Host host, String downloadUrl) {
        Activity innerActivity = host.getActivity();
        if (innerActivity == null) {
            SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
            return;
        }
        if (!(innerActivity instanceof AppCompatActivity)) {
            SampleLog.e("activity is not AppCompatActivity: %s", innerActivity);
            return;
        }
        FragmentManager fm = ((AppCompatActivity) innerActivity).getSupportFragmentManager();
        if (fm.isStateSaved()) {
            SampleLog.e(Constants.ErrorLog.FRAGMENT_MANAGER_STATE_SAVED);
            return;
        }

        //noinspection ResultOfMethodCallIgnored
        new RxPermissions((FragmentActivity) innerActivity)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (!granted) {
                        TipUtil.show(R.string.imsdk_sample_tip_require_permission_storage);
                        return;
                    }
                    FILE_DOWNLOAD_HELPER.enqueueFileDownload(null, downloadUrl);
                });
    }

    @Nullable
    private final IMMessageRevokeStateFrameLayout mMessageRevokeStateFrameLayout;
    @Nullable
    private final IMMessageRevokeTextView mMessageRevokeTextView;

    @Nullable
    private final MessageDebugView mMessageDebugView;
    @Nullable
    private final TextView mMessageTime;

    public IMMessageViewHolder(@NonNull Host host, int layout) {
        super(host, layout);
        mMessageRevokeStateFrameLayout = itemView.findViewById(R.id.message_revoke_state_layout);
        mMessageRevokeTextView = itemView.findViewById(R.id.message_revoke_text_view);

        mMessageDebugView = itemView.findViewById(R.id.message_debug_view);
        mMessageTime = itemView.findViewById(R.id.message_time);
    }

    public IMMessageViewHolder(@NonNull Host host, @NonNull View itemView) {
        super(host, itemView);
        mMessageRevokeStateFrameLayout = itemView.findViewById(R.id.message_revoke_state_layout);
        mMessageRevokeTextView = itemView.findViewById(R.id.message_revoke_text_view);

        mMessageDebugView = itemView.findViewById(R.id.message_debug_view);
        mMessageTime = itemView.findViewById(R.id.message_time);
    }

    @Override
    public final void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        onBindItemObject(position, (DataObject<IMMessage>) originObject);
    }

    @CallSuper
    protected void onBindItemObject(int position, @NonNull DataObject<IMMessage> itemObject) {
        final IMMessage imMessage = itemObject.object;

        final long sessionUserId = imMessage._sessionUserId.get();
        final int conversationType = imMessage._conversationType.get();
        final long targetUserId = imMessage._targetUserId.get();
        final long localMessageId = imMessage.id.get();
        if (mMessageDebugView != null) {
            mMessageDebugView.setMessage(
                    sessionUserId,
                    conversationType,
                    targetUserId,
                    localMessageId
            );
        }

        if (mMessageRevokeStateFrameLayout != null) {
            mMessageRevokeStateFrameLayout.setMessage(imMessage);
        }
        if (mMessageRevokeTextView != null) {
            mMessageRevokeTextView.setTargetUserId(imMessage.fromUserId.get());
        }

        if (mMessageTime != null) {
            updateMessageTimeView(mMessageTime, itemObject);
        }
    }

    /**
     * 获取一个时间间隔 ms，当与上一条消息的时间超过此间隔时，表示需要显示时间. 如果返回的时间间隔不大于 0, 则表示总是显示时间
     */
    protected long getShowTimeDuration() {
        return TimeUnit.MINUTES.toMillis(5);
    }

    protected boolean needShowTime(DataObject<IMMessage> dataObject) {
        final long showTimeDuration = getShowTimeDuration();
        if (showTimeDuration <= 0) {
            return true;
        }

        boolean needShowTime = true;
        if (dataObject != null) {
            if (dataObject.object != null) {
                final long currentMessageTime = dataObject.object.timeMs.getOrDefault(0L);
                if (currentMessageTime <= 0) {
                    Throwable e = new IllegalArgumentException("invalid timeMs " + dataObject.object);
                    SampleLog.e(e);
                }

                int position = getAdapterPosition();
                if (position > 0) {
                    UnionTypeItemObject preObject = host.getAdapter().getItem(position - 1);
                    if (preObject != null) {
                        //noinspection rawtypes
                        if (preObject.itemObject instanceof DataObject
                                && ((DataObject) preObject.itemObject).object instanceof IMMessage) {
                            //noinspection rawtypes
                            IMMessage preIMMessage = (IMMessage) ((DataObject) preObject.itemObject).object;
                            final long preMessageTime = preIMMessage.timeMs.getOrDefault(0L);
                            if (preMessageTime <= 0) {
                                Throwable e = new IllegalArgumentException("invalid timeMs " + preIMMessage);
                                SampleLog.e(e);
                            }
                            needShowTime = currentMessageTime - preMessageTime >= showTimeDuration;
                        }
                    }
                }
            }
        }

        return needShowTime;
    }

    protected void updateMessageTimeView(TextView messageTimeView, DataObject<IMMessage> dataObject) {
        if (messageTimeView == null) {
            SampleLog.v("updateMessageTimeView ignore null messageTimeView");
            return;
        }
        final boolean needShowTime = needShowTime(dataObject);
        if (!needShowTime) {
            messageTimeView.setVisibility(View.GONE);
            return;
        }

        long currentMessageTime = -1;
        if (dataObject != null && dataObject.object != null) {
            currentMessageTime = dataObject.object.timeMs.getOrDefault(0L);
        }
        if (currentMessageTime <= 0) {
            SampleLog.v("invalid current message time: %s", currentMessageTime);
            messageTimeView.setVisibility(View.GONE);
            return;
        }

        messageTimeView.setText(formatTime(currentMessageTime));
        messageTimeView.setVisibility(View.VISIBLE);
    }

    protected String formatTime(long time) {
        return FormatUtil.getHumanTimeDistance(time, new FormatUtil.DefaultDateFormatOptions());
    }

    public static class Helper {

        /**
         * 竖向默认消息模式
         */
        @Nullable
        public static UnionTypeItemObject createDefault(DataObject<IMMessage> dataObject, long sessionUserId) {
            if (dataObject.object.toUserId.isUnset()) {
                SampleLog.e("imMessage toUserId is unset %s", dataObject.object);
                return null;
            }
            if (dataObject.object.type.isUnset()) {
                SampleLog.e("imMessage type is unset %s", dataObject.object);
                return null;
            }

            // 区分消息是收到的还是发送的
            final boolean received = dataObject.object.toUserId.get() == sessionUserId;
            final long msgType = dataObject.object.type.get();

            // 已撤回的消息
            if (msgType == IMConstants.MessageType.REVOKED) {
                return received
                        ? UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_MESSAGE_REVOKE_RECEIVED,
                        dataObject)
                        : UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_MESSAGE_REVOKE_SEND,
                        dataObject);
            }

            // 文本消息
            if (msgType == IMConstants.MessageType.TEXT) {
                return received
                        ? UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_MESSAGE_TEXT_RECEIVED,
                        dataObject)
                        : UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_MESSAGE_TEXT_SEND,
                        dataObject);
            }

            // 图片消息
            if (msgType == IMConstants.MessageType.IMAGE) {
                return received ? UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_MESSAGE_IMAGE_RECEIVED,
                        dataObject)
                        : UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_MESSAGE_IMAGE_SEND,
                        dataObject);
            }

            // 语音消息
            if (msgType == IMConstants.MessageType.AUDIO) {
                return received ? UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_MESSAGE_VOICE_RECEIVED,
                        dataObject)
                        : UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_MESSAGE_VOICE_SEND,
                        dataObject);
            }

            // 视频消息
            if (msgType == IMConstants.MessageType.VIDEO) {
                return received ? UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_MESSAGE_VIDEO_RECEIVED,
                        dataObject)
                        : UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_MESSAGE_VIDEO_SEND,
                        dataObject);
            }

            // TODO 其它类型的消息

            SampleLog.e("createDefault unknown message type: %s", dataObject.object);
            return null;
        }

        /**
         * 横向全屏预览模式
         */
        @Nullable
        public static UnionTypeItemObject createPreviewDefault(DataObject<IMMessage> dataObject, long sessionUserId) {
            if (dataObject.object.toUserId.isUnset()) {
                SampleLog.e("imMessage toUserId is unset %s", dataObject.object);
                return null;
            }
            if (dataObject.object.type.isUnset()) {
                SampleLog.e("imMessage type is unset %s", dataObject.object);
                return null;
            }

            // 区分消息是收到的还是发送的
            final boolean received = dataObject.object.toUserId.get() == sessionUserId;
            final long msgType = dataObject.object.type.get();

            // 视频消息
            if (msgType == IMConstants.MessageType.VIDEO) {
                return UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_MESSAGE_PREVIEW_VIDEO,
                        dataObject);
            }

            // 图片消息
            if (msgType == IMConstants.MessageType.IMAGE) {
                return UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IM_MESSAGE_PREVIEW_IMAGE,
                        dataObject);
            }

            SampleLog.e("createPreviewDefault unknown message type %s", dataObject.object);
            return null;
        }

        @Nullable
        private static HolderFinder getHolderFinder(@NonNull UnionTypeViewHolder holder) {
            clearHolderFinderTag(holder);

            int position = holder.getAdapterPosition();
            if (position < 0) {
                SampleLog.e("invalid position %s", position);
                return null;
            }
            UnionTypeItemObject itemObject = holder.host.getAdapter().getItem(position);
            if (itemObject == null) {
                SampleLog.e("item object is null");
                return null;
            }
            if (!(itemObject.itemObject instanceof DataObject)) {
                SampleLog.e("item object is not data object");
                return null;
            }
            final DataObject<?> dataObject = (DataObject<?>) itemObject.itemObject;
            if (!(dataObject.object instanceof IMMessage)) {
                SampleLog.e("item object's data object's object is not ImMessage");
                return null;
            }

            final IMMessage message = (IMMessage) dataObject.object;
            Activity innerActivity = holder.host.getActivity();
            if (innerActivity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return null;
            }
            if (innerActivity != TopActivity.getInstance().get()) {
                SampleLog.e("activity is not the top activity");
                return null;
            }
            Lifecycle lifecycle = null;
            Fragment fragment = holder.host.getFragment();
            if (fragment != null) {
                lifecycle = fragment.getLifecycle();
            } else {
                if (innerActivity instanceof AppCompatActivity) {
                    lifecycle = ((AppCompatActivity) innerActivity).getLifecycle();
                }
            }
            if (lifecycle == null) {
                SampleLog.e("lifecycle is null");
                return null;
            }

            // 区分消息是收到的还是发送的
            final boolean received = message.toUserId.getOrDefault(0L) == IMSessionManager.getInstance().getSessionUserId();
            HolderFinder holderFinder = new HolderFinder();
            holderFinder.holder = holder;
            holderFinder.position = position;
            holderFinder.itemObject = itemObject;
            holderFinder.dataObject = dataObject;
            holderFinder.message = message;
            holderFinder.innerActivity = innerActivity;
            holderFinder.lifecycle = lifecycle;
            holderFinder.received = received;
            return holderFinder;
        }

        private interface OnHolderFinderRefreshCallback {
            void onHolderFinderRefresh(@Nullable HolderFinder holderFinder);
        }

        private static void refreshHolderFinderAsync(@NonNull final HolderFinder input, @NonNull OnHolderFinderRefreshCallback callback) {
            final Object tag = new Object();
            setHolderFinderTag(input.holder, tag);
            Threads.postBackground(() -> {
                final IMMessage message = IMMessageManager.getInstance().getMessage(
                        input.message._sessionUserId.get(),
                        input.message._conversationType.get(),
                        input.message._targetUserId.get(),
                        input.message.id.get()
                );
                Threads.runOnUi(() -> {
                    if (isHolderFinderTagChanged(input.holder, tag)) {
                        SampleLog.v("ignore. holder finder tag changed");
                        return;
                    }
                    if (message == null) {
                        callback.onHolderFinderRefresh(null);
                    } else {
                        input.message = message;
                        callback.onHolderFinderRefresh(input);
                    }
                });
            });
        }

        public static class HolderFinder {
            public UnionTypeViewHolder holder;
            public int position;
            public UnionTypeItemObject itemObject;
            public DataObject<?> dataObject;
            public IMMessage message;
            public Activity innerActivity;
            public Lifecycle lifecycle;

            // 区分消息是收到的还是发送的
            public boolean received;
        }

        public static void showPreview(UnionTypeViewHolder holder, long targetUserId) {
            final HolderFinder holderFinder = getHolderFinder(holder);
            if (holderFinder == null) {
                SampleLog.e("showPreview holderFinder is null");
                return;
            }

            if (holderFinder.message.type.isUnset()) {
                SampleLog.e("imMessage type is unset %s", holderFinder.message);
                return;
            }
            final long type = holderFinder.message.type.get();
            if (type == IMConstants.MessageType.IMAGE
                    || type == IMConstants.MessageType.VIDEO) {
                // 图片或者视频
                new IMImageOrVideoPreviewDialog(
                        holderFinder.lifecycle,
                        holderFinder.innerActivity,
                        holderFinder.innerActivity.findViewById(Window.ID_ANDROID_CONTENT),
                        holderFinder.message,
                        targetUserId
                ).show();
                return;
            }

            SampleLog.e("showPreview other message type %s", holderFinder.message);
        }

        private static void clearHolderFinderTag(UnionTypeViewHolder holder) {
            setHolderFinderTag(holder, new Object());
        }

        private static void setHolderFinderTag(UnionTypeViewHolder holder, Object tag) {
            holder.itemView.setTag(R.id.imsdk_sample_holder_finder_tag, tag);
        }

        private static boolean isHolderFinderTagChanged(UnionTypeViewHolder holder, Object tag) {
            return holder.itemView.getTag(R.id.imsdk_sample_holder_finder_tag) != tag;
        }

        public static void showMenu(UnionTypeViewHolder holder) {
            final HolderFinder holderFinder = getHolderFinder(holder);
            if (holderFinder == null) {
                SampleLog.e("holder finder is null");
                return;
            }
            refreshHolderFinderAsync(holderFinder, refreshHolderFinder -> {
                if (refreshHolderFinder == null) {
                    SampleLog.e("refreshHolderFinderAsync refreshHolderFinder is null");
                    return;
                }
                if (showMenuInternal(refreshHolderFinder)) {
                    ViewUtil.requestParentDisallowInterceptTouchEvent(refreshHolderFinder.holder.itemView);
                }
            });
        }

        private static boolean showMenuInternal(@NonNull final HolderFinder holderFinder) {
            if (holderFinder.message.type.isUnset()) {
                SampleLog.e("message type is unset %s", holderFinder.message);
                return false;
            }

            final long type = holderFinder.message.type.get();
            final int MENU_ID_COPY = 1;
            final int MENU_ID_RECALL = 2;
            if (type == IMConstants.MessageType.TEXT) {
                // 文字
                View anchorView = holderFinder.holder.itemView.findViewById(R.id.message_text);
                if (anchorView == null) {
                    SampleLog.v("showMenu MessageType.TEXT R.id.message_text not found");
                    return false;
                }

                if (anchorView.getWidth() <= 0 || anchorView.getHeight() <= 0) {
                    SampleLog.v("showMenu anchor view not layout");
                    return false;
                }

                final List<String> menuList = new ArrayList<>();
                final List<Integer> menuIdList = new ArrayList<>();

                menuList.add(I18nResources.getString(R.string.imsdk_sample_menu_copy));
                menuIdList.add(MENU_ID_COPY);
                if (!holderFinder.received) {
                    if (holderFinder.message.sendState.getOrDefault(IMConstants.SendStatus.SUCCESS) == IMConstants.SendStatus.SUCCESS) {
                        menuList.add(I18nResources.getString(R.string.imsdk_sample_menu_recall));
                        menuIdList.add(MENU_ID_RECALL);
                    }
                }

                final IMChatMessageMenuDialog menuDialog = new IMChatMessageMenuDialog(
                        holderFinder.innerActivity,
                        holderFinder.innerActivity.findViewById(Window.ID_ANDROID_CONTENT),
                        anchorView,
                        0,
                        menuList,
                        menuIdList);
                menuDialog.setOnIMMenuClickListener((menuId, menuText, menuView) -> {
                    if (menuId == MENU_ID_COPY) {
                        // 复制
                        ClipboardUtil.copy(holderFinder.message.body.getOrDefault(""));
                    } else if (menuId == MENU_ID_RECALL) {
                        // 撤回
                        revoke(holderFinder.holder);
                    } else {
                        SampleLog.e("IMChatMessageMenuDialog onItemMenuClick invalid menuId:%s, menuText:%s, menuView:%s",
                                menuId, menuText, menuView);
                    }
                });
                menuDialog.show();
                return true;
            }
            if (type == IMConstants.MessageType.IMAGE) {
                // 图片
                View anchorView = holderFinder.holder.itemView.findViewById(R.id.resize_image_view);
                if (anchorView == null) {
                    SampleLog.v("showMenu MessageType.IMAGE R.id.resize_image_view not found");
                    return false;
                }

                if (anchorView.getWidth() <= 0 || anchorView.getHeight() <= 0) {
                    SampleLog.v("showMenu anchor view not layout");
                    return false;
                }

                final List<String> menuList = new ArrayList<>();
                final List<Integer> menuIdList = new ArrayList<>();

                if (!holderFinder.received) {
                    if (holderFinder.message.sendState.getOrDefault(IMConstants.SendStatus.SUCCESS) == IMConstants.SendStatus.SUCCESS) {
                        menuList.add(I18nResources.getString(R.string.imsdk_sample_menu_recall));
                        menuIdList.add(MENU_ID_RECALL);
                    }
                }

                if (menuList.size() <= 0) {
                    return false;
                }
                IMChatMessageMenuDialog menuDialog = new IMChatMessageMenuDialog(
                        holderFinder.innerActivity,
                        holderFinder.innerActivity.findViewById(Window.ID_ANDROID_CONTENT),
                        anchorView,
                        0,
                        menuList,
                        menuIdList);
                menuDialog.setOnIMMenuClickListener((menuId, menuText, menuView) -> {
                    if (menuId == MENU_ID_RECALL) {
                        // 撤回
                        revoke(holderFinder.holder);
                    } else {
                        SampleLog.e("showMenu onItemMenuClick invalid menuId:%s, menuText:%s, menuView:%s",
                                menuId, menuText, menuView);
                    }
                });
                menuDialog.show();
                return true;
            }

            // TODO
            SampleLog.e("imMessage type is unknown %s", holderFinder.message);
            return false;
        }

        /**
         * 撤回
         */
        private static void revoke(UnionTypeViewHolder holder) {
            final HolderFinder holderFinder = getHolderFinder(holder);
            if (holderFinder == null) {
                SampleLog.e("revoke getHolderFinder return null");
                return;
            }
            final IMMessage message = holderFinder.message;
            IMMessageQueueManager.getInstance().enqueueRevokeActionMessage(message);
        }

    }

}
