package com.masonsoft.imsdk.sample.common.mediapicker;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.util.TipUtil;

import java.util.List;

/**
 * 对图片选择器的细节过滤，如允许显示哪些图片，允许选中哪些图片等。
 */
public interface MediaSelector {

    /**
     * 允许显示这张图片，返回 true, 否则返回 false.
     */
    boolean accept(@NonNull MediaData.MediaInfo info);

    /**
     * 是否允许选中目标图片，允许返回 true, 否则返回 false.
     */
    boolean canSelect(@NonNull List<MediaData.MediaInfo> mediaInfoListSelected, @NonNull MediaData.MediaInfo info);

    /**
     * 是否允许取消选中目标图片，可以取消选中返回 true, 否则返回 false.
     */
    boolean canDeselect(@NonNull List<MediaData.MediaInfo> mediaInfoListSelected, int currentSelectedIndex, @NonNull MediaData.MediaInfo info);

    /**
     * 当前选中状态下是否可以显示完成按钮
     *
     * @param mediaInfoListSelected
     */
    boolean canFinishSelect(@NonNull List<MediaData.MediaInfo> mediaInfoListSelected);

    class SimpleMediaSelector implements MediaSelector {

        @Override
        public boolean accept(@NonNull MediaData.MediaInfo info) {
            return true;
        }

        @Override
        public boolean canSelect(@NonNull List<MediaData.MediaInfo> mediaInfoListSelected, @NonNull MediaData.MediaInfo info) {
            if (!info.isImageMimeType()) {
                TipUtil.show(R.string.imsdk_sample_tip_image_invalid);
                return false;
            }
            if (info.isImageMemorySizeTooLarge()) {
                TipUtil.show(R.string.imsdk_sample_tip_image_too_large);
                return false;
            }
            if (info.size > Constants.SELECTOR_MAX_IMAGE_FILE_SIZE) {
                TipUtil.show(R.string.imsdk_sample_tip_image_too_large);
                return false;
            }
            return true;
        }

        @Override
        public boolean canDeselect(@NonNull List<MediaData.MediaInfo> mediaInfoListSelected, int currentSelectedIndex, @NonNull MediaData.MediaInfo info) {
            return true;
        }

        @Override
        public boolean canFinishSelect(@NonNull List<MediaData.MediaInfo> mediaInfoListSelected) {
            return !mediaInfoListSelected.isEmpty();
        }
    }

}
