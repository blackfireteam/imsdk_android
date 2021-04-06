package com.masonsoft.imsdk.sample.common.imagepicker;

import androidx.annotation.NonNull;

import com.idonans.uniontype.UnionTypeItemObject;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeMapperImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnionTypeImageData {

    @NonNull
    final ImageData imageData;
    @NonNull
    final Map<ImageData.ImageBucket, List<UnionTypeItemObject>> unionTypeGridItemsMap;
    @NonNull
    final Map<ImageData.ImageBucket, List<UnionTypeItemObject>> unionTypePagerItemsMap;
    @NonNull
    final List<UnionTypeItemObject> unionTypeBucketItems;

    public int pagerPendingIndex;
    ImagePicker3Dialog dialog;

    UnionTypeImageData(ImagePicker3Dialog dialog, ImageData imageData) {
        this.dialog = dialog;
        this.imageData = imageData;

        this.unionTypeGridItemsMap = new HashMap<>();
        this.unionTypePagerItemsMap = new HashMap<>();
        this.unionTypeBucketItems = new ArrayList<>(this.imageData.allSubBuckets.size());

        for (ImageData.ImageBucket bucket : this.imageData.allSubBuckets) {
            List<UnionTypeItemObject> gridItems = new ArrayList<>(bucket.imageInfos.size());
            List<UnionTypeItemObject> pagerItems = new ArrayList<>(bucket.imageInfos.size());

            for (ImageData.ImageInfo imageInfo : bucket.imageInfos) {
                gridItems.add(UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_APP_IMPL_IMAGE_PICKER_3_GRID,
                        new DataObject<>(imageInfo)
                                .putExtObjectObject1(this.imageData)
                                .putExtObjectObject2(UnionTypeImageData.this)));

                pagerItems.add(UnionTypeItemObject.valueOf(
                        UnionTypeMapperImpl.UNION_TYPE_APP_IMPL_IMAGE_PICKER_3_PAGER,
                        new DataObject<>(imageInfo)
                                .putExtObjectObject1(this.imageData)
                                .putExtObjectObject2(UnionTypeImageData.this)));
            }

            this.unionTypeGridItemsMap.put(bucket, gridItems);
            this.unionTypePagerItemsMap.put(bucket, pagerItems);
            unionTypeBucketItems.add(UnionTypeItemObject.valueOf(
                    UnionTypeMapperImpl.UNION_TYPE_APP_IMPL_IMAGE_PICKER_3_BUCKET,
                    new DataObject<>(bucket)
                            .putExtObjectObject1(this.imageData)
                            .putExtObjectObject2(UnionTypeImageData.this)));
        }
    }

    public void childClick() {
        dialog.mGridView.updateConfirmNextStatus();
    }
}
