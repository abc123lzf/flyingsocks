package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import com.lzf.flyingsocks.client.proxy.ConnectionState;
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
final class MainScreenModule extends AbstractModule<SWTViewComponent> {

    private final Display display;

    private final Shell shell;

    private final ClientOperator operator;

    private final Label connStatusLabel;

    private static final Color BUTTON_FOCUS_COLOR = new Color(null, 101, 181, 255);
    private static final Color BUTTON_NORMAL_COLOR = new Color(null, 81, 161, 243);

    MainScreenModule(SWTViewComponent component, Display display) {
        super(Objects.requireNonNull(component), "Main-Screen");
        this.display = display;
        this.operator = getComponent().getParentComponent();
        Shell sh = new Shell(display);
        sh.setSize(640, 540);
        sh.setText("主界面");
        sh.setVisible(false);
        sh.setBackground(new Color(display, 255, 255, 255));
        try (InputStream is = ResourceManager.openIconImageStream()) {
            sh.setImage(new Image(display, is));
        } catch (IOException e) {
            throw new Error(e);
        }
        sh.addListener(SWT.Close, e -> {
            e.doit = false;
            setVisiable(false);
        });
        this.shell = sh;

        Label status = new Label(sh, SWT.LEFT);
        status.setBounds(0, 425, 620, 30);
        this.connStatusLabel = status;

        try (InputStream is = ResourceManager.openFlyingsocksImageStream()) {
            createLabel(null, 0, 0, 620, 180, new Image(display, is));
        } catch (IOException e) {
            throw new Error(e);
        }

        createLabel("选择服务器", 20, 200, 140, 40, null);
        new ServerList(160, 200, 450, 40);
    }

    private final class ServerList {
        private final SortedMap<Integer, Node> nodeMap = new TreeMap<>();
        private Node usingNode;
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
            setStatusLabelText("未连接");
            changeConnBtn(false);
            update();
            operator.registerProxyServerConfigListener(Config.UPDATE_EVENT, this::update, false);
            conn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if(disconnect) {
                        operator.setProxyServerUsing(usingNode, false);
                        setStatusLabelText("未连接");
                    } else {
                        Node n = selectNode();
                        if(n != null) {
                            operator.setProxyServerUsing(n, true);
                            display.timerExec(300, new Runnable() {
                                @Override
                                public void run() {
                                    if(!n.isUse()) {
                                        return;
                                    }

                                    ConnectionState cs = operator.queryProxyServerConnectionState(n);
                                    if(cs == null) {
                                        return;
                                    }

                                    switch (cs) {
                                        case NEW: setStatusLabelText("初始化中..."); break;
                                        case SSL_INITIAL: setStatusLabelText("准备SSL证书连接..."); break;
                                        case SSL_CONNECTING: setStatusLabelText("正在发起SSL证书连接..."); break;
                                        case SSL_CONNECT_TIMEOUT: setStatusLabelText("SSL证书连接超时,请检查服务器配置"); break;
                                        case SSL_CONNECT_AUTH_FAILURE: setStatusLabelText("未通过服务器认证,请检查认证信息是否正确"); break;
                                        case SSL_CONNECT: setStatusLabelText("正在获取SSL证书..."); break;
                                        case SSL_CONNECT_DONE: setStatusLabelText("SSL证书获取完成"); break;
                                        case SSL_CONNECT_ERROR: setStatusLabelText("SSL证书连接错误"); break;
                                        case PROXY_INITIAL: setStatusLabelText("准备发起代理连接..."); break;
                                        case PROXY_CONNECTING: setStatusLabelText("正在连接代理服务..."); break;
                                        case PROXY_CONNECT_TIMEOUT: setStatusLabelText("代理服务连接超时"); break;
                                        case PROXY_CONNECT: setStatusLabelText("成功与服务器建立代理服务连接"); break;
                                        case PROXY_CONNECT_AUTH_FAILURE: setStatusLabelText("代理服务认证失败,请检查认证信息是否正确"); break;
                                        case PROXY_CONNECT_ERROR: setStatusLabelText("与代理服务连接发生错误"); break;
                                        case PROXY_DISCONNECT: setStatusLabelText("暂时与服务器断开连接,尝试进行重连..."); break;
                                        case UNUSED: setStatusLabelText("代理服务器连接已停止"); break;
                                    }

                                    if(!cs.isNormal() && !cs.canRetry()) {
                                        return;
                                    }

                                    display.timerExec(cs != ConnectionState.PROXY_CONNECT ? 300 : 1000, this);
                                }
                            });
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
                    usingNode = nodes[i];
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


    private void setStatusLabelText(String text) {
        connStatusLabel.setText(text);
    }
}
