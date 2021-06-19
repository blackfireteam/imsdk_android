package com.masonsoft.imsdk.uikit.uniontype.viewholder;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.MSIMMessage;
import com.masonsoft.imsdk.uikit.IMUIKitConstants;
import com.masonsoft.imsdk.uikit.IMUIKitLog;
import com.masonsoft.imsdk.uikit.R;
import com.masonsoft.imsdk.uikit.databinding.ImsdkSampleUnionTypeImplImMessageImageReceivedBinding;
import com.masonsoft.imsdk.uikit.uniontype.DataObject;

import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;

public class IMMessageImageReceivedViewHolder extends IMMessageImageViewHolder {

    private final ImsdkSampleUnionTypeImplImMessageImageReceivedBinding mBinding;

    public IMMessageImageReceivedViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_im_message_image_received);
        mBinding = ImsdkSampleUnionTypeImplImMessageImageReceivedBinding.bind(itemView);
    }

    @Override
    protected void onBindItemObject(int position, @NonNull DataObject<MSIMMessage> itemObject) {
        super.onBindItemObject(position, itemObject);
        final MSIMMessage message = itemObject.object;

        mBinding.avatar.setTargetUserId(message.getSender());
        mBinding.avatar.setShowBorder(false);

        ViewUtil.onClick(mBinding.avatar, v -> {
            final Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                IMUIKitLog.e(IMUIKitConstants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }

            // TODO FIXME
            IMUIKitLog.e("require open profile");
        });
    }

}
