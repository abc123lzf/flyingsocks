package com.lzf.flyingsocks.server;

import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.lzf.flyingsocks.server.Server.VERSION;

/**
 * 服务器启动引导类
 */
public abstract class ServerBoot {

    private static final Logger log = LoggerFactory.getLogger(ServerBoot.class);

    private static final Server server = new StandardServer();

    public static void main(String[] args) {
        if(server.getState() != LifecycleState.NEW) {
            throw new Error();
        }

        boot();
    }

    private static void boot() {
        synchronized (ServerBoot.class) {
            if(server.getState() != LifecycleState.NEW) {
                return;
            }

            long st = System.currentTimeMillis();
            log.info("flyingsocks server {} start...", VERSION);
            try {
                server.init();
                server.start();
                long ed = System.currentTimeMillis();
                log.info("flyingsocks server {} start complete, use {} millisecond", VERSION, ed - st);

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    log.info("flyingsocks server {} ready to shutdown", VERSION);
                    shutdown();
                    log.info("flyingsocks server {} shut down complete", VERSION);
                }));

            } catch (ComponentException e) {
                log.error("flyingsocks server {} start failure, cause:", VERSION);
                log.info("If it caused by BUG, please submit issue at https://github.com/abc123lzf/flyingsocks , Thanks");
            }
        }
    }

    private static void shutdown() {
        synchronized (ServerBoot.class) {
            if(server.getState() != LifecycleState.STARTED)
                throw new IllegalStateException("Server state is STARTED");

            server.stop();
        }
    }


    private ServerBoot() {
        throw new UnsupportedOperationException();
    }
}