package com.masonsoft.imsdk.sample.uniontype.viewholder;

import androidx.annotation.NonNull;

import com.idonans.lang.util.ViewUtil;
import com.idonans.uniontype.Host;
import com.idonans.uniontype.UnionTypeViewHolder;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.common.imagepicker.ImageData;
import com.masonsoft.imsdk.sample.common.imagepicker.UnionTypeImageData;
import com.masonsoft.imsdk.sample.databinding.UnionTypeAppImplImagePicker3GridBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

public class ImagePicker3GridViewHolder extends UnionTypeViewHolder {

    private static final long DURATION = 120L;
    private final UnionTypeAppImplImagePicker3GridBinding mBinding;

    public ImagePicker3GridViewHolder(@NonNull Host host) {
        super(host, R.layout.union_type_app_impl_image_picker_3_grid);
        mBinding = UnionTypeAppImplImagePicker3GridBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<ImageData.ImageInfo> itemObject = (DataObject<ImageData.ImageInfo>) originObject;
        ImageData.ImageInfo imageInfo = itemObject.object;
        ImageData imageData = itemObject.getExtObjectObject1(null);
        UnionTypeImageData unionTypeImageData = itemObject.getExtObjectObject2(null);

        mBinding.image.setUrl(imageInfo.path);
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
                if (imageData.imageSelector.canDeselect(imageData.imageInfosSelected, currentSelectedIndex, imageInfo)) {
                    imageData.imageInfosSelected.remove(imageInfo);
                }
            } else {
                // 选中
                if (imageData.imageSelector.canSelect(imageData.imageInfosSelected, imageInfo)) {
                    imageData.imageInfosSelected.add(imageInfo);
                }
            }
            host.getAdapter().notifyDataSetChanged();
            if (unionTypeImageData != null) {
                unionTypeImageData.childClick();
            }
        });
        ViewUtil.onClick(itemView, v -> {
            if (itemObject.getExtHolderItemClick1() != null) {
                itemObject.getExtHolderItemClick1().onItemClick(ImagePicker3GridViewHolder.this);
            }
            /*
            // FIXME
            if (host.getAdapter() instanceof OnItemClickAdapter) {
                OnItemClickAdapter onItemClickAdapter = (OnItemClickAdapter) host.getAdapter();
                if (onItemClickAdapter.onItemClickListener != null) {
                    onItemClickAdapter.onItemClickListener.ItemClick(itemObject.object, position);
                }
            }*/
        });

    }

}
