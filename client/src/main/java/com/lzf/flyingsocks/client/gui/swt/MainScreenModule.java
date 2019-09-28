package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.Node;

/**
 * 主界面
 */
class MainScreenModule extends AbstractModule<SWTViewComponent> {

    private final Display display;

    private final Shell shell;

    private final ClientOperator operator;

    private static final Color BUTTON_FOCUS_COLOR = new Color(null, 101, 181, 255);
    private static final Color BUTTON_NORMAL_COLOR = new Color(null, 81, 161, 243);

    MainScreenModule(SWTViewComponent component, Display display) {
        super(Objects.requireNonNull(component), "Main-Screen");
        this.display = display;
        this.operator = getComponent().getParentComponent();
        this.shell = initial();
        initialUI();
    }

    private Shell initial() {
        Shell shell = new Shell(display);
        shell.setSize(640, 540);
        shell.setText("主界面");
        shell.setVisible(false);
        shell.setBackground(new Color(display, 255, 255, 255));
        try (InputStream is = ResourceManager.openIconImageStream()) {
            shell.setImage(new Image(display, is));
        } catch (IOException e) {
            throw new Error(e);
        }
        shell.addListener(SWT.Close, e -> {
            e.doit = false;
            setVisiable(false);
        });
        return shell;
    }

    private void initialUI() {
        try (InputStream is = ResourceManager.openFlyingsocksImageStream()) {
            createLabel(null, 0, 0, 620, 180, new Image(display, is));
        } catch (IOException e) {
            throw new Error(e);
        }

        createLabel("选择服务器", 20, 200, 140, 40, null);
        new ServerList(160, 200, 460, 40);
    }


    private final class ServerList {
        private final SortedMap<Integer, Node> nodeMap = new TreeMap<>();
        private final Combo combo;
        private final Button connBtn;
        private boolean disconnect;

        ServerList(int x, int y, int width, int height) {
            Combo combo = new Combo(shell, SWT.READ_ONLY);
            combo.setBounds(x, y, width, height);
            this.combo = combo;
            Button conn = new Button(shell, SWT.PUSH);
            conn.setBounds(210, 260, 200, 40);
            conn.setBackground(BUTTON_NORMAL_COLOR);
            conn.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    conn.setBackground(BUTTON_FOCUS_COLOR);

                }
                @Override
                public void focusLost(FocusEvent e) {
                    conn.setBackground(BUTTON_NORMAL_COLOR);
                }
            });
            this.connBtn = conn;

            changeConnBtn(false);
            update();
            operator.registerProxyServerConfigListener(Config.UPDATE_EVENT, this::update, false);
            conn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if(disconnect) {
                        nodeMap.forEach((k, v) -> {
                            if(v.isUse()) {
                                operator.setProxyServerUsing(v, false);
                            }
                        });
                    } else {
                        Node n = selectNode();
                        if(n != null) {
                            operator.setProxyServerUsing(n, true);
                        } else {
                            showMessageBox("提示", "请选择一个有效的服务器", SWT.ICON_INFORMATION | SWT.OK);
                        }
                    }
                }
            });
        }

        private void update() {
            Node[] nodes = operator.getServerNodes();
            int cnt = combo.getItemCount();
            boolean use = false;
            for (int i = 0; i < nodes.length; i++) {
                nodeMap.put(i, nodes[i]);
                String text = nodes[i].getHost() + ":" + nodes[i].getPort();
                if(i < cnt) {
                    combo.setItem(i, text);
                } else {
                    combo.add(text, i);
                }

                if(nodes[i].isUse()) {
                    combo.select(i);
                    use = true;
                }
            }

            if(use) {
                changeConnBtn(true);
            } else {
                changeConnBtn(false);
            }
        }

        private void changeConnBtn(boolean disconnect) {
            if(disconnect) {
                connBtn.setText("断开连接");
                this.disconnect = true;
            } else {
                connBtn.setText("连接");
                this.disconnect = false;
            }
        }

        Node selectNode() {
            return nodeMap.get(combo.getSelectionIndex());
        }
    }

    void setVisiable(boolean visiable) {
        shell.setVisible(visiable);
    }

    private void createLabel(String text, int x, int y, int width, int height, Image image) {
        Label l = new Label(shell, SWT.CENTER);
        l.setBounds(x, y, width, height);
        l.setBackground(new Color(display, 255, 255, 255));
        /*l.setFont(new Font(display, "微软雅黑", height - 4, SWT.NORMAL));*/
        if(text != null) {
            l.setText(text);
        }

        if(image != null) {
            l.setImage(image);
        }
    }

    private void showMessageBox(String title, String content, int setting) {
        MessageBox box = new MessageBox(shell, setting);
        box.setText(title);
        box.setMessage(content);
        box.open();
    }
}
