package com.lzf.flyingsocks.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 字符串、数字处理工具
 */
public final class BaseUtils {

    /**
     * 将点标记法IPV4字符串转换为int
     * @param ipv4 IPV4字符串
     * @return IPV4地址的int表示法
     */
    public static int parseIPv4StringToInteger(String ipv4) {
        String[] str = splitPreserveAllTokens(ipv4, '.');
        int num = Integer.valueOf(str[0]) << 24;
        num |= Integer.parseInt(str[1]) << 16;
        num |= Integer.parseInt(str[2]) << 8;
        num |= Integer.parseInt(str[3]);
        return num;
    }

    /**
     * 将int类型的数字转换为点标记法的IP地址
     * @param addr IP地址的4字节数字形式
     * @return 点标记法的IP地址
     */
    public static String parseIntToIPv4Address(int addr) {
        int d0 = addr >>> 24;
        int d1 = (addr & 0x00FFFFFF) >>> 16;
        int d2 = (addr & 0x0000FFFF) >>> 8;
        int d3 = addr & 0x000000FF;
        return d0 + "." + d1 + "." + d2 + "." + d3;
    }

    public static String parseByteArrayToIPv4Address(byte[] b) {
        return (b[0] & 0x0FF) + "." + (b[1] & 0x0FF) + "." + (b[2] & 0x0FF) + "." + (b[3] & 0x0FF);
    }

    /**
     * 将无符号short转换为Int数字
     * @param val 无符号short
     * @return int数字
     */
    public static int parseUnsignedShortToInteger(short val) {
        return val & 0x0FFFF;
    }

    public static int parseByteToInteger(byte val) {
        return val & 0x0FF;
    }

    public static String[] splitPreserveAllTokens(String str, char separatorChar) {
        return splitWorker(str, separatorChar, true);
    }

    public static String[] splitPreserveAllTokens(String str, String separatorChars) {
        return splitWorker(str, separatorChars, -1, true);
    }

    @SuppressWarnings("unchecked")
    private static String[] splitWorker(String str, char separatorChar, boolean preserveAllTokens) {

        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return new String[0];
        }
        List list = new ArrayList();
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        while (i < len) {
            if (str.charAt(i) == separatorChar) {
                if (match || preserveAllTokens) {
                    list.add(str.substring(start, i));
                    match = false;
                    lastMatch = true;
                }
                start = ++i;
                continue;
            }
            lastMatch = false;
            match = true;
            i++;
        }
        if (match || (preserveAllTokens && lastMatch)) {
            list.add(str.substring(start, i));
        }
        return (String[]) list.toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    private static String[] splitWorker(String str, String separatorChars, int max, boolean preserveAllTokens) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return new String[0];
        }
        List list = new ArrayList();
        int sizePlus1 = 1;
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        if (separatorChars == null) {
            while (i < len) {
                if (Character.isWhitespace(str.charAt(i))) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        } else if (separatorChars.length() == 1) {
            char sep = separatorChars.charAt(0);
            while (i < len) {
                if (str.charAt(i) == sep) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        } else {
            while (i < len) {
                if (separatorChars.indexOf(str.charAt(i)) >= 0) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        }
        if (match || (preserveAllTokens && lastMatch)) {
            list.add(str.substring(start, i));
        }
        return (String[]) list.toArray(new String[0]);
    }

    private BaseUtils() {
        throw new UnsupportedOperationException();
    }
}
