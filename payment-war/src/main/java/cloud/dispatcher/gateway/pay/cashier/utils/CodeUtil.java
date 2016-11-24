package cloud.dispatcher.gateway.pay.cashier.utils;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

public class CodeUtil {

    public static String shaHex(String value, String charset) {
        try {
            return DigestUtils.shaHex(value.getBytes(charset));
        } catch (UnsupportedEncodingException error) {
            throw new RuntimeException(error);
        }
    }

    public static String md5Hex(String value, String charset) {
        try {
            return DigestUtils.md5Hex(value.getBytes(charset));
        } catch (UnsupportedEncodingException error) {
            throw new RuntimeException(error);
        }
    }

    public static byte[] base64Decode(String value) {
        return Base64.decodeBase64(value);
    }

    public static String base64Encode(byte[] bytes) {
        return new String(Base64.encodeBase64(bytes));
    }


}
