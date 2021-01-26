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
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端发送给服务器的认证消息，其报文格式为：
 *
 * +------+---------+---------+-------------------------+
 * |HEADER|Auth Type| Length  |           Auth          |
 * |      | (1 byte)|(2 bytes)|         Message         |
 * +------+---------+---------+-------------------------+
 */
public class AuthRequestMessage extends Message {

    private static final byte[] HEADER = new byte[] { 0x6C, 0x7A, 0x66 };

    public static final int LENGTH_OFFSET = HEADER.length + 1;  //HEADER.length + sizeof(type)

    public static final int LENGTH_SIZE = 2;  //sizeof(short)

    public static final int LENGTH_ADJUSTMENT = 0;

    /**
     * Auth Message字段的编码格式
     */
    private static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

    /**
     * 认证方式
     */
    private byte type;

    /**
     * 认证参数
     */
    private Map<String, String> parameters;


    public AuthRequestMessage(ByteBuf serialBuf) throws SerializationException {
        super(serialBuf);
    }


    public AuthRequestMessage(byte type) {
        this.type = type;
    }


    public byte getAuthType() {
        return type;
    }

    public void setAuthType(byte type) {
        this.type = type;
    }

    public String getParameter(String key) {
        Map<String, String> parameters = this.parameters;
        if (parameters == null) {
            return null;
        }

        return parameters.get(key);
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(this.parameters);
    }

    public void putContent(String key, String value) {
        Map<String, String> parameters = this.parameters;
        if (parameters == null) {
            parameters = new HashMap<>(4);
            this.parameters = parameters;
        }

        parameters.put(key, value);
    }


    @Override
    public ByteBuf serialize(ByteBufAllocator allocator) throws SerializationException {
        Map<String, String> parameters = this.parameters;
        byte type = this.type;
        if (parameters == null) {
            throw new SerializationException("Auth type should not be null");
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        JSONObject msg = new JSONObject((Map) parameters);
        String str = msg.toJSONString();
        byte[] b = Base64.getEncoder().encode(str.getBytes(DEFAULT_ENCODING));
        if (b.length > Short.MAX_VALUE) {
            throw new SerializationException("auth message is too long, it's should be no longer than 32767 bytes");
        }

        ByteBuf buf = allocator.directBuffer(HEADER.length + 1 + 2 + b.length);
        buf.writeBytes(HEADER);
        buf.writeByte(type);
        buf.writeShort((short) b.length);
        buf.writeBytes(b);

        return buf;
    }


    @Override
    protected void deserialize(ByteBuf buf) throws SerializationException {
        for (byte b : HEADER) {
            if (b != buf.readByte()) {
                throw new SerializationException(AuthRequestMessage.class, "Wrong header");
            }
        }

        this.type = buf.readByte();

        short len = buf.readShort();
        byte[] src = new byte[len];
        buf.readBytes(src);

        byte[] message = Base64.getDecoder().decode(src);
        JSONObject obj;
        try {
            obj = JSON.parseObject(new String(message, 0, message.length, DEFAULT_ENCODING));
        } catch (JSONException e) {
            throw new SerializationException("auth message error", e);
        }

        Map<String, String> parameters = new HashMap<>(obj.size() * 2);
        obj.forEach((k, v) -> parameters.put(k, v.toString()));
        this.parameters = parameters;
    }


    public static byte[] getMessageHeader() {
        return Arrays.copyOf(HEADER, HEADER.length);
    }


    @Override
    public String toString() {
        return "AuthRequestMessage{" +
                "type=" + type +
                ", parameters=" + parameters +
                '}';
    }
}