package com.lzf.flyingsocks.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 字符串、数字处理工具
 */
public final class BaseUtils {

    //主机名正则表达式
    private static final Pattern HOST_PATTERN = Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$");

    //IPV4地址正则表达式
    private static final Pattern IPV4_PATTERN = Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");

    //IPV6正则表达式
    private static final Pattern IPV6_PATTERN = Pattern.compile("^\\s*((([0-9A-Fa-f]{1,4}:){7}(([0-9A-Fa-f]{1,4})|:))|(([0-9A-Fa-f]{1,4}:){6}(:|((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})|(:[0-9A-Fa-f]{1,4})))|(([0-9A-Fa-f]{1,4}:){5}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4}){0,1}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}){0,2}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){0,3}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(([0-9A-Fa-f]{1,4}:)(:[0-9A-Fa-f]{1,4}){0,4}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(:(:[0-9A-Fa-f]{1,4}){0,5}((:((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})?)|((:[0-9A-Fa-f]{1,4}){1,2})))|(((25[0-5]|2[0-4]\\d|[01]?\\d{1,2})(\\.(25[0-5]|2[0-4]\\d|[01]?\\d{1,2})){3})))(%.+)?\\s*$");

    /**
     * @param ip 点标记法IPV4字符串
     * @return 判断该字符串是否符合
     */
    public static boolean isIPv4Address(String ip) {
        return IPV4_PATTERN.matcher(ip).matches();
    }

    /**
     * 判断是否是主机名
     *
     * @param host 主机名
     * @return 是否是主机名
     */
    public static boolean isHostName(String host) {
        return HOST_PATTERN.matcher(host).matches();
    }

    /**
     * 判断是否是IPV6地址字符串
     *
     * @param ip IP字符串
     * @return 是否是IPV6地址
     */
    public static boolean isIPv6Address(String ip) {
        return IPV6_PATTERN.matcher(ip).matches();
    }

    /**
     * 是否是IPV4或IPV6地址
     *
     * @param ip IP地址
     * @return 是否是IPV4或IPV6地址
     */
    public static boolean isIPAddress(String ip) {
        return isIPv4Address(ip) || isIPv6Address(ip);
    }

    /**
     * @param port 字符串
     * @return 判断一个字符串是否可能是端口号
     */
    public static boolean isPortString(String port) {
        try {
            int p = Integer.parseInt(port);
            return isPort(p);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param port 数字
     * @return 该数字是否符合端口范围
     */
    public static boolean isPort(int port) {
        return port > 0 && port < 65536;
    }

    /**
     * 将点标记法IPV4字符串转换为int
     *
     * @param ipv4 IPV4字符串
     * @return IPV4地址的int表示法
     */
    public static int parseIPv4StringToInteger(String ipv4) {
        String[] str = splitPreserveAllTokens(ipv4, '.');
        int num = Integer.parseInt(str[0]) << 24;
        num |= Integer.parseInt(str[1]) << 16;
        num |= Integer.parseInt(str[2]) << 8;
        return num | Integer.parseInt(str[3]);
    }

    /**
     * 将int类型的数字转换为点标记法的IP地址
     *
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

    /**
     * @param b IPv4地址
     * @return 点标记法IPv4地址字符串
     */
    public static String parseByteArrayToIPv4Address(byte[] b) {
        return (b[0] & 0x0FF) + "." + (b[1] & 0x0FF) + "." + (b[2] & 0x0FF) + "." + (b[3] & 0x0FF);
    }

    public static int parseByteArrayToIPv4Integer(byte[] b) {
        if (b.length != 4)
            throw new IllegalArgumentException("illegal ipv4 byte array");
        int num = (b[0] & 0x0FF) << 24;
        num |= (b[1] & 0x0FF) << 16;
        num |= (b[2] & 0x0FF) << 8;
        return (num | (b[3] & 0x0FF));
    }

    /**
     * 判断一个IPV4地址是否在目标网络号中
     *
     * @param ip      需要判断的IPv4地址
     * @param network 网络号
     * @param mask    网络号子网掩码
     * @return 判定结果
     */
    public static boolean isIPv4AddressInRange(int ip, int network, int mask) {
        return (ip & mask) == (network & mask);
    }


    /**
     * 将无符号short转换为Int数字
     *
     * @param val 无符号short
     * @return int数字
     */
    public static int parseUnsignedShortToInteger(short val) {
        return val & 0x0FFFF;
    }

    public static int parseByteToInteger(byte val) {
        return val & 0x0FF;
    }

    public static String reverseString(String str) {
        return new StringBuilder(str).reverse().toString();
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
