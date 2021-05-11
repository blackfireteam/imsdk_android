package com.masonsoft.imsdk.sample.uniontype.viewholder;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.common.ItemClickUnionTypeAdapter;
import com.masonsoft.imsdk.sample.common.mediapicker.MediaData;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplImagePickerBucketBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;
import io.github.idonans.uniontype.UnionTypeViewHolder;

public class ImagePickerBucketViewHolder extends UnionTypeViewHolder {

    private final ImsdkSampleUnionTypeImplImagePickerBucketBinding mBinding;

    public ImagePickerBucketViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_image_picker_bucket);
        mBinding = ImsdkSampleUnionTypeImplImagePickerBucketBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<MediaData.MediaBucket> itemObject = (DataObject<MediaData.MediaBucket>) originObject;
        //
        final MediaData.MediaBucket mediaBucket = itemObject.object;
        final MediaData mediaData = itemObject.getExtObjectObject1(null);
        String url = null;
        if (mediaBucket.cover != null) {
            url = mediaBucket.cover.uri.toString();
        }
        mBinding.image.setUrl(url);
        mBinding.count.setText(String.valueOf(mediaBucket.mediaInfoList.size()));
        if (mediaBucket.allMediaInfo) {
            mBinding.title.setText(R.string.imsdk_sample_custom_soft_keyboard_item_media_bucket_all);
        } else {
            mBinding.title.setText(mediaBucket.bucketDisplayName);
        }
        ViewUtil.onClick(itemView, v -> {
            if (itemObject.getExtHolderItemClick1() != null) {
                itemObject.getExtHolderItemClick1().onItemClick(ImagePickerBucketViewHolder.this);
            }

            if (host.getAdapter() instanceof ItemClickUnionTypeAdapter) {
                ItemClickUnionTypeAdapter adapter = (ItemClickUnionTypeAdapter) host.getAdapter();
                if (adapter.getOnItemClickListener() != null) {
                    adapter.getOnItemClickListener().onItemClick(ImagePickerBucketViewHolder.this);
                }
            }
        });
    }

}
