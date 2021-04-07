package com.masonsoft.imsdk.sample.uniontype.viewholder;

import androidx.annotation.NonNull;

import com.idonans.uniontype.Host;
import com.idonans.uniontype.UnionTypeViewHolder;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.sample.common.ItemClickUnionTypeAdapter;
import com.masonsoft.imsdk.sample.common.imagepicker.ImageData;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplImagePickerPagerBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

public class ImagePickerPagerViewHolder extends UnionTypeViewHolder {

    private final ImsdkSampleUnionTypeImplImagePickerPagerBinding mBinding;

    public ImagePickerPagerViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_image_picker_pager);
        mBinding = ImsdkSampleUnionTypeImplImagePickerPagerBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<ImageData.ImageInfo> itemObject = (DataObject<ImageData.ImageInfo>) originObject;
        final ImageData.ImageInfo imageInfo = itemObject.object;
        final ImageData imageData = itemObject.getExtObjectObject1(null);

        SampleLog.v("ImagePickerPagerViewHolder onBind position:%s uri:%s", position, imageInfo.uri);
        mBinding.image.setPhotoUri(imageInfo.uri);

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
