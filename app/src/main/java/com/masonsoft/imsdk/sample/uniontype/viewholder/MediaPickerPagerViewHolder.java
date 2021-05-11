package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.view.View;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.common.ItemClickUnionTypeAdapter;
import com.masonsoft.imsdk.sample.common.mediapicker.MediaData;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplMediaPickerPagerBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.util.Objects;

import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;
import io.github.idonans.uniontype.UnionTypeViewHolder;

public class MediaPickerPagerViewHolder extends UnionTypeViewHolder {

    private final ImsdkSampleUnionTypeImplMediaPickerPagerBinding mBinding;

    public MediaPickerPagerViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_media_picker_pager);
        mBinding = ImsdkSampleUnionTypeImplMediaPickerPagerBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<MediaData.MediaInfo> itemObject = (DataObject<MediaData.MediaInfo>) originObject;
        final MediaData.MediaInfo mediaInfo = itemObject.object;
        final MediaData mediaData = itemObject.getExtObjectObject1(null);

        SampleLog.v(Objects.defaultObjectTag(this) + " onBind position:%s uri:%s", position, mediaInfo.uri);

        if (mediaInfo.isVideoMimeType()) {
            ViewUtil.setVisibilityIfChanged(mBinding.videoFlag, View.VISIBLE);
            mBinding.durationText.setText(formatDuration(mediaInfo.durationMs));
        } else {
            ViewUtil.setVisibilityIfChanged(mBinding.videoFlag, View.GONE);
            mBinding.durationText.setText(null);
        }
        mBinding.image.setPhotoUri(mediaInfo.uri);

        mBinding.image.setOnPhotoTapListener((view, x, y) -> {
            if (itemObject.getExtHolderItemClick1() != null) {
                itemObject.getExtHolderItemClick1().onItemClick(MediaPickerPagerViewHolder.this);
            }

            if (host.getAdapter() instanceof ItemClickUnionTypeAdapter) {
                final ItemClickUnionTypeAdapter adapter = (ItemClickUnionTypeAdapter) host.getAdapter();
                if (adapter.getOnItemClickListener() != null) {
                    adapter.getOnItemClickListener().onItemClick(MediaPickerPagerViewHolder.this);
                }
            }
        });
    }

    private String formatDuration(long durationMs) {
        final long durationS = (long) Math.ceil(durationMs / 1000f);
        final long min = durationS / 60;
        final long s = durationS % 60;
        return min + ":" + s;
    }

}
