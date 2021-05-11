package com.masonsoft.imsdk.sample.uniontype.viewholder;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.common.ItemClickUnionTypeAdapter;
import com.masonsoft.imsdk.sample.common.mediapicker.MediaData;
import com.masonsoft.imsdk.sample.common.mediapicker.UnionTypeMediaData;
import com.masonsoft.imsdk.sample.databinding.ImsdkSampleUnionTypeImplImagePickerGridBinding;
import com.masonsoft.imsdk.sample.uniontype.DataObject;

import io.github.idonans.lang.util.ViewUtil;
import io.github.idonans.uniontype.Host;
import io.github.idonans.uniontype.UnionTypeViewHolder;

public class ImagePickerGridViewHolder extends UnionTypeViewHolder {

    private final ImsdkSampleUnionTypeImplImagePickerGridBinding mBinding;

    public ImagePickerGridViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_image_picker_grid);
        mBinding = ImsdkSampleUnionTypeImplImagePickerGridBinding.bind(itemView);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<MediaData.MediaInfo> itemObject = (DataObject<MediaData.MediaInfo>) originObject;
        MediaData.MediaInfo mediaInfo = itemObject.object;
        MediaData mediaData = itemObject.getExtObjectObject1(null);
        UnionTypeMediaData unionTypeMediaData = itemObject.getExtObjectObject2(null);

        mBinding.image.setUrl(mediaInfo.uri.toString());
        int selectedIndex = mediaData.indexOfSelected(mediaInfo);
        if (selectedIndex >= 0) {
            mBinding.flagSelect.setSelected(true);
            mBinding.flagSelectText.setText(String.valueOf(selectedIndex + 1));
        } else {
            mBinding.flagSelect.setSelected(false);
            mBinding.flagSelectText.setText(null);
        }

        ViewUtil.onClick(mBinding.flagSelect, v -> {
            int currentSelectedIndex = mediaData.indexOfSelected(mediaInfo);
            if (currentSelectedIndex >= 0) {
                // 取消选中
                if (mediaData.mMediaSelector.canDeselect(mediaData.mMediaInfoListSelected, currentSelectedIndex, mediaInfo)) {
                    mediaData.mMediaInfoListSelected.remove(mediaInfo);
                }
            } else {
                // 选中
                if (mediaData.mMediaSelector.canSelect(mediaData.mMediaInfoListSelected, mediaInfo)) {
                    mediaData.mMediaInfoListSelected.add(mediaInfo);
                }
            }
            host.getAdapter().notifyDataSetChanged();
            if (unionTypeMediaData != null) {
                unionTypeMediaData.childClick();
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
