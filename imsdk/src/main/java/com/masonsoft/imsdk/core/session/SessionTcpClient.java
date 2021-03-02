package com.masonsoft.imsdk.core.session;

import androidx.annotation.NonNull;

import com.idonans.core.Charsets;
import com.masonsoft.imsdk.core.NettyTcpClient;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SessionTcpClient extends NettyTcpClient {

    private static final String AES_KEY_DEFAULT = "10231234545613465926778834590126";
    private static final String AES_IV = "3101238945674526";

    @NonNull
    private final Session mSession;

    public SessionTcpClient(@NonNull Session session) {
        super(session.getTcpHost(), session.getTcpPort());
        mSession = session;
    }

    @Override
    protected byte[] encryptMessage(int messageType, byte[] messageData) {
        // 请求登录的消息强制加密
        // TODO

        return super.encryptMessage(messageType, messageData);
    }

    @NonNull
    @Override
    protected byte[] decryptMessage(@NonNull byte[] messageData) {
        // 解密消息内容
        final String aesKey = mSession.getAesKey();
        return crypt(messageData, aesKey, false);
    }

    /**
     * 按照约定参数 AES 加密/解密
     */
    @NonNull
    private static byte[] crypt(@NonNull byte[] input, @NonNull String key, boolean encrypt/*是否为加密模式*/) {
        try {
            if (key == null) {
                // 如果没有自定义 key, 则使用默认 key
                key = AES_KEY_DEFAULT;
            }

            final AlgorithmParameterSpec ivSpec = new IvParameterSpec(AES_IV.getBytes(Charsets.UTF8));
            final SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(Charsets.UTF8), "AES");
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", new BouncyCastleProvider());
            cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(input);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

}
