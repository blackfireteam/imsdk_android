package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeViewHolderListeners;

import io.github.idonans.uniontype.Host;
import me.relex.photodraweeview.PhotoDraweeView;

public class IMMessagePreviewImageViewHolder extends IMMessageViewHolder {

    private final PhotoDraweeView mImage;

    public IMMessagePreviewImageViewHolder(@NonNull Host host) {
        super(host, R.layout.imsdk_sample_union_type_impl_im_message_preview_image);
        mImage = itemView.findViewById(R.id.image);
    }

    @Override
    protected void onBindItemObject(int position, @NonNull DataObject<IMMessage> itemObject) {
        super.onBindItemObject(position, itemObject);

        final IMMessage imMessage = itemObject.object;
        final String url = imMessage.body.getOrDefault(null);
        mImage.setPhotoUri(url == null ? null : Uri.parse(url));

        mImage.setOnViewTapListener((view, x, y) -> {
            UnionTypeViewHolderListeners.OnItemClickListener listener = itemObject.getExtHolderItemClick1();
            if (listener != null) {
                listener.onItemClick(this);
            }
        });
    }

}
