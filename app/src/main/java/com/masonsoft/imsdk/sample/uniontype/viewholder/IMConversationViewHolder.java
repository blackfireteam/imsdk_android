package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.app.Activity;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMConversation;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.core.IMMessageQueueManager;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.chat.SingleChatActivity;
import com.masonsoft.imsdk.sample.common.impopup.IMChatConversationMenuDialog;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplImConversationBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

import java.util.ArrayList;
import java.util.List;

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

        // debug
        mBinding.conversationDebugView.setConversation(sessionUserId, conversationId);

        mBinding.avatar.setTargetUserId(targetUserId);
        mBinding.avatar.setBorderColor(false);
        mBinding.name.setTargetUserId(targetUserId);
        mBinding.userVerifiedFlag.setTargetUserId(targetUserId);
        mBinding.userGoldFlag.setTargetUserId(targetUserId);

        mBinding.unreadCountView.setConversation(sessionUserId, conversationId);

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

            final int MENU_ID_DELETE = 1;

            final List<String> menuList = new ArrayList<>();
            final List<Integer> menuIdList = new ArrayList<>();
            menuList.add(I18nResources.getString(R.string.imsdk_sample_menu_delete));
            menuIdList.add(MENU_ID_DELETE);

            final View anchorView = itemView;
            final IMChatConversationMenuDialog menuDialog = new IMChatConversationMenuDialog(innerActivity,
                    innerActivity.findViewById(Window.ID_ANDROID_CONTENT),
                    anchorView,
                    0,
                    menuList,
                    menuIdList) {
                @Override
                protected void onShow() {
                    super.onShow();
                    anchorView.setBackgroundColor(0xfff2f4f5);
                    ViewUtil.requestParentDisallowInterceptTouchEvent(anchorView);
                }

                @Override
                protected void onHide() {
                    super.onHide();
                    anchorView.setBackground(null);
                }
            };
            menuDialog.setOnIMMenuClickListener((menuId, menuText, menuView) -> {
                if (menuId == MENU_ID_DELETE) {
                    // 删除
                    IMMessageQueueManager.getInstance().enqueueDeleteConversationActionMessage(conversation);
                } else {
                    SampleLog.e("IMChatConversationMenuDialog onItemMenuClick invalid menuId:%s, menuText:%s, menuView:%s",
                            menuId, menuText, menuView);
                }
            });
            menuDialog.show();
            return true;
        });
    }

}
