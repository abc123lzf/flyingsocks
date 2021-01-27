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
#include "transparent.h"

static jstring parse_address(JNIEnv* env, struct in_addr* addr) {
    char string[40];
    inet_ntop(AF_INET, addr, string, sizeof(string));
    return (*env)->NewStringUTF(env, string);
}

JNIEXPORT jobject JNICALL
Java_com_lzf_flyingsocks_client_proxy_transparent_LinuxNative_getTargetAddress0(JNIEnv* env, jclass class, jint fd) {
    struct sockaddr_in remote_addr = {0};
    socklen_t socklen = 0;
    int opt_res = getsockopt(fd, SOL_IP, SO_ORIGINAL_DST, &remote_addr, &socklen);
    if (opt_res != 0) {
        return NULL;
    }

    jint port = remote_addr.sin_port;
    jstring host = parse_address(env, &remote_addr.sin_addr);

    jobject address_class = (*env)->FindClass(env, "java.net.InetSocketAddress");
    jmethodID address_method = (*env)->GetMethodID(env, address_class, "createUnresolved", "(Ljava/lang/String;I)Ljava/net/InetSocketAddress");
    return (*env)->CallStaticObjectMethod(env, address_class, address_method, host, port);
}
