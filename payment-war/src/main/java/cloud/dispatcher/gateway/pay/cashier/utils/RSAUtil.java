package cloud.dispatcher.gateway.pay.cashier.utils;

import javax.crypto.Cipher;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtil {

    public static PrivateKey getPrivateKey(String key) {
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(
                    CodeUtil.base64Decode(key)));
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    public static boolean verify(String value, String sign, String key, String charset) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = CodeUtil.base64Decode(key);
            PublicKey publicKey = keyFactory.generatePublic(
                    new X509EncodedKeySpec(encodedKey));

            Signature signature = Signature.getInstance("SHA1WithRSA");

            signature.initVerify(publicKey);
            signature.update(value.getBytes(charset));

            return signature.verify(CodeUtil.base64Decode(sign));
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    public static String encrypt(String value, String key, String charset) {
        try {
            PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(
                    CodeUtil.base64Decode(key));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(priPKCS8);

            Signature signature = Signature.getInstance("SHA1WithRSA");

            signature.initSign(privateKey);
            signature.update(value.getBytes(charset));

            return CodeUtil.base64Encode(signature.sign());
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    public static String decrypt(String value, String key, String charset) {
        try {
            PrivateKey privateKey = getPrivateKey(key);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            InputStream stream = new ByteArrayInputStream(CodeUtil.base64Decode(value));
            ByteArrayOutputStream writer = new ByteArrayOutputStream();
            byte[] buffer = new byte[128];
            int size;

            while ((size = stream.read(buffer)) != -1) {
                byte[] block = null;
                if (buffer.length == size) {
                    block = buffer;
                } else {
                    block = new byte[size];
                    for (int i = 0; i < size; i++) {
                        block[i] = buffer[i];
                    }
                }
                writer.write(cipher.doFinal(block));
            }
            return new String(writer.toByteArray(), charset);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }
}
