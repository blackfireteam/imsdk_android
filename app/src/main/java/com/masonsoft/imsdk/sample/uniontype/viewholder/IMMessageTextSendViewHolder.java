package com.masonsoft.imsdk.sample.uniontype.viewholder;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.idonans.uniontype.Host;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImMessageTextSendBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

public class IMMessageTextSendViewHolder extends IMMessageTextViewHolder {

    private final ImsdkSampleUnionTypeImMessageTextSendBinding mBinding;

    public IMMessageTextSendViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_im_message_text_send);
        mBinding = ImsdkSampleUnionTypeImMessageTextSendBinding.bind(itemView);
    }

    @CallSuper
    @Override
    public void onBind(int position, @NonNull Object originObject) {
        super.onBind(position, originObject);
        //noinspection unchecked
        final DataObject<IMMessage> itemObject = (DataObject<IMMessage>) originObject;
        final IMMessage imMessage = itemObject.object;

        mSendStatusView.setTargetLocalMessageId(imMessage.id);
    }

}
