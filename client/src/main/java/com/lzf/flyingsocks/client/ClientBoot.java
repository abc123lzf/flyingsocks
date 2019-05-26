package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.ComponentException;
import io.netty.util.ResourceLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientBoot {
    private static final Logger log = LoggerFactory.getLogger(ClientBoot.class);

    public static void main(String[] args) {
        log.info("flyingsocks client v1.0 start...");

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
        try {
            Client client = new StandardClient();
            client.init();
            client.start();
        } catch (ComponentException e) {
            log.error("flyingsocks client v1.0 start failure, cause:", e);
            System.exit(1);
        }

        log.info("flyingsocks client v1.0 start complete");
    }

}
