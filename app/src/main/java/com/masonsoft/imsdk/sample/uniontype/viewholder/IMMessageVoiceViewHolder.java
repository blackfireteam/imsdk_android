package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeViewHolderListeners;
import com.masonsoft.imsdk.sample.widget.IMMessageVoiceView;

import io.github.idonans.uniontype.Host;

public abstract class IMMessageVoiceViewHolder extends IMMessageViewHolder {

    protected final IMMessageVoiceView mVoiceView;
    protected final ImageView mVoiceImageFlag;
    protected final TextView mVoiceDurationText;

    public IMMessageVoiceViewHolder(@NonNull Host host, int layout) {
        super(host, layout);
        mVoiceView = itemView.findViewById(R.id.voice_view);
        mVoiceImageFlag = itemView.findViewById(R.id.voice_image_flag);
        mVoiceDurationText = itemView.findViewById(R.id.voice_duration_text);
    }

    public IMMessageVoiceViewHolder(@NonNull Host host, @NonNull View itemView) {
        super(host, itemView);
        mVoiceView = itemView.findViewById(R.id.voice_view);
        mVoiceImageFlag = itemView.findViewById(R.id.voice_image_flag);
        mVoiceDurationText = itemView.findViewById(R.id.voice_duration_text);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onBindItemObject(int position, @NonNull DataObject<IMMessage> itemObject) {
        super.onBindItemObject(position, itemObject);
        final IMMessage message = itemObject.object;

        mVoiceView.setMessage(message);

        final long durationMs = message.durationMs.getOrDefault(0L);
        mVoiceDurationText.setText(durationMs / 1000 + " ''");

        mVoiceView.setOnLongClickListener(v -> {
            final UnionTypeViewHolderListeners.OnItemLongClickListener listener = itemObject.getExtHolderItemLongClick1();
            if (listener != null) {
                listener.onItemLongClick(this);
            }
            return true;
        });
    }

}
