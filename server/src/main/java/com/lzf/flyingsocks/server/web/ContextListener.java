package com.lzf.flyingsocks.server.web;

import com.lzf.flyingsocks.server.ServerBoot;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public final class ContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServerBoot.boot();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServerBoot.shutdown();
    }
}
