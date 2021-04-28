package com.masonsoft.imsdk.sample.common.impopup;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleCommonImChatMessageMenuDialogBinding;
import com.masonsoft.imsdk.util.Preconditions;

import java.util.List;

import io.github.idonans.backstack.dialog.ViewDialog;

/**
 * 长按一个消息的弹框, 内部基于 IMChatMessagePopupView 实现
 */
public class IMChatMessageMenuDialog {

    private final ViewDialog mViewDialog;
    private IMChatMessagePopupView mPopupView;

    public IMChatMessageMenuDialog(Activity activity,
                                   ViewGroup parentView,
                                   View anchorView,
                                   int coverDrawableResId,
                                   List<String> menuList,
                                   List<Integer> menuIdList) {
        mViewDialog = new ViewDialog.Builder(activity)
                .setContentView(R.layout.imsdk_sample_common_im_chat_message_menu_dialog)
                .setParentView(parentView)
                .setOnShowListener(() -> {
                    if (mPopupView != null) {
                        mPopupView.showAnchorViewCover();
                    }
                    onShow();
                })
                .setOnHideListener(cancel -> {
                    if (mPopupView != null) {
                        mPopupView.hideAnchorViewCover();
                    }
                    onHide();
                })
                .dimBackground(false)
                .setCancelable(true)
                .create();
        Preconditions.checkArgument(menuList.size() == menuIdList.size());
        final ImsdkSampleCommonImChatMessageMenuDialogBinding binding = ImsdkSampleCommonImChatMessageMenuDialogBinding.bind(mViewDialog.getContentView());
        mPopupView = binding.popupView;
        mPopupView.showForAnchorView(anchorView, coverDrawableResId, menuList, menuIdList);
        mPopupView.setOnIMMenuClickListener((menuText, menuIndex, menuId) -> {
            if (mOnIMMenuClickListener != null) {
                mOnIMMenuClickListener.onItemMenuClick(menuText, menuIndex, menuId);
            }
            hide();
        });
    }

    public void show() {
        mViewDialog.show();
    }

    public void hide() {
        mViewDialog.hide(false);
    }

    private OnIMMenuClickListener mOnIMMenuClickListener;

    public void setOnIMMenuClickListener(OnIMMenuClickListener onIMMenuClickListener) {
        mOnIMMenuClickListener = onIMMenuClickListener;
    }

    protected void onShow() {
    }

    protected void onHide() {
    }

}
