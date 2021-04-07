package com.masonsoft.imsdk.sample.uniontype.viewholder;

import androidx.annotation.NonNull;

import com.idonans.lang.util.ViewUtil;
import com.idonans.uniontype.Host;
import com.idonans.uniontype.UnionTypeViewHolder;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.common.ItemClickUnionTypeAdapter;
import com.masonsoft.imsdk.sample.common.imagepicker.ImageData;
import com.masonsoft.imsdk.sample.common.imagepicker.UnionTypeImageData;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplImagePickerGridBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

public class ImagePickerGridViewHolder extends UnionTypeViewHolder {

    private final ImsdkSampleUnionTypeImplImagePickerGridBinding mBinding;

    public ImagePickerGridViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_image_picker_grid);
        mBinding = ImsdkSampleUnionTypeImplImagePickerGridBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<ImageData.ImageInfo> itemObject = (DataObject<ImageData.ImageInfo>) originObject;
        ImageData.ImageInfo imageInfo = itemObject.object;
        ImageData imageData = itemObject.getExtObjectObject1(null);
        UnionTypeImageData unionTypeImageData = itemObject.getExtObjectObject2(null);

        mBinding.image.setUrl(imageInfo.uri.toString());
        int selectedIndex = imageData.indexOfSelected(imageInfo);
        if (selectedIndex >= 0) {
            mBinding.flagSelect.setSelected(true);
            mBinding.flagSelectText.setText(String.valueOf(selectedIndex + 1));
        } else {
            mBinding.flagSelect.setSelected(false);
            mBinding.flagSelectText.setText(null);
        }

        ViewUtil.onClick(mBinding.flagSelect, v -> {
            int currentSelectedIndex = imageData.indexOfSelected(imageInfo);
            if (currentSelectedIndex >= 0) {
                // 取消选中
                if (imageData.imageSelector.canDeselect(imageData.imageInfoListSelected, currentSelectedIndex, imageInfo)) {
                    imageData.imageInfoListSelected.remove(imageInfo);
                }
            } else {
                // 选中
                if (imageData.imageSelector.canSelect(imageData.imageInfoListSelected, imageInfo)) {
                    imageData.imageInfoListSelected.add(imageInfo);
                }
            }
            host.getAdapter().notifyDataSetChanged();
            if (unionTypeImageData != null) {
                unionTypeImageData.childClick();
            }
        });
        ViewUtil.onClick(itemView, v -> {
            if (itemObject.getExtHolderItemClick1() != null) {
                itemObject.getExtHolderItemClick1().onItemClick(ImagePickerGridViewHolder.this);
            }
            if (host.getAdapter() instanceof ItemClickUnionTypeAdapter) {
                final ItemClickUnionTypeAdapter adapter = (ItemClickUnionTypeAdapter) host.getAdapter();
                if (adapter.getOnItemClickListener() != null) {
                    adapter.getOnItemClickListener().onItemClick(ImagePickerGridViewHolder.this);
                }
            }
        });
    }

}
