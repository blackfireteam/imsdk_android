package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.chat.SingleChatActivity;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplDiscoverUserBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;
import io.github.idonans.uniontype.UnionTypeViewHolder;

/**
 * discover user
 */
public class DiscoverUserViewHolder extends UnionTypeViewHolder {

    private final ImsdkSampleUnionTypeImplDiscoverUserBinding mBinding;

    public DiscoverUserViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_discover_user);
        mBinding = ImsdkSampleUnionTypeImplDiscoverUserBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<Long> itemObject = (DataObject<Long>) originObject;
        final long userId = itemObject.object;

        mBinding.avatar.setTargetUserId(userId);
        mBinding.username.setTargetUserId(userId);

        ViewUtil.onClick(itemView, v -> {
            final Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }

            if (userId <= 0) {
                SampleLog.e(Constants.ErrorLog.INVALID_USER_ID);
                return;
            }

            SingleChatActivity.start(innerActivity, userId);
        });
    }

}
