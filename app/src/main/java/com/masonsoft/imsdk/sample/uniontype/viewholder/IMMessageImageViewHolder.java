package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.idonans.lang.util.ViewUtil;
import com.idonans.uniontype.Host;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeViewHolderListeners;
import com.masonsoft.imsdk.sample.widget.IMImageView;
import com.masonsoft.imsdk.sample.widget.ResizeImageView;

public abstract class IMMessageImageViewHolder extends IMMessageViewHolder {

    protected final TextView mMessageTime;
    protected final ResizeImageView mResizeImageView;
    protected final IMImageView mImage;

    public IMMessageImageViewHolder(@NonNull Host host, int layout) {
        super(host, layout);
        mMessageTime = itemView.findViewById(R.id.message_time);
        mResizeImageView = itemView.findViewById(R.id.resize_image_view);
        mImage = itemView.findViewById(R.id.image);
    }

    public IMMessageImageViewHolder(@NonNull Host host, @NonNull View itemView) {
        super(host, itemView);
        mMessageTime = itemView.findViewById(R.id.message_time);
        mResizeImageView = itemView.findViewById(R.id.resize_image_view);
        mImage = itemView.findViewById(R.id.image);
    }

    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<IMMessage> itemObject = (DataObject<IMMessage>) originObject;
        final IMMessage imMessage = itemObject.object;

        updateMessageTimeView(mMessageTime, itemObject);
        mResizeImageView.setImageSize(imMessage.width.getOrDefault(0L), imMessage.height.getOrDefault(0L));
        mImage.setChatMessage(imMessage);

        mResizeImageView.setOnLongClickListener(v -> {
            UnionTypeViewHolderListeners.OnItemLongClickListener listener = itemObject.getExtHolderItemLongClick1();
            if (listener != null) {
                listener.onItemLongClick(this);
            }
            return true;
        });
        ViewUtil.onClick(mResizeImageView, v -> {
            UnionTypeViewHolderListeners.OnItemClickListener listener = itemObject.getExtHolderItemClick1();
            if (listener != null) {
                listener.onItemClick(this);
            }
        });
    }

}
