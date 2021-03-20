package com.masonsoft.imsdk.core;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.idonans.core.thread.Threads;
import com.masonsoft.imsdk.IMMessage;
import com.masonsoft.imsdk.IMMessageFactory;
import com.masonsoft.imsdk.IMSessionMessage;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IMMessageQueueManagerTest {

    @Test
    public void testEnqueueSendMessage() {
        // 测试发送一条新消息

        final IMMessage message = IMMessageFactory.createTextMessage("hello, text message");
        IMMessageQueueManager.getInstance().enqueueSendMessage(
                message,
                1,
                new IMSessionMessage.EnqueueCallback() {
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