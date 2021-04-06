package com.masonsoft.imsdk.sample.uniontype.viewholder;

import androidx.annotation.NonNull;

import com.idonans.lang.util.ViewUtil;
import com.idonans.uniontype.Host;
import com.idonans.uniontype.UnionTypeViewHolder;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.common.imagepicker.ImageData;
import com.masonsoft.imsdk.sample.databinding.UnionTypeAppImplImagePicker3BucketBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

public class ImagePickerBucketViewHolder extends UnionTypeViewHolder {

    private final UnionTypeAppImplImagePicker3BucketBinding mBinding;

    public ImagePickerBucketViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_image_picker_bucket);
        mBinding = UnionTypeAppImplImagePicker3BucketBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<ImageData.ImageBucket> itemObject = (DataObject<ImageData.ImageBucket>) originObject;
        //
        final ImageData.ImageBucket imageBucket = itemObject.object;
        final ImageData imageData = itemObject.getExtObjectObject1(null);
        String url = null;
        if (imageBucket.cover != null) {
            url = imageBucket.cover.path;
        }
        mBinding.image.setUrl(url);
        mBinding.count.setText(String.valueOf(imageBucket.imageInfos.size()));
        if (imageBucket.allImageInfos) {
            // FIXME
            mBinding.title.setText("所有照片");
        } else {
            mBinding.title.setText(imageBucket.name);
        }
        ViewUtil.onClick(itemView, v -> {
            if (itemObject.getExtHolderItemClick1() != null) {
                itemObject.getExtHolderItemClick1().onItemClick(ImagePickerBucketViewHolder.this);
            }

            /*
            // TODO
            // FIXME
            if (host.getAdapter() instanceof OnItemClickAdapter) {
                OnItemClickAdapter onItemClickAdapter = (OnItemClickAdapter) host.getAdapter();
                if (onItemClickAdapter.onItemClickListener != null) {
                    onItemClickAdapter.onItemClickListener.ItemClick(itemObject.object, position);
                }
            }
            */
        });
    }

}
