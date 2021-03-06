package com.masonsoft.imsdk.uikit.uniontype.viewholder;

import android.app.Activity;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.MSIMConversation;
import com.masonsoft.imsdk.MSIMManager;
import com.masonsoft.imsdk.core.I18nResources;
import com.masonsoft.imsdk.uikit.IMUIKitComponentManager;
import com.masonsoft.imsdk.uikit.IMUIKitConstants;
import com.masonsoft.imsdk.uikit.IMUIKitLog;
import com.masonsoft.imsdk.uikit.R;
import com.masonsoft.imsdk.uikit.common.impopup.IMChatConversationMenuDialog;
import com.masonsoft.imsdk.uikit.databinding.ImsdkSampleUnionTypeImplImConversationBinding;
import com.masonsoft.imsdk.uikit.uniontype.DataObject;

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
        final DataObject<MSIMConversation> itemObject = (DataObject<MSIMConversation>) originObject;
        final MSIMConversation conversation = itemObject.object;

        final long sessionUserId = conversation.getSessionUserId();
        final long conversationId = conversation.getConversationId();
        final long targetUserId = conversation.getTargetUserId();

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
                IMUIKitLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }

            IMUIKitComponentManager.getInstance().dispatchConversationViewClick(
                    innerActivity,
                    sessionUserId,
                    conversationId,
                    targetUserId
            );
        });
        itemView.setOnLongClickListener(v -> {
            final Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                IMUIKitLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_IS_NULL);
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
                    MSIMManager.getInstance().getConversationManager().deleteConversation(
                            sessionUserId,
                            conversation
                    );
                } else {
                    IMUIKitLog.e("IMChatConversationMenuDialog onItemMenuClick invalid menuId:%s, menuText:%s, menuView:%s",
                            menuId, menuText, menuView);
                }
            });
            menuDialog.show();
            return true;
        });
    }

}
