package com.masonsoft.imsdk.sample.common.media.audio;

import android.media.MediaRecorder;

import androidx.annotation.NonNull;
import androidx.core.math.MathUtils;

import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;

import java.io.File;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.manager.TmpFileManager;
import io.github.idonans.core.thread.Threads;
import io.github.idonans.core.util.Preconditions;

/**
 * 音频的录音与播放
 */
public class AudioManager {

    private static final Singleton<AudioManager> INSTANCE = new Singleton<AudioManager>() {
        @Override
        protected AudioManager create() {
            return new AudioManager();
        }
    };

    private static final long AUDIO_RECORD_MIN_DURATION = Constants.AUDIO_RECORD_MIN_DURATION;
    private static final long AUDIO_RECORD_MAX_DURATION = Constants.AUDIO_RECORD_MAX_DURATION;

    public static AudioManager getInstance() {
        return INSTANCE.get();
    }

    private final Object mAudioRecorderLock = new Object();
    // 音频录音
    private MediaRecorder mAudioRecorder;
    private String mAudioRecorderFile;
    private long mAudioRecordStartTimeMs;

    private AudioManager() {
    }

    public void startAudioRecord() {
        synchronized (mAudioRecorderLock) {
            startAudioRecordInternal();
            if (mAudioRecorder != null) {
                mAudioRecordStartTimeMs = System.currentTimeMillis();
                final long audioRecorderId = System.identityHashCode(mAudioRecorder);
                Threads.postUi(() -> {
                    synchronized (mAudioRecorderLock) {
                        final boolean abort = System.identityHashCode(mAudioRecorder) != audioRecorderId;
                        if (abort) {
                            return;
                        }
                        // 录音到了最大值
                        stopAudioRecord(false, true);
                    }
                }, AUDIO_RECORD_MAX_DURATION);
                notifyAudioRecordStart();
                Threads.postUi(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mAudioRecorderLock) {
                            final boolean abort = System.identityHashCode(mAudioRecorder) != audioRecorderId;
                            if (abort) {
                                return;
                            }
                            if (mAudioRecorderFile != null) {
                                final long timeNowMs = System.currentTimeMillis();
                                AudioManager.this.notifyAudioRecordProgress(timeNowMs - mAudioRecordStartTimeMs);
                                Threads.postUi(this, MathUtils.clamp(300 - (System.currentTimeMillis() - timeNowMs), 100, 300));
                            }
                        }
                    }
                }, 300L);
            } else {
                notifyAudioRecordError();
            }
        }
    }

    public void cancelAudioRecord() {
        stopAudioRecord(true, false);
    }

    public void stopAudioRecord() {
        stopAudioRecord(false, false);
    }

    private void stopAudioRecord(boolean cancelByManual, boolean reachMaxDuration) {
        synchronized (mAudioRecorderLock) {
            final String audioFile = mAudioRecorderFile;
            final long audioRecordStartTimeMs = mAudioRecordStartTimeMs;
            stopAudioRecordInternal();
            if (audioFile != null && audioRecordStartTimeMs > 0 && mAudioRecorderFile == null) {
                final long duration = System.currentTimeMillis() - audioRecordStartTimeMs;
                if (cancelByManual) {
                    notifyAudioRecordCancel(false);
                } else if (duration < AUDIO_RECORD_MIN_DURATION) {
                    notifyAudioRecordCancel(true);
                } else {
                    notifyAudioRecordCompletedSuccess(audioFile, reachMaxDuration);
                }
            }
        }
    }

    /**
     * 开始录音
     */
    private void startAudioRecordInternal() {
        synchronized (mAudioRecorderLock) {
            try {
                stopAudioRecordInternal();

                final File audioRecorderFile = TmpFileManager.getInstance().createNewTmpFileQuietly("imsdk_sample_audio_record", ".m4a");
                Preconditions.checkNotNull(audioRecorderFile);
                mAudioRecorderFile = audioRecorderFile.getAbsolutePath();
                mAudioRecorder = new MediaRecorder();
                mAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                // 使用 mp4 容器并且后缀改为 .m4a
                mAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mAudioRecorder.setOutputFile(mAudioRecorderFile);
                mAudioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mAudioRecorder.setOnErrorListener((mr, what, extra) -> {
                    SampleLog.e("AudioRecorder OnErrorListener what:%s", what);
                });
                mAudioRecorder.prepare();
                mAudioRecorder.start();
            } catch (Throwable e) {
                SampleLog.e(e);
                stopAudioRecordInternal();
            }
        }
    }

    /**
     * 停止录音
     */
    private void stopAudioRecordInternal() {
        synchronized (mAudioRecorderLock) {
            try {
                if (mAudioRecorder != null) {
                    mAudioRecorder.release();
                }
            } catch (Throwable e) {
                SampleLog.e(e);
            } finally {
                mAudioRecorder = null;
                mAudioRecorderFile = null;
                mAudioRecordStartTimeMs = -1L;
            }
        }
    }

    // 通知录音开始
    private void notifyAudioRecordStart() {
    }

    // 通知录音进度
    private void notifyAudioRecordProgress(long duration) {
    }

    // 通知录音出错
    private void notifyAudioRecordError() {
    }

    // 通知录音取消
    private void notifyAudioRecordCancel(boolean lessThanMinDuration/*是否是因为不足最短录音时长而取消*/) {
    }

    // 通知录音正常结束
    private void notifyAudioRecordCompletedSuccess(@NonNull final String audioRecorderFile, boolean reachMaxDuration/*是否是因为到达了最大时长而结束*/) {
    }

}
