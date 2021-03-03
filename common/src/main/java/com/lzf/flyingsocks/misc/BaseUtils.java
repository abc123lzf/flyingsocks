/*
 * Copyright (c) 2019 abc123lzf <abc123lzf@126.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lzf.flyingsocks.misc;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;

/**
 * 字符串、数字处理工具
 */
public final class BaseUtils {

    /**
     * @param ip 点标记法IPV4字符串
     * @return 判断该字符串是否符合
     */
    public static boolean isIPv4Address(String ip) {
        return InetAddressValidator.getInstance().isValidInet4Address(ip);
    }

    /**
     * 判断是否是主机名
     *
     * @param host 主机名
     * @return 是否是主机名
     */
    public static boolean isHostName(String host) {
        return DomainValidator.getInstance(true).isValid(host);
    }

    /**
     * 判断是否是IPV6地址字符串
     *
     * @param ip IP字符串
     * @return 是否是IPV6地址
     */
    public static boolean isIPv6Address(String ip) {
        return InetAddressValidator.getInstance().isValidInet6Address(ip);
    }

    /**
     * 是否是IPV4或IPV6地址
     *
     * @param ip IP地址
     * @return 是否是IPV4或IPV6地址
     */
    public static boolean isIPAddress(String ip) {
        return InetAddressValidator.getInstance().isValid(ip);
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
        String[] str = StringUtils.splitPreserveAllTokens(ipv4, '.');
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

    public static String parseByteArrayToIPv6Address(byte[] b) {
        if (b.length != 16) {
            throw new IllegalArgumentException();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i += 2) {
            sb.append(String.format("%02X%02X", parseByteToInteger(b[i]), parseByteToInteger(b[i + 1])));
            if (i < 14) {
                sb.append(':');
            }
        }

        return sb.toString();
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
        return Short.toUnsignedInt(val);
    }

    public static int parseByteToInteger(byte val) {
        return val & 0x0FF;
    }

    private BaseUtils() {
        throw new UnsupportedOperationException();
    }
}
