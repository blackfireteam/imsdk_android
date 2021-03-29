package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.annotation.SuppressLint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;

import com.idonans.uniontype.Host;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeViewHolderListeners;

public abstract class IMMessageTextViewHolder extends IMMessageViewHolder {

    private final TextView mMessageTime;
    private final TextView mMessageText;

    public IMMessageTextViewHolder(@NonNull Host host, int layout) {
        super(host, layout);
        mMessageTime = itemView.findViewById(R.id.message_time);
        mMessageText = itemView.findViewById(R.id.message_text);
    }

    public IMMessageTextViewHolder(@NonNull Host host, @NonNull View itemView) {
        super(host, itemView);
        mMessageTime = itemView.findViewById(R.id.message_time);
        mMessageText = itemView.findViewById(R.id.message_text);
    }

    @SuppressLint("ClickableViewAccessibility")
    @CallSuper
    @Override
    public void onBind(int position, @NonNull Object originObject) {
        //noinspection unchecked
        final DataObject<IMMessage> itemObject = (DataObject<IMMessage>) originObject;
        final IMMessage imMessage = itemObject.object;

        updateMessageTimeView(mMessageTime, itemObject);
        mMessageText.setText(imMessage.body.getOrDefault(null) + " id:" + imMessage.id.get() + ", seq:" + imMessage.seq.get() + ", toUserId:" + imMessage.toUserId.get() + ", fromUserId:" + imMessage.fromUserId.get());

        GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(mMessageText.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                UnionTypeViewHolderListeners.OnItemClickListener listener = itemObject.getExtHolderItemClick1();
                if (listener != null) {
                    listener.onItemClick(IMMessageTextViewHolder.this);
                }
                return true;
            }
        });
        mMessageText.setOnTouchListener((v, event) -> gestureDetectorCompat.onTouchEvent(event));

        mMessageText.setOnLongClickListener(v -> {
            UnionTypeViewHolderListeners.OnItemLongClickListener listener = itemObject.getExtHolderItemLongClick1();
            if (listener != null) {
                listener.onItemLongClick(this);
            }
            return true;
        });
    }

}
