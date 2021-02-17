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
package com.lzf.flyingsocks.protocol;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * +------+------+-----+-----------------+
 * |HEADER| SUCC | LEN |    EXTRA_DATA   |
 * +------+------+-----+-----------------+
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/26 3:07
 */
public class AuthResponseMessage extends Message {

    private static final byte[] HEADER = new byte[] { 0x6C, 0x7A, 0x66 };

    private static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

    public static final int LENGTH_OFFSET = HEADER.length + 1;

    public static final int LENGTH_SIZE = 2;  //sizeof(short)

    public static final int LENGTH_ADJUSTMENT = 0;

    /**
     * 是否通过认证
     */
    private boolean success;

    /**
     * 附加信息
     */
    private Map<String, String> extraData;


    public AuthResponseMessage(boolean success) {
        this.success = success;
    }

    public AuthResponseMessage(ByteBuf buf) throws SerializationException {
        super(buf);
    }

    public boolean isSuccess() {
        return success;
    }

    public Map<String, String> getExtraData() {
        Map<String, String> extraData = this.extraData;
        return extraData != null ? Collections.unmodifiableMap(extraData) : null;
    }

    @Override
    public ByteBuf serialize(ByteBufAllocator allocator) throws SerializationException {
        Map<String, String> extraData = this.extraData;
        boolean success = this.success;

        int len = HEADER.length + 1;
        byte[] data = null;
        if (extraData != null) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            JSONObject obj = new JSONObject((Map) extraData);
            data = obj.toJSONString().getBytes(DEFAULT_ENCODING);
        }

        if (data != null) {
            if (data.length > Short.MAX_VALUE) {
                throw new SerializationException(AuthResponseMessage.class, "Extra data length > 32767");
            }
            len += data.length;
        }

        ByteBuf result = allocator.directBuffer(len);
        result.writeBytes(HEADER);
        result.writeBoolean(success);
        if (data != null) {
            result.writeShort((short) data.length);
            result.writeBytes(data);
        } else {
            result.writeShort(0);
        }

        return result;
    }

    @Override
    protected void deserialize(ByteBuf buf) throws SerializationException {
        for (byte b : HEADER) {
            if (buf.readByte() != b) {
                throw new SerializationException(AuthResponseMessage.class, "Wrong header");
            }
        }

        boolean success = buf.readBoolean();
        short extraDataLen = buf.readShort();

        if (extraDataLen > 0) {
            byte[] dst = new byte[extraDataLen];
            buf.readBytes(dst);
            JSONObject obj = JSON.parseObject(new String(dst, DEFAULT_ENCODING));
            this.extraData = new HashMap<>(obj.size() * 2);
        }

        this.success = success;
    }


    public static byte[] getMessageHeader() {
        return Arrays.copyOf(HEADER, HEADER.length);
    }
}
