package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.masonsoft.imsdk.core.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.sample.Constants;
import com.masonsoft.imsdk.sample.SampleLog;
import com.masonsoft.imsdk.util.Objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.idonans.core.util.DimenUtil;

public class IMImageView extends ImageLayout {

    private static final boolean DEBUG = Constants.DEBUG_WIDGET;

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

    private String mLocationThumbUrl;

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final int locationImageSize = DimenUtil.dp2px(200);
        mLocationThumbUrl = "http://restapi.amap.com/v3/staticmap?location=%s,%s&zoom=%s&size=" + locationImageSize + "*" + locationImageSize + "&markers=mid,0xFF0000,0:%s,%s&key=7d496af79e5fabd7616131817f337541";
    }

    public void setChatMessage(IMMessage message) {
        String thumbUrl = null;
        final List<String> firstAvailableUrls = new ArrayList<>();

        if (message != null && !message.type.isUnset()) {
            final int type = message.type.get();

            if (type == IMConstants.MessageType.IMAGE) {
                final String localBodyOrigin = message.localBodyOrigin.getOrDefault(null);
                if (localBodyOrigin != null) {
                    firstAvailableUrls.add(localBodyOrigin);
                }
                final String body = message.body.getOrDefault(null);
                if (body != null) {
                    firstAvailableUrls.add(body);
                }
                if (DEBUG) {
                    SampleLog.v(Objects.defaultObjectTag(this) + " image message localBodyOrigin:%s, body:%s",
                            localBodyOrigin, body);
                }
            } else if (type == IMConstants.MessageType.VIDEO) {
                final String localThumbOrigin = message.localThumbOrigin.getOrDefault(null);
                if (localThumbOrigin != null) {
                    firstAvailableUrls.add(localThumbOrigin);
                }
                final String thumb = message.thumb.getOrDefault(null);
                if (thumb != null) {
                    firstAvailableUrls.add(thumb);
                }
                if (DEBUG) {
                    SampleLog.v(Objects.defaultObjectTag(this) + " video message localThumbOrigin:%s, thumb:%s", localThumbOrigin, thumb);
                }
            } else if (type == IMConstants.MessageType.LOCATION) {
                String url = String.format(Locale.CHINA, mLocationThumbUrl,
                        message.lng.getOrDefault(0d),
                        message.lat.getOrDefault(0d),
                        message.zoom.getOrDefault(0L),
                        message.lng.getOrDefault(0d),
                        message.lat.getOrDefault(0d));
                if (DEBUG) {
                    SampleLog.v(Objects.defaultObjectTag(this) + " location thumb url %s", url);
                }
                firstAvailableUrls.add(url);
            } else {
                SampleLog.e(Objects.defaultObjectTag(this) + " not support type %s", type);
            }
        }

        //noinspection ConstantConditions
        this.setFirstAvailableUrls(thumbUrl, firstAvailableUrls.toArray(new String[]{}));
    }

}