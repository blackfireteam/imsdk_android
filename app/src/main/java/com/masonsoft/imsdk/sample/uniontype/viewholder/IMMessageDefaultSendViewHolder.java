package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.core.IMMessage;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplImMessageDefaultSendBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;

public class IMMessageDefaultSendViewHolder extends IMMessageDefaultViewHolder {

    private final ImsdkSampleUnionTypeImplImMessageDefaultSendBinding mBinding;

    public IMMessageDefaultSendViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_im_message_default_send);
        mBinding = ImsdkSampleUnionTypeImplImMessageDefaultSendBinding.bind(itemView);
    }

    @Override
    protected void onBindItemObject(int position, @NonNull DataObject<IMMessage> itemObject) {
        super.onBindItemObject(position, itemObject);
        final IMMessage message = itemObject.object;
        mBinding.sendStatusView.setMessage(message);

        mBinding.avatar.setTargetUserId(message.fromUserId.getOrDefault(0L));
        mBinding.avatar.setShowBorder(false);

        mBinding.readStatusView.setMessage(message);

        ViewUtil.onClick(mBinding.avatar, v -> {
            Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }

            // TODO FIXME open profile ?
            IMLog.w("require open profile");
        });
    }

}