package com.example.vvgreenhouse.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 加密工具类 —— 用于密码加密比对
 */
public class MD5Util {

    /**
     * 对字符串进行MD5加密，返回32位小写十六进制字符串
     */
    public static String md5(String input) {
        if (input == null || input.isEmpty()) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 比对原始字符串与MD5值是否匹配
     */
    public static boolean verify(String raw, String md5Hex) {
        if (raw == null || md5Hex == null) return false;
        String computed = md5(raw);
        return md5Hex.equalsIgnoreCase(computed);
    }
}
