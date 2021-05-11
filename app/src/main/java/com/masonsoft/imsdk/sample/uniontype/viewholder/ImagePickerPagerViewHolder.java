package com.masonsoft.imsdk.sample.uniontype.viewholder;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.common.ItemClickUnionTypeAdapter;
import com.masonsoft.imsdk.sample.common.mediapicker.MediaData;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplImagePickerPagerBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

import io.github.idonans.uniontype.Host;
import io.github.idonans.uniontype.UnionTypeViewHolder;

public class ImagePickerPagerViewHolder extends UnionTypeViewHolder {

    private final ImsdkSampleUnionTypeImplImagePickerPagerBinding mBinding;

    public ImagePickerPagerViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_image_picker_pager);
        mBinding = ImsdkSampleUnionTypeImplImagePickerPagerBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<MediaData.MediaInfo> itemObject = (DataObject<MediaData.MediaInfo>) originObject;
        final MediaData.MediaInfo mediaInfo = itemObject.object;
        final MediaData mediaData = itemObject.getExtObjectObject1(null);

        SampleLog.v("ImagePickerPagerViewHolder onBind position:%s uri:%s", position, mediaInfo.uri);
        mBinding.image.setPhotoUri(mediaInfo.uri);

        mBinding.image.setOnPhotoTapListener((view, x, y) -> {
            if (itemObject.getExtHolderItemClick1() != null) {
                itemObject.getExtHolderItemClick1().onItemClick(ImagePickerPagerViewHolder.this);
            }

            if (host.getAdapter() instanceof ItemClickUnionTypeAdapter) {
                final ItemClickUnionTypeAdapter adapter = (ItemClickUnionTypeAdapter) host.getAdapter();
                if (adapter.getOnItemClickListener() != null) {
                    adapter.getOnItemClickListener().onItemClick(ImagePickerPagerViewHolder.this);
                }
            }
        });
    }

}
