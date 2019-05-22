package com.lzf.flyingsocks.server;

import com.lzf.flyingsocks.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerBoot {

    private static final Logger log = LoggerFactory.getLogger(ServerBoot.class);

    public static void main(String[] args) {
        log.info("flyingsocks server v1.0 start...");
        Server server = new StandardServer();
        try {
            server.init();
            server.start();
            log.info("flyingsocks server v1.0 start complete");
        } catch (ComponentException e) {
            log.error("flyingsocks server v1.0 start failure, cause:", e);
        }
    }

}
