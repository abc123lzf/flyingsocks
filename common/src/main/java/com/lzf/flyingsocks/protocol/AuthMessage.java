package com.lzf.flyingsocks.protocol;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.util.*;

/**
 * 客户端发送给服务器的认证消息，其报文格式为：
 * +---------+---------+---------+
 * |Auth Type| Length  |   Auth  |
 * | (1 byte)|(2 bytes)|  Message|
 * +---------+---------+---------+
 */
public class AuthMessage implements Message {
    public static final Charset DEFAULT_ENCODING = Charset.forName("UTF-8");

    private AuthMethod authMethod;

    private Map<String, String> authInfo;

    public enum AuthMethod {
        SIMPLE((byte)0x01, "password"),
        USER((byte)0x02, "user", "pass");

        private final byte head;
        private final List<String> containsKey;

        AuthMethod(byte head, String... keys) {
            this.head = head;
            List<String> kl = new ArrayList<>(keys.length);
            kl.addAll(Arrays.asList(keys));
            containsKey = Collections.unmodifiableList(kl);
        }

        public List<String> getContainsKey() {
            return containsKey;
        }

        private byte getHead() {
            return head;
        }

        private static AuthMethod getAuthMethodByHead(byte head) {
            for(AuthMethod authMethod : AuthMethod.values()) {
                if(authMethod.head == head)
                    return authMethod;
            }

            return null;
        }
    }

    AuthMessage() { }

    public AuthMessage(ByteBuf serialBuf) throws SerializationException {
        deserialize(serialBuf);
    }

    public AuthMessage(AuthMethod authMethod) {
        this.authMethod = authMethod;
    }

    public AuthMethod getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(AuthMethod authMethod) {
        this.authMethod = authMethod;
    }

    public String getContent(String key) {
        if(authInfo == null)
            return null;
        return authInfo.get(key);
    }

    public void putContent(String key, String value) {
        if(authInfo == null)
            authInfo = new HashMap<>(4);
        authInfo.put(key, value);
    }


    @Override @SuppressWarnings("unchecked")
    public ByteBuf serialize() throws SerializationException {
        if(authMethod == null)
            throw new SerializationException("Auth type should not be null");

        JSONObject msg = new JSONObject((Map)authInfo);
        List<String> keys = authMethod.getContainsKey();

        for(String key : keys) {
            if(!msg.containsKey(key))
                throw new SerializationException("illegal auth message, need key:" + key);
        }

        String str = msg.toJSONString();
        byte[] b = str.getBytes(DEFAULT_ENCODING);
        if(str.length() > Short.MAX_VALUE)
            throw new SerializationException("auth message is too long, it's should be no longer than 32767 bytes");

        ByteBuf buf = Unpooled.buffer(1 + 2 + b.length);
        buf.writeByte(authMethod.getHead());
        buf.writeShort((short)b.length);
        buf.writeBytes(b);

        return buf;
    }

    @Override
    public void deserialize(ByteBuf buf) throws SerializationException {
        AuthMethod type = AuthMethod.getAuthMethodByHead(buf.readByte());
        if(type == null)
            throw new SerializationException("unsupport auth type");

        short len = buf.readShort();
        byte[] b = new byte[len];
        buf.readBytes(b);

        JSONObject obj;
        try {
            obj = JSON.parseObject(new String(b, 0, len, DEFAULT_ENCODING));
        } catch (JSONException e) {
            throw new SerializationException("auth message error", e);
        }

        List<String> keys = type.getContainsKey();
        Map<String, String> map = new HashMap<>((int)(keys.size() * 1.6));
        for(String key : keys) {
            if(!obj.containsKey(key))
                throw new SerializationException("auth message is not complete: not found key [" + key + "]");
            map.put(key, obj.getString(key));
        }

        this.authMethod = type;
        this.authInfo = map;
    }


}