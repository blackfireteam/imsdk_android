package com.masonsoft.imsdk.sample.uniontype.viewholder;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.common.microlifecycle.MicroLifecycleComponentManager;
import com.masonsoft.imsdk.sample.common.microlifecycle.MicroLifecycleComponentManagerHost;
import com.masonsoft.imsdk.sample.common.microlifecycle.RecyclerViewMicroLifecycleComponentManager;
import com.masonsoft.imsdk.sample.common.microlifecycle.real.Real;
import com.masonsoft.imsdk.sample.common.microlifecycle.real.RealHost;
import com.masonsoft.imsdk.sample.uniontype.DataObject;
import com.masonsoft.imsdk.sample.uniontype.UnionTypeViewHolderListeners;
import com.masonsoft.imsdk.sample.widget.IMMessageVoiceView;

import io.github.idonans.uniontype.Host;
import io.github.idonans.uniontype.UnionTypeAdapter;

public abstract class IMMessageVoiceViewHolder extends IMMessageViewHolder {

    protected final IMMessageVoiceView mVoiceView;
    protected final ImageView mVoiceImageFlag;
    protected final TextView mVoiceDurationText;

    @Nullable
    private LocalMicroLifecycle mLocalMicroLifecycle;

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

        createLocalMicroLifecycle();
    }

    private void createLocalMicroLifecycle() {
        if (mLocalMicroLifecycle == null) {
            UnionTypeAdapter adapter = host.getAdapter();
            if (adapter instanceof MicroLifecycleComponentManagerHost) {
                MicroLifecycleComponentManager microLifecycleComponentManager = ((MicroLifecycleComponentManagerHost) adapter).getMicroLifecycleComponentManager();
                if (microLifecycleComponentManager != null) {
                    mLocalMicroLifecycle = createLocalMicroLifecycle(microLifecycleComponentManager);
                }
            }
        }
    }

    @NonNull
    private LocalMicroLifecycle createLocalMicroLifecycle(@NonNull MicroLifecycleComponentManager microLifecycleComponentManager) {
        return new LocalMicroLifecycle(microLifecycleComponentManager);
    }

    private class LocalMicroLifecycle extends RecyclerViewMicroLifecycleComponentManager.ViewHolderMicroLifecycleComponent implements RealHost {

        public LocalMicroLifecycle(@NonNull MicroLifecycleComponentManager microLifecycleComponentManager) {
            super(microLifecycleComponentManager);
        }

        @Nullable
        @Override
        public RecyclerView.ViewHolder getViewHolder() {
            return IMMessageVoiceViewHolder.this;
        }

        @Override
        public Real getReal() {
            return mVoiceView;
        }

        @Override
        public void onCreate() {
            super.onCreate();
            if (mVoiceView != null) {
                mVoiceView.performCreate();
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            if (mVoiceView != null) {
                mVoiceView.performStart();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (mVoiceView != null) {
                mVoiceView.performResume();
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mVoiceView != null) {
                mVoiceView.performPause();
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            if (mVoiceView != null) {
                mVoiceView.performStop();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mLocalMicroLifecycle = null;
            if (mVoiceView != null) {
                mVoiceView.performDestroy();
            }
        }
    }

}
