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

import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 服务器启动引导类
 */
public abstract class ServerBoot {

    private static final Logger log = LoggerFactory.getLogger(ServerBoot.class);

    private static final Server server = new StandardServer();

    public static void main(String[] args) {
        if (server.getState() != LifecycleState.NEW) {
            throw new Error();
        }

        boot();
    }

    private static void boot() {
        synchronized (ServerBoot.class) {
            if (server.getState() != LifecycleState.NEW) {
                return;
            }

            printBanner();
            long st = System.currentTimeMillis();
            log.info("flyingsocks server {} start...", server.getVersion());
            try {
                server.init();
                server.start();
                long ed = System.currentTimeMillis();
                log.info("flyingsocks server {} start complete, use {} millisecond", server.getVersion(), ed - st);

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    log.info("flyingsocks server {} ready to shutdown", server.getVersion());
                    shutdown();
                    log.info("flyingsocks server {} shut down complete", server.getVersion());
                }));

            } catch (ComponentException e) {
                log.error("flyingsocks server {} start failure, cause:", server.getVersion());
                log.info("If it caused by BUG, please submit issue at https://github.com/abc123lzf/flyingsocks , Thanks");
            }
        }
    }

    private static void shutdown() {
        synchronized (ServerBoot.class) {
            if (server.getState() != LifecycleState.STARTED) {
                throw new IllegalStateException("Server state is STARTED");
            }

            server.stop();
        }
    }

    private static void printBanner() {
        try (InputStream is = server.getConfigManager().loadResource("classpath://META-INF/banner");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException ignore) {
            // NOOP
        }
    }


    private ServerBoot() {
        throw new UnsupportedOperationException();
    }
}