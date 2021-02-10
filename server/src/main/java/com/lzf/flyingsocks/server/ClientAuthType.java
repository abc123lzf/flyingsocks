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
package com.lzf.flyingsocks.server;

/**
 * 客户端认证方式
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/26 19:58
 */
public enum ClientAuthType {

    SIMPLE(0, "simple"), USER(1, "user");

    private final byte messageHeader;

    private final String configValue;

    ClientAuthType(int messageHeader, String configValue) {
        this.messageHeader = (byte) messageHeader;
        this.configValue = configValue;
    }

    public byte getMessageHeader() {
        return messageHeader;
    }

    public String configValue() {
        return configValue;
    }

    public static ClientAuthType configValueOf(String value) {
        for (ClientAuthType type : values()) {
            if (type.configValue().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
