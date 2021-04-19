package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.app.chat.SingleChatActivity;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplHomeSparkBinding;
import com.masonsoft.imsdk.sample.entity.Spark;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;
import io.github.idonans.uniontype.UnionTypeViewHolder;

/**
 * home spark
 */
public class HomeSparkViewHolder extends UnionTypeViewHolder {

    private final ImsdkSampleUnionTypeImplHomeSparkBinding mBinding;

    public HomeSparkViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_home_spark);
        mBinding = ImsdkSampleUnionTypeImplHomeSparkBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<Spark> itemObject = (DataObject<Spark>) originObject;
        final Spark spark = itemObject.object;

        mBinding.imageLayout.setUrl(spark.pic);
        mBinding.username.setTargetUserId(spark.userId);

        ViewUtil.onClick(itemView, v -> {
            final Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }

            // TODO FIXME
        });
        ViewUtil.onClick(mBinding.actionWink, v -> {
            final Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }

            // TODO FIXME
            // send wink message ?
        });
        ViewUtil.onClick(mBinding.actionMessage, v -> {
            final Activity innerActivity = host.getActivity();
            if (innerActivity == null) {
                SampleLog.e(Constants.ErrorLog.ACTIVITY_IS_NULL);
                return;
            }

            if (spark.userId <= 0) {
                SampleLog.e(Constants.ErrorLog.INVALID_USER_ID);
                return;
            }

            SingleChatActivity.start(innerActivity, spark.userId);
        });
    }

}
