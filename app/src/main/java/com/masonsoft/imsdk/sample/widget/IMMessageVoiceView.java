package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.masonsoft.imsdk.core.IMMessage;
import com.masonsoft.imsdk.sample.R;
import com.masonsoft.imsdk.sample.common.media.player.MediaPlayer;
import com.masonsoft.imsdk.sample.common.media.player.MediaPlayerDelegate;
import com.masonsoft.imsdk.sample.common.microlifecycle.real.Real;

import io.github.idonans.lang.util.ViewUtil;

public class IMMessageVoiceView extends MicroLifecycleFrameLayout implements Real {

    public IMMessageVoiceView(Context context) {
        this(context, null);
    }

    public IMMessageVoiceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMMessageVoiceView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IMMessageVoiceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    @Nullable
    private IMMessage mMessage;

    private VoicePlayerView mVoicePlayerView;

    @Nullable
    private MediaPlayerDelegate mMediaPlayerDelegate;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setContentView(R.layout.imsdk_sample_widget_im_message_voice_view);
        mVoicePlayerView = findViewById(R.id.voice_player_view);

        ViewUtil.onClick(mVoicePlayerView, v -> {
            togglePlayer();
            performClick();
        });
        mVoicePlayerView.setOnLongClickListener(v -> {
            performLongClick();
            return true;
        });
        mMediaPlayerDelegate = new MediaPlayerDelegate();
        setDefaultManual(true);
    }

    public void setOnPlayerStateUpdateListener(@Nullable VoicePlayerView.OnPlayerStateUpdateListener listener) {
        mVoicePlayerView.setOnPlayerStateUpdateListener(listener);
    }

    @Override
    public void forcePause() {
        if (isResumed()) {
            toggleWithManual();
        }
    }

    private void togglePlayer() {
        if (isResumed()
                && mMediaPlayerDelegate != null) {
            MediaPlayer mediaPlayer = mMediaPlayerDelegate.getMediaPlayer();
            if (mediaPlayer != null) {
                SimpleExoPlayer player = mediaPlayer.getPlayer();
                if (player != null) {
                    if (!canPausePlayer(player)) {
                        // 正常播放结束或者播放失败时点击重新开始播放
                        mMediaPlayerDelegate.initPlayerIfNeed(mVoicePlayerView, getVoiceUrl(), true, isResumed(), false);
                        return;
                    }
                }
            }
        }

        toggleWithManual();
    }

    private boolean canPausePlayer(SimpleExoPlayer player) {
        return player.getPlaybackState() != Player.STATE_ENDED
                && player.getPlaybackState() != Player.STATE_IDLE
                && player.getPlayWhenReady();
    }

    public void setMessage(@Nullable IMMessage message) {
        mMessage = message;

        mVoicePlayerView.setDurationMs(message == null ? 0L : message.durationMs.getOrDefault(0L));

        if (mMediaPlayerDelegate != null) {
            mMediaPlayerDelegate.initPlayerIfNeed(mVoicePlayerView, getVoiceUrl(), true, isResumed(), false);
        }
    }

    @Nullable
    private String getVoiceUrl() {
        if (mMessage != null) {
            return mMessage.body.getOrDefault(null);
        }
        return null;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mMediaPlayerDelegate != null) {
            mMediaPlayerDelegate.initPlayerIfNeed(mVoicePlayerView, getVoiceUrl(), true, isResumed(), false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mMediaPlayerDelegate != null) {
            mMediaPlayerDelegate.pausePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mMediaPlayerDelegate != null) {
            mMediaPlayerDelegate.releasePlayer();
        }
    }

}
