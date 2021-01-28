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

import static com.lzf.flyingsocks.client.Client.VERSION;

/**
 * 客户端启动引导类
 */
public abstract class ClientBoot {

    private static final Logger log = LoggerFactory.getLogger(ClientBoot.class);

    public static void main(String[] args) {
        if (log.isDebugEnabled()) {
            log.debug("System properties: {}", JSON.toJSONString(System.getProperties()));
            log.debug("System env: {}", JSON.toJSONString(System.getenv()));
        }

        log.info("flyingsocks client {} start...", VERSION);
        long st = System.currentTimeMillis();
        Client client = new StandardClient();
        try {
            client.init();
            client.start();
            System.gc();
        } catch (ComponentException e) {
            log.error("flyingsocks client {} start failure, cause:", VERSION, e);
            log.info("submit issue at https://github.com/abc123lzf/flyingsocks");
            System.exit(1);
        }

        long ed = System.currentTimeMillis();
        log.info("flyingsocks client {} start complete, use {} millisecond", VERSION, ed - st);

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