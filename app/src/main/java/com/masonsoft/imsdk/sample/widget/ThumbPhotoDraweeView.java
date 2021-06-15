package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;

import me.relex.photodraweeview.PhotoDraweeView;

public class ThumbPhotoDraweeView extends PhotoDraweeView {

    public ThumbPhotoDraweeView(Context context, GenericDraweeHierarchy hierarchy) {
        super(context, hierarchy);
    }

    public ThumbPhotoDraweeView(Context context) {
        super(context);
    }

    public ThumbPhotoDraweeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThumbPhotoDraweeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setImageUrl(@Nullable ImageRequest thumb, @Nullable ImageRequest... firstAvailable) {
        setEnableDraweeMatrix(false);
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setOldController(getController())
                .setLowResImageRequest(thumb)
                .setFirstAvailableImageRequests(firstAvailable)
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        super.onFailure(id, throwable);
                        setEnableDraweeMatrix(false);
                    }

                    @Override
                    public void onFinalImageSet(String id, ImageInfo imageInfo,
                                                Animatable animatable) {
                        super.onFinalImageSet(id, imageInfo, animatable);
                        setEnableDraweeMatrix(true);
                        if (imageInfo != null) {
                            update(imageInfo.getWidth(), imageInfo.getHeight());
                        }
                    }

                    @Override
                    public void onIntermediateImageFailed(String id, Throwable throwable) {
                        super.onIntermediateImageFailed(id, throwable);
                        setEnableDraweeMatrix(false);
                    }

                    @Override
                    public void onIntermediateImageSet(String id, ImageInfo imageInfo) {
                        super.onIntermediateImageSet(id, imageInfo);
                        setEnableDraweeMatrix(true);
                        if (imageInfo != null) {
                            update(imageInfo.getWidth(), imageInfo.getHeight());
                        }
                    }
                })
                .build();
        setController(controller);
    }

}
