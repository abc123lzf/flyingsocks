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
package com.lzf.flyingsocks.server.core.client;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.ConfigManager;
import com.lzf.flyingsocks.encrypt.EncryptProvider;
import com.lzf.flyingsocks.encrypt.EncryptSupport;
import com.lzf.flyingsocks.encrypt.JksSSLEncryptProvider;
import com.lzf.flyingsocks.encrypt.OpenSSLEncryptProvider;
import com.lzf.flyingsocks.protocol.AuthMessage;
import com.lzf.flyingsocks.server.ServerConfig;
import com.lzf.flyingsocks.server.core.OpenSSLConfig;
import com.lzf.flyingsocks.server.core.ProxyProcessor;
import com.lzf.flyingsocks.server.db.UserDatabase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用于处理FS客户端连接的组件
 */
public class ClientProcessor extends AbstractComponent<ProxyProcessor> {

    public ClientProcessor(ProxyProcessor proxyProcessor) {
        super("ClientProcessor", Objects.requireNonNull(proxyProcessor));
    }

    @Override
    protected void initInternal() {
        ServerConfig.Node node = parent.getServerConfig();
        EncryptProvider provider;
        switch (node.encryptType) {
            case OpenSSL:
                provider = EncryptSupport.lookupProvider("OpenSSL");
                break;
            case JKS:
                provider = EncryptSupport.lookupProvider("JKS");
                break;
            case None:
                provider = null;
                break;
            default: {
                log.error("Unsupport encrypt type {}", node.encryptType);
                System.exit(1);
                return;
            }
        }

        if (provider != null) {
            if (provider instanceof OpenSSLEncryptProvider) {
                ConfigManager<?> manager = parent.getParentComponent().getConfigManager();
                OpenSSLConfig cfg = new OpenSSLConfig(manager);
                manager.registerConfig(cfg);

                Map<String, Object> params = new HashMap<>(8);

                try (InputStream certIs = cfg.openRootCertStream()) {
                    byte[] buf = new byte[10240];
                    int r = certIs.read(buf);
                    byte[] file = new byte[r];
                    System.arraycopy(buf, 0, file, 0, r);
                    addComponent(new CertRequestProcessor(this, file));
                    ByteArrayInputStream bais = new ByteArrayInputStream(file);
                    params.put("file.cert", bais);
                } catch (IOException e) {
                    log.error("Exception occur at CA cert MD5 calcuate", e);
                    System.exit(1);
                }

                try {
                    params.put("client", false);
                    params.put("file.cert.root", cfg.openRootCertStream());
                    params.put("file.key", cfg.openKeyStream());
                } catch (IOException e) {
                    log.error("Can not open CA file stream", e);
                    System.exit(1);
                }

                try {
                    provider.initialize(params);
                } catch (Exception e) {
                    log.error("Load OpenSSL Module occur a exception", e);
                    throw new ComponentException(e);
                }

            } else if (provider instanceof JksSSLEncryptProvider) {
                throw new ComponentException("Unsupport JKS encrypt method");
            } else {
                throw new ComponentException("Unsupport other encrypt method");
            }
        }

        addComponent(new ProxyRequestProcessor(this, provider));

        super.initInternal();
    }

    @Override
    protected void startInternal() {
        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        super.stopInternal();
    }

    /**
     * 对客户端认证报文进行比对
     *
     * @param msg 客户端认证报文
     * @return 是否通过认证
     */
    boolean doAuth(AuthMessage msg) {
        ServerConfig.Node n = parent.getServerConfig();
        if (n.authType.authMethod != msg.getAuthMethod()) { //如果认证方式不匹配
            return false;
        }

        if (n.authType == ServerConfig.AuthType.SIMPLE) {
            List<String> keys = msg.getAuthMethod().getContainsKey();
            for (String key : keys) {
                if (!n.getArgument(key).equals(msg.getContent(key)))
                    return false;
            }
            return true;
        } else if (n.authType == ServerConfig.AuthType.USER) {
            String group = n.getArgument("group");
            UserDatabase db = parent.getParentComponent().getUserDatabase();

            return db.doAuth(group, msg.getContent("user"), msg.getContent("pass"));
        }

        return false;
    }
}
