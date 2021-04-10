package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.app.Activity;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.IMConversation;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.chat.SingleChatActivity;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplImConversationBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;
import io.github.idonans.uniontype.UnionTypeViewHolder;

public class IMConversationViewHolder extends UnionTypeViewHolder {

    private final ImsdkSampleUnionTypeImplImConversationBinding mBinding;

    public IMConversationViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_im_conversation);
        mBinding = ImsdkSampleUnionTypeImplImConversationBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<IMConversation> itemObject = (DataObject<IMConversation>) originObject;
        final IMConversation conversation = itemObject.object;

        final long sessionUserId = conversation._sessionUserId.get();
        final long conversationId = conversation.id.get();
        final long targetUserId = conversation.targetUserId.get();

        mBinding.avatar.setTargetUserId(targetUserId);
        mBinding.avatar.setBorderColor(false);
        mBinding.name.setTargetUserId(targetUserId);

        mBinding.unreadCountView.setConversation(sessionUserId, conversationId);
        mBinding.unreadCountView.setOnlyDrawableBackground(false);

        mBinding.time.setConversation(sessionUserId, conversationId);
        mBinding.msg.setConversation(sessionUserId, conversationId);

        ViewUtil.onClick(itemView, v -> {
            final Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }

            SingleChatActivity.start(innerActivity, targetUserId);
        });
        itemView.setOnLongClickListener(v -> {
            final Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return false;
            }

            // TODO FIXME
            /*
            View anchorView = itemView;
            new IMChatConversationMenuDialog(innerActivity,
                    innerActivity.findViewById(Window.ID_ANDROID_CONTENT),
                    anchorView,
                    0,
                    new String[]{"删除"}) {
                @Override
                protected void onShow() {
                    super.onShow();
                    anchorView.setBackgroundColor(0xfff2f4f5);
                    requestParentDisallowInterceptTouchEvent(anchorView);
                }

                @Override
                protected void onHide() {
                    super.onHide();
                    anchorView.setBackground(null);
                }
            }.setOnIMMenuClickListener((menuText, menuIndex) -> {
                if (menuIndex == 0) {
                    Threads.postBackground(() -> ImManager.getInstance().deleteChatConversation(conversation));
                } else {
                    Timber.e("invalid menuIndex:%s, menuText:%s", menuIndex, menuText);
                }
            }).show();
            */
            return true;
        });
    }

    public static void requestParentDisallowInterceptTouchEvent(View view) {
        if (view == null) {
            return;
        }
        ViewParent viewParent = view.getParent();
        if (viewParent != null) {
            viewParent.requestDisallowInterceptTouchEvent(true);
        }
    }

}
