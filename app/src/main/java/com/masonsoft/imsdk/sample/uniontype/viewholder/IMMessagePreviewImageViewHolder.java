package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.MSIMImageElement;
import com.masonsoft.imsdk.MSIMMessage;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeViewHolderListeners;
import com.masonsoft.imsdk.sample.widget.ThumbPhotoDraweeView;

import io.github.idonans.uniontype.Host;

public class IMMessagePreviewImageViewHolder extends IMMessageViewHolder {

    private final ThumbPhotoDraweeView mImage;

    public IMMessagePreviewImageViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_im_message_preview_image);
        mImage = itemView.findViewById(R.id.image);
    }

    @Override
    protected void onBindItemObject(int position, @NonNull DataObject<MSIMMessage> itemObject) {
        super.onBindItemObject(position, itemObject);

        final MSIMMessage message = itemObject.object;
        String url = null;
        final MSIMImageElement element = message.getImageElement();
        if (element != null) {
            url = element.getUrl();
        }
        mImage.setPhotoUri(url == null ? null : Uri.parse(url));

        mImage.setOnViewTapListener((view, x, y) -> {
            UnionTypeViewHolderListeners.OnItemClickListener listener = itemObject.getExtHolderItemClick1();
            if (listener != null) {
                listener.onItemClick(this);
            }
        });
    }

}
