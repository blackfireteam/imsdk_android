package com.masonsoft.imsdk.sample;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.masonsoft.imsdk.core.IMSessionManager;
import com.masonsoft.imsdk.core.session.Session;
import com.masonsoft.imsdk.util.Objects;

import io.github.idonans.core.Singleton;
import io.github.idonans.core.manager.StorageManager;
import io.github.idonans.core.thread.Threads;

public class LocalSettingsManager {

    private static final Singleton<LocalSettingsManager> INSTANCE = new Singleton<LocalSettingsManager>() {
        @Override
        protected LocalSettingsManager create() {
            return new LocalSettingsManager();
        }
    };

    public static LocalSettingsManager getInstance() {
        return INSTANCE.get();
    }

    private static final String KEY_SETTINGS = "key:settings_20210420";

    @NonNull
    private Settings mSettings = new Settings();

    private LocalSettingsManager() {
        restore();
    }

    public void attach() {
        SampleLog.v(Objects.defaultObjectTag(this) + " attach");

        IMSessionManager.getInstance().setSession(mSettings.createSession());
    }

    private void restore() {
        try {
            final String json = StorageManager.getInstance().get(Constants.SAMPLE_STORAGE_NAMESPACE, KEY_SETTINGS);
            if (!TextUtils.isEmpty(json)) {
                final Settings settings = new Gson().fromJson(json, new TypeToken<Settings>() {
                }.getType());
                if (settings != null) {
                    mSettings = settings;
                }
            }
        } catch (Throwable e) {
            SampleLog.e(e);
        }
    }

    @NonNull
    public Settings getSettings() {
        return mSettings.copy();
    }

    public void save() {
        Threads.postBackground(() -> {
            try {
                final Settings settings = mSettings.copy();
                final String json = new Gson().toJson(settings);
                StorageManager.getInstance().set(Constants.SAMPLE_STORAGE_NAMESPACE, KEY_SETTINGS, json);
            } catch (Throwable e) {
                SampleLog.e(e);
            }
        });
    }

    public static class Settings {

        public String imHost;
        public int imPort;
        public String imToken;

        public boolean hasValidSession() {
            return !TextUtils.isEmpty(this.imHost)
                    && this.imPort > 0
                    && !TextUtils.isEmpty(this.imToken);
        }

        @Nullable
        public Session createSession() {
            if (hasValidSession()) {
                return new Session(this.imToken, this.imHost, this.imPort);
            }
            return null;
        }

        private Settings copy() {
            final Settings target = new Settings();
            target.imHost = this.imHost;
            target.imPort = this.imPort;
            target.imToken = this.imToken;
            return target;
        }
    }

}
