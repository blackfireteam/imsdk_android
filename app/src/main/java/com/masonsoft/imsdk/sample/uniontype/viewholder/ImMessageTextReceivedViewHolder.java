package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.app.Activity;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.idonans.lang.util.ViewUtil;
import com.idonans.uniontype.Host;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.xmqvip.xiaomaiquan.R;
import com.xmqvip.xiaomaiquan.common.widget.UserCacheAvatar;
import com.xmqvip.xiaomaiquan.module.userprofile.UserProfileActivity;

import butterknife.BindView;

public class ImMessageTextReceivedViewHolder extends ImMessageTextViewHolder {

    @BindView(R.id.avatar)
    UserCacheAvatar mAvatar;

    public ImMessageTextReceivedViewHolder(@NonNull Host host) {
        super(host, R.layout.union_type_app_impl_im_message_text_received);
    }

    @CallSuper
    @Override
    public void onBind(int position, @NonNull Object originObject) {
        super.onBind(position, originObject);
        //noinspection unchecked
        final DataObject<IMMessage> itemObject = (DataObject<IMMessage>) originObject;
        final IMMessage imMessage = itemObject.object;

        mAvatar.setTargetUserId(imMessage.fromUserId);
        mAvatar.setShowBorder(false);

        ViewUtil.onClick(mAvatar, v -> {
            Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                SampleLog.e(Constants.Tip.ACTIVITY_IS_NULL);
                return;
            }
            UserProfileActivity.start(innerActivity, mAvatar.getTargetUserId());
        });
    }

}
