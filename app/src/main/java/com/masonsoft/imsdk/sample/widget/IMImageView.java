package com.masonsoft.imsdk.sample.widget;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

import com.idonans.core.util.DimenUtil;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.core.IMConstants;
import com.masonsoft.imsdk.sample.SampleLog;

import java.util.Locale;

public class IMImageView extends ImageLayout {

    private static final boolean DEBUG = false;

    public IMImageView(Context context) {
        this(context, null);
    }

    public IMImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IMImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    public IMImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr, 0);
    }

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    }

    private final int mLocationImageSize = DimenUtil.dp2px(200);
    private final String mLocationThumbUrl = "http://restapi.amap.com/v3/staticmap?location=%s,%s&zoom=%s&size=" + mLocationImageSize + "*" + mLocationImageSize + "&markers=mid,0xFF0000,0:%s,%s&key=7d496af79e5fabd7616131817f337541";

    public void setChatMessage(IMMessage imMessage) {
        Uri uri = null;


        if (imMessage != null && !imMessage.type.isUnset()) {
            final int type = imMessage.type.get();

            if (type == IMConstants.MessageType.IMAGE) {
                final String body = imMessage.body.getOrDefault(null);
                if (body != null) {
                    uri = Uri.parse(body);
                }
                if (DEBUG) {
                    SampleLog.v("image message body uri %s", uri);
                }
            } else if (type == IMConstants.MessageType.VIDEO) {
                final String thumb = imMessage.thumb.getOrDefault(null);
                if (thumb != null) {
                    uri = Uri.parse(thumb);
                }
                if (DEBUG) {
                    SampleLog.v("video message thumb uri %s", uri);
                }
            } else if (type == IMConstants.MessageType.LOCATION) {
                String url = String.format(Locale.CHINA, mLocationThumbUrl,
                        imMessage.lng.getOrDefault(0d),
                        imMessage.lat.getOrDefault(0d),
                        imMessage.zoom.getOrDefault(0L),
                        imMessage.lng.getOrDefault(0d),
                        imMessage.lat.getOrDefault(0d));
                if (DEBUG) {
                    SampleLog.v("location thumb url %s", url);
                }
                uri = Uri.parse(url);
            } else {
                SampleLog.e("not support type %s", type);
            }
        }

        setUrl(uri == null ? null : uri.toString());
    }

}