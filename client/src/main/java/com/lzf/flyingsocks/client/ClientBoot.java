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
package com.lzf.flyingsocks.client;

import com.alibaba.fastjson.JSON;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.locks.LockSupport;

/**
 * 客户端启动引导类
 */
public abstract class ClientBoot {

    private static final Logger log = LoggerFactory.getLogger(ClientBoot.class);

    private static final Client client = new StandardClient();

    public static void main(String[] args) {
        if (log.isDebugEnabled()) {
            log.debug("System properties: {}", JSON.toJSONString(System.getProperties()));
            log.debug("System env: {}", JSON.toJSONString(System.getenv()));
        }

        log.info("flyingsocks client {} start...", client.getVersion());
        long st = System.currentTimeMillis();
        try {
            client.init();
            client.start();
            System.gc();
        } catch (ComponentException e) {
            log.error("flyingsocks client {} start failure, cause:", client.getVersion(), e);
            log.info("submit issue at https://github.com/abc123lzf/flyingsocks");
            Client.exitWithNotify(1, "exitmsg.client_boot.start_failure", e.getMessage());
        }

        long ed = System.currentTimeMillis();
        log.info("flyingsocks client {} start complete, use {} millisecond", client.getVersion(), ed - st);

        boolean running = client.runGUITask();
        if (!running) {
            Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (!client.getState().after(LifecycleState.STOPING)) {
                    client.stop();
                }
                LockSupport.unpark(mainThread);
            }, "Thread-ShutdownHook"));

            LockSupport.park();
            log.info("Shutdown client at {}", LocalDateTime.now());
        }
    }


    private ClientBoot() {
        throw new UnsupportedOperationException();
    }
}