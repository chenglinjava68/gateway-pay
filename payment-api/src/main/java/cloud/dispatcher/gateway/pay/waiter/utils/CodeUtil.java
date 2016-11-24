package cloud.dispatcher.gateway.pay.waiter.utils;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.digest.DigestUtils;

public class CodeUtil {

    public static String md5Hex(String value) {
        try {
            return DigestUtils.md5Hex(value.getBytes("gbk"));
        } catch (UnsupportedEncodingException error) {
            throw new RuntimeException(error);
        }
    }

    public static String shaHex(String value) {
        try {
            return DigestUtils.shaHex(value.getBytes("gbk"));
        } catch (UnsupportedEncodingException error) {
            throw new RuntimeException(error);
        }
    }
}
