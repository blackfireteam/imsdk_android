package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.annotation.SuppressLint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeViewHolderListeners;

import io.github.idonans.uniontype.Host;

public abstract class IMMessageWinkViewHolder extends IMMessageViewHolder {

    private final TextView mMessageText;

    public IMMessageWinkViewHolder(@NonNull Host host, int layout) {
        super(host, layout);
        mMessageText = itemView.findViewById(R.id.message_text);
    }

    public IMMessageWinkViewHolder(@NonNull Host host, @NonNull View itemView) {
        super(host, itemView);
        mMessageText = itemView.findViewById(R.id.message_text);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onBindItemObject(int position, @NonNull DataObject<IMMessage> itemObject) {
        super.onBindItemObject(position, itemObject);

        GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(mMessageText.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                final UnionTypeViewHolderListeners.OnItemClickListener listener = itemObject.getExtHolderItemClick1();
                if (listener != null) {
                    listener.onItemClick(IMMessageWinkViewHolder.this);
                }
                return true;
            }
        });
        mMessageText.setOnTouchListener((v, event) -> gestureDetectorCompat.onTouchEvent(event));

        mMessageText.setOnLongClickListener(v -> {
            final UnionTypeViewHolderListeners.OnItemLongClickListener listener = itemObject.getExtHolderItemLongClick1();
            if (listener != null) {
                listener.onItemLongClick(this);
            }
            return true;
        });
    }

}