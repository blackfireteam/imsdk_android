package com.masonsoft.imsdk.uikit.uniontype.viewholder;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.MSIMMessage;
import com.masonsoft.imsdk.uikit.IMUIKitConstants;
import com.masonsoft.imsdk.uikit.IMUIKitLog;
import com.masonsoft.imsdk.uikit.R;
import com.masonsoft.imsdk.uikit.databinding.ImsdkUikitUnionTypeImplImMessageImageSendBinding;
import com.masonsoft.imsdk.uikit.uniontype.DataObject;

import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;

public class IMMessageVideoSendViewHolder extends IMMessageVideoViewHolder {

    private final ImsdkUikitUnionTypeImplImMessageImageSendBinding mBinding;

    public IMMessageVideoSendViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_uikit_union_type_impl_im_message_video_send);
        mBinding = ImsdkUikitUnionTypeImplImMessageImageSendBinding.bind(itemView);
    }

    @Override
    protected void onBindItemObject(int position, @NonNull DataObject<MSIMMessage> itemObject) {
        super.onBindItemObject(position, itemObject);
        final MSIMMessage message = itemObject.object;

        mBinding.sendStatusView.setMessage(message);
        mBinding.progressView.setMessage(message);

        mBinding.avatar.setTargetUserId(message.getSender());
        mBinding.avatar.setShowBorder(false);

        mBinding.readStatusView.setMessage(message);

        ViewUtil.onClick(mBinding.avatar, v -> {
            final Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                IMUIKitLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }

            // TODO open profile ?
            IMUIKitLog.w("require open profile");
        });
    }

}
