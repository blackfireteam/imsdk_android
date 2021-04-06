package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.idonans.uniontype.Host;
import com.idonans.uniontype.UnionTypeViewHolder;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.common.imagepicker.ImageData;
import com.masonsoft.imsdk.sample.databinding.UnionTypeAppImplImagePicker3PagerBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

public class ImagePicker3PagerViewHolder extends UnionTypeViewHolder {

    private final UnionTypeAppImplImagePicker3PagerBinding mBinding;

    public ImagePicker3PagerViewHolder(@NonNull Host host) {
        super(host, R.layout.union_type_app_impl_image_picker_3_pager);
        mBinding = UnionTypeAppImplImagePicker3PagerBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<ImageData.ImageInfo> itemObject = (DataObject<ImageData.ImageInfo>) originObject;
        final ImageData.ImageInfo imageInfo = itemObject.object;
        final ImageData imageData = itemObject.getExtObjectObject1(null);

        mBinding.image.setPhotoUri(Uri.parse(imageInfo.path));

        mBinding.image.setOnPhotoTapListener((view, x, y) -> {
            if (itemObject.getExtHolderItemClick1() != null) {
                itemObject.getExtHolderItemClick1().onItemClick(ImagePicker3PagerViewHolder.this);
            }
        });

        /*
        // FIXME
        mImage.setOnViewTapListener((view, x, y) -> {
            if (host.getAdapter() instanceof OnItemClickAdapter) {
                OnItemClickAdapter onItemClickAdapter = (OnItemClickAdapter) host.getAdapter();
                if (onItemClickAdapter.onItemClickListener != null) {
                    onItemClickAdapter.onItemClickListener.ItemClick(itemObject.object, position);
                }
            }
        });
        */
    }

}
