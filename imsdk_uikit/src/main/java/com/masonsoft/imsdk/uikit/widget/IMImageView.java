package com.masonsoft.imsdk.uikit.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.masonsoft.imsdk.MSIMConstants;
import com.masonsoft.imsdk.MSIMImageElement;
import com.masonsoft.imsdk.MSIMMessage;
import com.masonsoft.imsdk.MSIMVideoElement;
import com.masonsoft.imsdk.uikit.IMUIKitConstants;
import com.masonsoft.imsdk.uikit.IMUIKitLog;
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.List;

import io.github.idonans.core.util.Preconditions;

public class IMImageView extends ImageLayout {

    private static final boolean DEBUG = IMUIKitConstants.DEBUG_WIDGET;

    public IMImageView(Context context) {
        this(context, null);
    }

    public IMImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public IMImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    }

    public void setChatMessage(MSIMMessage message) {
        final List<String> firstAvailableUrls = new ArrayList<>();

        if (message != null) {
            final int messageType = message.getMessageType();

            if (messageType == MSIMConstants.MessageType.IMAGE) {
                final MSIMImageElement element = message.getImageElement();
                Preconditions.checkNotNull(element);
                final String localPath = element.getPath();
                if (localPath != null) {
                    firstAvailableUrls.add(localPath);
                }
                final String url = element.getUrl();
                if (url != null) {
                    firstAvailableUrls.add(url);
                }
                if (DEBUG) {
                    IMUIKitLog.v(Objects.defaultObjectTag(this) + " image message localPath:%s, url:%s",
                            localPath, url);
                }
            } else if (messageType == MSIMConstants.MessageType.VIDEO) {
                final MSIMVideoElement element = message.getVideoElement();
                Preconditions.checkNotNull(element);
                final String localThumbPath = element.getThumbPath();
                if (localThumbPath != null) {
                    firstAvailableUrls.add(localThumbPath);
                }
                final String thumbUrl = element.getThumbUrl();
                if (thumbUrl != null) {
                    firstAvailableUrls.add(thumbUrl);
                }
                if (DEBUG) {
                    IMUIKitLog.v(Objects.defaultObjectTag(this) + " video message localThumbPath:%s, thumbUrl:%s", localThumbPath, thumbUrl);
                }
            } else {
                IMUIKitLog.e(Objects.defaultObjectTag(this) + " not support type %s", messageType);
            }
        }

        setImageUrl(null, firstAvailableUrls.toArray(new String[]{}));
    }

}