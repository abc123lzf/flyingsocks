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
#include <jni.h>
#include <Windows.h>
/* Header for class com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy */

#ifndef _Included_com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy
#define _Included_com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy
 * Method:    isProxyEnable0
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy_isProxyEnable0
  (JNIEnv *, jclass);

/*
 * Class:     com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy
 * Method:    switchSystemProxy0
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy_switchSystemProxy0
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy
 * Method:    setupProxyServerAddress0
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy_setupProxyServerAddress0
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy
 * Method:    setupNoProxyHosts0
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy_setupNoProxyHosts0
  (JNIEnv *, jclass, jstring);

/*
 * Class:     com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy
 * Method:    obtainNoProxyHosts0
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy_obtainNoProxyHosts0
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
