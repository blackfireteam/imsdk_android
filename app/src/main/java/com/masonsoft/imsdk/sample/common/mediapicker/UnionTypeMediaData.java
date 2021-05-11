package com.masonsoft.imsdk.sample.common.mediapicker;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.idonans.uniontype.UnionTypeItemObject;

public class UnionTypeMediaData {

    @NonNull
    final MediaData mMediaData;
    @NonNull
    final Map<MediaData.MediaBucket, List<UnionTypeItemObject>> unionTypeGridItemsMap;
    @NonNull
    final Map<MediaData.MediaBucket, List<UnionTypeItemObject>> unionTypePagerItemsMap;
    @NonNull
    final List<UnionTypeItemObject> unionTypeBucketItems;

    public int pagerPendingIndex;
    MediaPickerDialog dialog;

    UnionTypeMediaData(MediaPickerDialog dialog, MediaData mediaData) {
        this.dialog = dialog;
        this.mMediaData = mediaData;

        this.unionTypeGridItemsMap = new HashMap<>();
        this.unionTypePagerItemsMap = new HashMap<>();
        this.unionTypeBucketItems = new ArrayList<>(this.mMediaData.allSubBuckets.size());

        for (MediaData.MediaBucket bucket : this.mMediaData.allSubBuckets) {
            List<UnionTypeItemObject> gridItems = new ArrayList<>(bucket.mediaInfoList.size());
            List<UnionTypeItemObject> pagerItems = new ArrayList<>(bucket.mediaInfoList.size());

            for (MediaData.MediaInfo mediaInfo : bucket.mediaInfoList) {
                gridItems.add(UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IMAGE_PICKER_GRID,
                        new DataObject<>(mediaInfo)
                                .putExtObjectObject1(this.mMediaData)
                                .putExtObjectObject2(UnionTypeMediaData.this)));

                pagerItems.add(UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_IMPL_IMAGE_PICKER_PAGER,
                        new DataObject<>(mediaInfo)
                                .putExtObjectObject1(this.mMediaData)
                                .putExtObjectObject2(UnionTypeMediaData.this)));
            }

            this.unionTypeGridItemsMap.put(bucket, gridItems);
            this.unionTypePagerItemsMap.put(bucket, pagerItems);
            unionTypeBucketItems.add(UnionTypeItemObject.valueOf(
                    UnionTypeMapperImpl.UNION_TYPE_IMPL_IMAGE_PICKER_BUCKET,
                    new DataObject<>(bucket)
                            .putExtObjectObject1(this.mMediaData)
                            .putExtObjectObject2(UnionTypeMediaData.this)));
        }
    }

    public void childClick() {
        dialog.mGridView.updateConfirmNextStatus();
    }
}
