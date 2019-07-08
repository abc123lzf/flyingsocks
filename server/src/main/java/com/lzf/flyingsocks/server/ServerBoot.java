package com.lzf.flyingsocks.server;

import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ServerBoot {

    private static final Logger log = LoggerFactory.getLogger(ServerBoot.class);

    private static final Server server = new StandardServer();

    public static void main(String[] args) {
        if(server.getState() != LifecycleState.NEW)
            throw new Error();
        boot();
    }

    public static void boot() {
        synchronized (ServerBoot.class) {
            if(server.getState() != LifecycleState.NEW)
                return;

            log.info("flyingsocks server v1.0 start...");
            try {
                server.init();
                server.start();
                log.info("flyingsocks server v1.0 start complete");
            } catch (ComponentException e) {
                log.error("flyingsocks server v1.0 start failure, cause:", e);
            }
        }
    }

    public static void shutdown() {
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