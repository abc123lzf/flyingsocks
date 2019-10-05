package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.lzf.flyingsocks.client.Client.VERSION;

/**
 * 客户端启动引导类
 */
public abstract class ClientBoot {

    private static final Logger log = LoggerFactory.getLogger(ClientBoot.class);

    public static void main(String[] args) {
        log.info("flyingsocks client {} start...", VERSION);
        long st = System.currentTimeMillis();
        try {
            Client client = new StandardClient();
            client.init();
            client.start();
            System.gc();
        } catch (ComponentException e) {
            log.error("flyingsocks client {} start failure, cause:", VERSION, e);
            log.info("If it caused by BUG, please submit issue at https://github.com/abc123lzf/flyingsocks , Thanks");
            System.exit(1);
        }

        long ed = System.currentTimeMillis();
        log.info("flyingsocks client {} start complete, use {} millisecond", VERSION, ed - st);
    }

    private ClientBoot() {
        throw new UnsupportedOperationException();
    }
}