package com.masonsoft.imsdk.core;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.masonsoft.imsdk.EnqueueCallback;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMMessageFactory;
import com.masonsoft.imsdk.IMSessionMessage;
import com.masonsoft.imsdk.core.session.Session;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.idonans.core.thread.Threads;

@RunWith(AndroidJUnit4.class)
public class IMMessageQueueManagerTest {

    @Test
    public void testEnqueueSendMessage() {
        // 测试发送一条新消息

        IMLog.setLogLevel(Log.VERBOSE);
        final Session session = new Session(
                "123123123123123",
                "112.112.112.112",
                1908
        );
        IMSessionManager.getInstance().setSession(session);
        IMSessionManager.getInstance().setSessionUserId(session, 1);

        final IMMessage message = IMMessageFactory.createTextMessage("hello, text message");
        IMMessageQueueManager.getInstance().enqueueSendSessionMessage(
                message,
                2,
                new EnqueueCallback() {
                    @Override
                    public void onEnqueueSuccess(@NonNull IMSessionMessage imSessionMessage) {
                        System.out.println("onEnqueueSuccess " + imSessionMessage);
                    }

                    @Override
                    public void onEnqueueFail(@NonNull IMSessionMessage imSessionMessage, int errorCode, String errorMessage) {
                        System.out.println("onEnqueueFail " + imSessionMessage);
                        System.out.println("onEnqueueFail errorCode:" + errorCode + ", errorMessage:" + errorMessage);
                    }
                }
        );

        Threads.sleepQuietly(10 * 1000);
    }

}