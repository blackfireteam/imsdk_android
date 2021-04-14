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
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.common.TopActivity;
import com.masonsoft.imsdk.sample.common.impopup.IMChatMessageMenuDialog;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;
import com.masonsoft.imsdk.sample.util.ClipboardUtil;
import com.masonsoft.imsdk.sample.util.FileDownloadHelper;
import com.masonsoft.imsdk.sample.util.FormatUtil;
import com.masonsoft.imsdk.sample.util.TipUtil;
import com.masonsoft.imsdk.sample.widget.debug.MessageDebugView;
import com.tbruyelle.rxpermissions3.RxPermissions;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.github.idonans.core.util.NetUtil;
import io.github.idonans.core.util.SystemUtil;
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
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (!granted) {
                        TipUtil.show(R.string.imsdk_sample_tip_require_permission_storage);
                        return;
                    }
                    FILE_DOWNLOAD_HELPER.enqueueFileDownload(null, downloadUrl);
                });
    }

    @Nullable
    private final MessageDebugView mMessageDebugView;
    @Nullable
    private final TextView mMessageTime;

    public IMMessageViewHolder(@NonNull Host host, int layout) {
        super(host, layout);
        mMessageDebugView = itemView.findViewById(R.id.message_debug_view);
        mMessageTime = itemView.findViewById(R.id.message_time);
    }

    public IMMessageViewHolder(@NonNull Host host, @NonNull View itemView) {
        super(host, itemView);
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

            // 文本消息
            // TODO

            // TODO 其它类型的消息

            SampleLog.e("createDefault unknown message type %s", dataObject.object);
            return null;
        }

        @SuppressWarnings("rawtypes")
        private static boolean getHolderFinder(UnionTypeViewHolder holder, HolderFinder[] out) {
            int position = holder.getAdapterPosition();
            if (position < 0) {
                SampleLog.e("invalid position %s", position);
                return false;
            }
            UnionTypeItemObject itemObject = holder.host.getAdapter().getItem(position);
            if (itemObject == null) {
                SampleLog.e("item object is null");
                return false;
            }
            if (!(itemObject.itemObject instanceof DataObject)) {
                SampleLog.e("item object is not data object");
                return false;
            }
            if (!(((DataObject) itemObject.itemObject).object instanceof IMMessage)) {
                SampleLog.e("item object's data object's object is not ImMessage");
                return false;
            }

            final IMMessage imMessage = (IMMessage) ((DataObject) itemObject.itemObject).object;
            Activity innerActivity = holder.host.getActivity();
            if (innerActivity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return false;
            }
            if (innerActivity != TopActivity.getInstance().get()) {
                SampleLog.e("activity is not the top activity");
                return false;
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
                return false;
            }

            // 区分消息是收到的还是发送的
            final boolean received = imMessage.toUserId.getOrDefault(0L) == IMSessionManager.getInstance().getSessionUserId();
            HolderFinder holderFinder = new HolderFinder();
            holderFinder.position = position;
            holderFinder.itemObject = itemObject;
            holderFinder.imMessage = imMessage;
            holderFinder.innerActivity = innerActivity;
            holderFinder.lifecycle = lifecycle;
            holderFinder.received = received;
            out[0] = holderFinder;
            return true;
        }

        public static class HolderFinder {
            public int position;
            public UnionTypeItemObject itemObject;
            public IMMessage imMessage;
            public Activity innerActivity;
            public Lifecycle lifecycle;

            // 区分消息是收到的还是发送的
            public boolean received;
        }

        public interface DefaultPreviewAction {
            boolean onDefaultPreviewAction(@NonNull UnionTypeViewHolder holder, long targetUserId, @NonNull HolderFinder finder);
        }

        public static boolean showPreview(UnionTypeViewHolder holder, long targetUserId, @Nullable DefaultPreviewAction defaultPreviewAction) {
            HolderFinder[] holderFinders = new HolderFinder[1];
            if (!getHolderFinder(holder, holderFinders)) {
                return false;
            }
            HolderFinder holderFinder = holderFinders[0];
            if (holderFinder.imMessage.type.isUnset()) {
                SampleLog.e("imMessage type is unset %s", holderFinder.imMessage);
                return false;
            }
            final long type = holderFinder.imMessage.type.get();

            // TODO
            SampleLog.e("imMessage type is unknown %s", holderFinder.imMessage);
            return false;
        }

        public static boolean showMenu(UnionTypeViewHolder holder) {
            final HolderFinder[] holderFinders = new HolderFinder[1];
            if (!getHolderFinder(holder, holderFinders)) {
                return false;
            }
            final HolderFinder holderFinder = holderFinders[0];
            if (holderFinder.imMessage.type.isUnset()) {
                SampleLog.e("imMessage type is unset %s", holderFinder.imMessage);
                return false;
            }
            final long type = holderFinder.imMessage.type.get();
            if (type == IMConstants.MessageType.TEXT) {
                // 文字
                View anchorView = holder.itemView.findViewById(R.id.message_text);
                if (anchorView == null) {
                    SampleLog.v("showMenu MESSAGE_TYPE_TEXT R.id.message_text not found");
                    return false;
                }

                if (anchorView.getWidth() <= 0 || anchorView.getHeight() <= 0) {
                    SampleLog.v("showMenu anchor view not layout");
                    return false;
                }

                IMChatMessageMenuDialog menuDialog = new IMChatMessageMenuDialog(
                        holderFinder.innerActivity,
                        holderFinder.innerActivity.findViewById(Window.ID_ANDROID_CONTENT),
                        anchorView,
                        0,
                        new String[]{
                                I18nResources.getString(R.string.imsdk_sample_menu_copy),
                                I18nResources.getString(R.string.imsdk_sample_menu_recall),
                        });
                menuDialog.setOnIMMenuClickListener((menuText, menuIndex) -> {
                    if (menuIndex == 0) {
                        // 复制
                        ClipboardUtil.copy(holderFinder.imMessage.body.getOrDefault(""));
                    } else if (menuIndex == 1) {
                        // 撤回
                        revoke(holder);
                    } else {
                        SampleLog.e("showMenu onItemMenuClick invalid menuText:%s, menuIndex:%s",
                                menuText, menuIndex);
                    }
                });
                menuDialog.show();
                return true;
            }

            // TODO
            SampleLog.e("imMessage type is unknown %s", holderFinder.imMessage);
            return false;
        }

        /**
         * 撤回
         */
        private static void revoke(UnionTypeViewHolder holder) {
            final HolderFinder[] holderFinders = new HolderFinder[1];
            if (!getHolderFinder(holder, holderFinders)) {
                return;
            }
            final HolderFinder holderFinder = holderFinders[0];
            final IMMessage message = holderFinder.imMessage;
            IMMessageQueueManager.getInstance().enqueueRevokeActionMessage(message);
        }

    }

}
