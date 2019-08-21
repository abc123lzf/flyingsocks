package com.lzf.flyingsocks.client;

import com.lzf.flyingsocks.*;
import com.lzf.flyingsocks.Component;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public abstract class Client extends TopLevelComponent
        implements Component<VoidComponent>, Environment, ClientOperator {

    /**
     * 默认组件名
     */
    public static final String DEFAULT_COMPONENT_NAME = "flyingsocks-client";

    /**
     * 当前版本号
     */
    public static final String VERSION = "v1.1";

    public Client() {
        super(DEFAULT_COMPONENT_NAME);
    }

    /**
     * @return 配置管理器
     */
    public final ConfigManager<?> getConfigManager() {
        return super.getConfigManager();
    }

    @Override
    public void cleanLogFiles() {
        GlobalConfig gc = getConfigManager().getConfig(GlobalConfig.NAME, GlobalConfig.class);
        if(gc != null) {
            File folder = new File(gc.configPath() + "/log");
            if(!folder.exists() || folder.isDirectory())
                return;

            File[] files = folder.listFiles();
            if(files == null)
                return;
            for (File file : files) {
                if(file.isFile() && !file.delete())
                    log.info("Can not delete file {}", file.getName());
            }
        }
    }

    @Override
    public void openLogDirectory() {
        GlobalConfig gc = getConfigManager().getConfig(GlobalConfig.NAME, GlobalConfig.class);
        if(gc != null) {
            File folder = new File(gc.configPath() + "/log");
            try {
                Desktop.getDesktop().open(folder);
            } catch (IOException e) {
                log.warn("Open log file directory occur a exception", e);
            }
        }
    }

    @Override
    public void openBrowser(String url) {
        try {
            if(isWindows()) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else {
                Desktop dt = Desktop.getDesktop();
                if(dt.isSupported(Desktop.Action.BROWSE)) {
                    dt.browse(new URI(url));
                }
            }
        } catch (IOException | URISyntaxException e) {
            log.warn("Open browser occur a exception", e);
        }
    }
}
