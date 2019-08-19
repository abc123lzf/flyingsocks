package com.lzf.flyingsocks.client.gui.swing;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.client.Client;
import com.lzf.flyingsocks.client.GlobalConfig;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;

/**
 * Swing GUI组件
 */
public class SwingViewComponent extends AbstractComponent<Client> {

    /**
     * 服务器设置界面
     */
    private ServerSettingModule serverSettingModule;

    /**
     * 系统托盘模块
     */
    private TrayModule trayModule;

    /**
     * Socks代理设置界面
     */
    private SocksSettingModule socksSettingModule;

    public SwingViewComponent(Client client) {
        super("SwingViewComponent", Objects.requireNonNull(client));
    }

    @Override
    protected void initInternal() {
        try {
            BeautyEyeLNFHelper.launchBeautyEyeLNF();
        } catch (Exception e) {
            log.warn("BeautyEyeLNFHelper exception", e);
        }

        GlobalConfig cfg = parent.getConfigManager().getConfig(GlobalConfig.NAME, GlobalConfig.class);
        if(!cfg.isOpenGUI())
            return;

        if(!SystemTray.isSupported())
            throw new UnsupportedOperationException("System not support the tray.");

        Image image;
        try {
            image = ResourceManager.loadIconImage();
        } catch (IOException e) {
            throw new ComponentException("file \"icon.png\" load error.", e);
        }

        Image smallImage;
        try {
            smallImage = ResourceManager.loadSystemTrayImage();
        } catch (IOException e) {
            throw new ComponentException("file \"icon-tray.png\" load error.", e);
        }

        this.trayModule = new TrayModule(this, smallImage);
        addModule(trayModule);

        this.serverSettingModule = new ServerSettingModule(this, image);
        addModule(serverSettingModule);

        this.socksSettingModule = new SocksSettingModule(this, image);
        addModule(socksSettingModule);
    }

}
