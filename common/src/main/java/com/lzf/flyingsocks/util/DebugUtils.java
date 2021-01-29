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
package com.lzf.flyingsocks.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseDecoder;

import static java.lang.System.out;

/**
 * 仅Debug使用
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/1/29 2:06
 */
@SuppressWarnings("unused")
public final class DebugUtils {

    private static final String[] HexStringMap;

    static {
        HexStringMap = new String[256];
        for (int i = 0; i < 255; i++) {
            String str = Integer.toHexString(i).toUpperCase();
            HexStringMap[i] = str.length() == 1 ? "0" + str : str;
        }
    }


    public static void printByteBuf(ByteBuf buf) {
        out.println("--------------------------Debug Util----------------------------");
        out.printf("ReadableBytes: %d", buf.readableBytes());
        out.println();
        out.println("00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F|0123456789ABCDEF");
        out.println("-----------------------------------------------|----------------");

        int index = buf.readerIndex();
        int size = buf.readableBytes();

        char[] arr = new char[16];
        int k = 0;

        for (int i = 0; i < size; i++) {
            byte b = buf.getByte(index + i);
            arr[k++] = (char) (b & 0xFF);
            if (k == 16) {
                for (char c : arr) {
                    out.print(HexStringMap[c]);
                    out.print(' ');
                }

                for (char c : arr) {
                    if (c >= 32 && c <= 126) {
                        out.print(c);
                    } else {
                        out.print('.');
                    }
                }

                out.println();

                k = 0;
            }
        }

        if (k > 0) {
            for (int i = 0; i < k; i++) {
                char c = arr[i];
                out.print(HexStringMap[c]);
                out.print(' ');
            }

            for (int i = k; i < 16; i++) {
                out.print("   ");
            }

            for (int i = 0; i < k; i++) {
                char c = arr[i];
                if (c >= 32 && c <= 126) {
                    out.print(c);
                } else {
                    out.print('.');
                }
            }
        }


        out.println();
        out.println("-----------------------------DONE-------------------------------");
    }


    public static void printHttpResponse(ByteBuf response) {
        EmbeddedChannel channel = new EmbeddedChannel(new HttpResponseDecoder());
        channel.writeInbound(response);
        HttpResponse resp = channel.readInbound();
        printHttpResponse(resp);
    }

    public static void printHttpResponse(HttpResponse response) {
        out.println("--------------------------Debug Util----------------------------");
        out.println(response.protocolVersion() + " " + response.status());
        HttpHeaders headers = response.headers();
        headers.forEach(entry -> {
            out.println(entry.getKey() + ": " + entry.getValue());
        });
        out.println("-----------------------------DONE-------------------------------");
    }



    private DebugUtils() {}

}
