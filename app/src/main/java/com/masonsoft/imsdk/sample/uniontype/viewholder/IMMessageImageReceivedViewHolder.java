package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplImMessageImageReceivedBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;

public class IMMessageImageReceivedViewHolder extends IMMessageImageViewHolder {

    private final ImsdkSampleUnionTypeImplImMessageImageReceivedBinding mBinding;

    public IMMessageImageReceivedViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_im_message_image_received);
        mBinding = ImsdkSampleUnionTypeImplImMessageImageReceivedBinding.bind(itemView);
    }

    @Override
    protected void onBindItemObject(int position, @NonNull DataObject<IMMessage> itemObject) {
        super.onBindItemObject(position, itemObject);
        final IMMessage message = itemObject.object;

        mBinding.avatar.setTargetUserId(message.fromUserId.get());
        mBinding.avatar.setShowBorder(false);

        ViewUtil.onClick(mBinding.avatar, v -> {
            final Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }

            // TODO FIXME
            SampleLog.e("require open profile");
        });
    }

}
