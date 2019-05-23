package com.lzf.flyingsocks.client.view;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.ComponentException;
import com.lzf.flyingsocks.client.Client;
import com.lzf.flyingsocks.client.GlobalConfig;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class ViewComponent extends AbstractComponent<Client> {

    private ServerSettingModule serverSettingModule;
    private TrayModule trayModule;

    public ViewComponent(Client client) {
        super("ViewComponent", Objects.requireNonNull(client));
    }

    @Override
    protected void initInternal() {
        GlobalConfig cfg = parent.getConfigManager().getConfig(GlobalConfig.NAME, GlobalConfig.class);
        if(!cfg.isOpenGUI())
            return;

        if(!SystemTray.isSupported())
            throw new UnsupportedOperationException("System not support the tray.");

        Image image;
        try {
            image = ImageIO.read(getParentComponent().loadResource("classpath://icon.png"));
        } catch (IOException e) {
            throw new ComponentException("file \"icon.png\" not found.", e);
        }


        this.trayModule = new TrayModule(this, image);
        addModule(trayModule);

        this.serverSettingModule = new ServerSettingModule(this, image);
        addModule(serverSettingModule);
    }
}
