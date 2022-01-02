package com.lzf.flyingsocks.client.gui.swing.serverconfigure;

import com.lzf.flyingsocks.client.gui.model.ServerVO;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author lizifan
 * @since 2022.1.2 18:51
 */
class ServerListUIContainer {

    private final JList<ServerVO> serverList;

    private Consumer<ServerVO> serverListSelectionListener;

    public ServerListUIContainer(JList<ServerVO> serverList) {
        serverList.setModel(new DefaultListModel<>());
        this.serverList = serverList;
        serverList.addListSelectionListener(e -> {
            ServerVO server = serverList.getSelectedValue();
            if (serverListSelectionListener != null) {
                serverListSelectionListener.accept(server);
            }
        });
    }

    public void setServerListSelectionListener(Consumer<ServerVO> serverListSelectionListener) {
        this.serverListSelectionListener = serverListSelectionListener;
    }

    public void refresh(List<ServerVO> source) {
        if (source == null) {
            return;
        }
        DefaultListModel<ServerVO> model = new DefaultListModel<>();
        model.addAll(source);
        this.serverList.setModel(model);
    }

    public void addEmpty() {
        DefaultListModel<ServerVO> model = (DefaultListModel<ServerVO>) this.serverList.getModel();
        model.addElement(new ServerVO());
    }

    public ServerVO getSelected() {
        return this.serverList.getSelectedValue();
    }

    public void remove(ServerVO vo) {
        DefaultListModel<ServerVO> model = (DefaultListModel<ServerVO>) this.serverList.getModel();
        model.removeElement(vo);
    }
}