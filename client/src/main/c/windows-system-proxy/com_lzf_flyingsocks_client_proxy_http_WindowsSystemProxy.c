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
#include "com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy.h"

const char* reg_path = "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Internet Settings";
const char* reg_config_proxy_server = "ProxyServer";
const char* reg_config_proxy_override = "ProxyOverride";
const char* reg_config_proxy_enable = "ProxyEnable";

static void throw_runtime_exception(JNIEnv *env, char* message) {
    jclass class = (*env)->FindClass(env, "java/lang/RuntimeException");
    (*env)->ThrowNew(env, class, message);
}

static LSTATUS open_config_hkey(HKEY *hkey) {
    return RegOpenKeyExA(HKEY_CURRENT_USER, reg_path, 0, KEY_ALL_ACCESS | KEY_WOW64_64KEY, hkey);
}


JNIEXPORT jboolean JNICALL Java_com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy_isProxyEnable0
        (JNIEnv *env, jclass class) {
    HKEY hkey;
    if (open_config_hkey(&hkey) != ERROR_SUCCESS) {
        char message[255];
        sprintf(message, "Could NOT open register [PATH: %s\\%s]", reg_path, reg_config_proxy_enable);
        throw_runtime_exception(env, message);
        return JNI_FALSE;
    }

    DWORD val;
    unsigned long pcbData = 4;
    LSTATUS res = RegGetValueA(hkey, NULL, reg_config_proxy_enable, RRF_RT_REG_DWORD, NULL, &val, &pcbData);
    RegCloseKey(hkey);
    if (res == ERROR_SUCCESS) {
        return val == 0 ? JNI_FALSE : JNI_TRUE;
    }

    char message[255];
    sprintf(message, "Could NOT query register [PATH: %s\\%s] ERROR_CODE: %ld", reg_path, reg_config_proxy_enable, res);
    throw_runtime_exception(env, message);
    return JNI_FALSE;
}


JNIEXPORT jboolean JNICALL Java_com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy_switchSystemProxy0
        (JNIEnv *env, jclass class, jboolean enable) {
    HKEY hkey;
    if (open_config_hkey(&hkey) != ERROR_SUCCESS) {
        return JNI_FALSE;
    }

    DWORD data = (enable == JNI_TRUE ? 1 : 0);
    LSTATUS res = RegSetValueExA(hkey, reg_config_proxy_enable, 0, REG_DWORD, (const BYTE *) &data, sizeof(DWORD));
    if (res == ERROR_SUCCESS) {
        RegCloseKey(hkey);
        return JNI_TRUE;
    }

    RegCloseKey(hkey);
    char message[255];
    sprintf(message, "Could NOT query register [PATH: %s\\%s] ERROR_CODE: %ld", reg_path, reg_config_proxy_enable, res);
    throw_runtime_exception(env, message);
    return JNI_FALSE;
}


JNIEXPORT jboolean JNICALL Java_com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy_setupProxyServerAddress0
        (JNIEnv *env, jclass class, jstring address) {
    HKEY hkey;
    if (open_config_hkey(&hkey) != ERROR_SUCCESS) {
        return JNI_FALSE;
    }

    jboolean copy = JNI_FALSE;
    const char* str = (*env)->GetStringUTFChars(env, address, &copy);
    jsize len = (*env)->GetStringUTFLength(env, address);

    LSTATUS res = RegSetValueExA(hkey, reg_config_proxy_server, 0, RRF_RT_REG_SZ, (BYTE*) str, len + 1);
    if (res == ERROR_SUCCESS) {
        RegCloseKey(hkey);
        return JNI_TRUE;
    }

    RegCloseKey(hkey);
    char message[255];
    sprintf(message, "Could NOT query register [PATH: %s\\%s] ERROR_CODE: %ld", reg_path, reg_config_proxy_server, res);
    throw_runtime_exception(env, message);
    return JNI_FALSE;
}


JNIEXPORT jboolean JNICALL Java_com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy_setupNoProxyHosts0
        (JNIEnv *env, jclass class, jstring hosts) {
    HKEY hkey;
    if (open_config_hkey(&hkey) != ERROR_SUCCESS) {
        return JNI_FALSE;
    }

    jboolean copy = JNI_FALSE;
    const char* str = (*env)->GetStringUTFChars(env, hosts, &copy);
    jsize len = (*env)->GetStringUTFLength(env, hosts);

    LSTATUS res = RegSetValueExA(hkey, reg_config_proxy_override, 0, RRF_RT_REG_SZ, (BYTE*) str, len + 1);
    if (res == ERROR_SUCCESS) {
        RegCloseKey(hkey);
        return JNI_TRUE;
    }

    RegCloseKey(hkey);
    char message[255];
    sprintf(message, "Could NOT query register [PATH: %s\\%s] ERROR_CODE: %ld", reg_path, reg_config_proxy_override, res);
    throw_runtime_exception(env, message);
    return JNI_FALSE;
}


JNIEXPORT jstring JNICALL Java_com_lzf_flyingsocks_client_proxy_http_WindowsSystemProxy_obtainNoProxyHosts0
        (JNIEnv *env, jclass class) {
    HKEY hkey;
    if (open_config_hkey(&hkey) != ERROR_SUCCESS) {
        return NULL;
    }

    char buf[1024];
    unsigned long pcbData = sizeof(buf);
    LSTATUS res = RegGetValueA(hkey, NULL, reg_config_proxy_override, RRF_RT_REG_SZ, NULL, buf, &pcbData);
    if (res == ERROR_SUCCESS) {
        RegCloseKey(hkey);
        return (*env)->NewStringUTF(env, buf);
    } else if (res == ERROR_MORE_DATA) {
        char* new_buf = malloc(pcbData + 10);
        res = RegGetValueA(hkey, NULL, reg_config_proxy_override, RRF_RT_REG_SZ, NULL, new_buf, &pcbData);
        if (res == ERROR_SUCCESS) {
            jstring result = (*env)->NewStringUTF(env, buf);
            free(new_buf);
            RegCloseKey(hkey);
            return result;
        }
        free(new_buf);
    }

    RegCloseKey(hkey);
    char message[255];
    sprintf(message, "Could NOT query register [PATH: %s\\%s] ERROR_CODE: %ld", reg_path, reg_config_proxy_override, res);
    throw_runtime_exception(env, message);
    return NULL;
}