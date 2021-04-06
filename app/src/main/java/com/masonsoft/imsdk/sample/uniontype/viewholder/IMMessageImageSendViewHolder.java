package com.masonsoft.imsdk.sample.uniontype.viewholder;

import androidx.annotation.NonNull;

import com.idonans.uniontype.Host;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplImMessageImageSendBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

public class IMMessageImageSendViewHolder extends IMMessageImageViewHolder {

    private final ImsdkSampleUnionTypeImplImMessageImageSendBinding mBinding;

    public IMMessageImageSendViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_im_message_image_send);
        mBinding = ImsdkSampleUnionTypeImplImMessageImageSendBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        super.onBind(position, originObject);
        //noinspection unchecked
        final DataObject<IMMessage> itemObject = (DataObject<IMMessage>) originObject;
        final IMMessage imMessage = itemObject.object;

        mBinding.sendStatusView.setMessage(imMessage);
    }

}
