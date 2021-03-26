package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.app.Activity;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.idonans.lang.util.ViewUtil;
import com.idonans.uniontype.Host;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMLog;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImMessageTextReceivedBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

public class IMMessageTextReceivedViewHolder extends IMMessageTextViewHolder {

    private final ImsdkSampleUnionTypeImMessageTextReceivedBinding mBinding;

    public IMMessageTextReceivedViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_im_message_text_received);
        mBinding = ImsdkSampleUnionTypeImMessageTextReceivedBinding.bind(itemView);
    }

    @CallSuper
    @Override
    public void onBind(int position, @NonNull Object originObject) {
        super.onBind(position, originObject);
        //noinspection unchecked
        final DataObject<IMMessage> itemObject = (DataObject<IMMessage>) originObject;
        final IMMessage imMessage = itemObject.object;

        mBinding.avatar.setTargetUserId(imMessage.fromUserId.getOrDefault(0L));
        mBinding.avatar.setShowBorder(false);

        ViewUtil.onClick(mBinding.avatar, v -> {
            Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }

            // TODO open profile ?
            IMLog.w("require open profile");
        });
    }

}
