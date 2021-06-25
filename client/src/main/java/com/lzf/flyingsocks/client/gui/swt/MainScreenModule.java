/*
 * Copyright (c) 2019 abc123lzf <abc123lzf@126.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import com.lzf.flyingsocks.client.gui.chart.DynamicTimeSeriesChart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.lzf.flyingsocks.client.gui.swt.Utils.adaptDPI;
import static com.lzf.flyingsocks.client.gui.swt.Utils.addButtonSelectionListener;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createButton;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createColor;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createCombo;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createLabel;
import static com.lzf.flyingsocks.client.gui.swt.Utils.createShell;
import static com.lzf.flyingsocks.client.gui.swt.Utils.i18n;
import static com.lzf.flyingsocks.client.gui.swt.Utils.loadImage;
import static com.lzf.flyingsocks.client.gui.swt.Utils.refreshCanvas;
import static com.lzf.flyingsocks.client.gui.swt.Utils.showMessageBox;
import static com.lzf.flyingsocks.client.proxy.server.ProxyServerConfig.Node;

/**
 * 主界面
 */
final class MainScreenModule extends SwtModule {

    private static final DateTimeFormatter STATUS_TEXT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /**
     * 鼠标停留按钮的颜色
     */
    private static final Color BUTTON_FOCUS_COLOR = createColor(101, 181, 255);

    /**
     * 按钮正常状态的颜色
     */
    private static final Color BUTTON_NORMAL_COLOR = createColor(81, 161, 243);


    private static final int CHART_WIDTH = 335;

    private static final int CHART_HEIGHT = 270;

    private ServerList serverList;

    /**
     * 服务器连接状态文本告示栏
     */
    private Text statusTextArea;

    /**
     * 上传速度监测图表
     */
    private DynamicTimeSeriesChart uploadChart;

    /**
     * 上传速度监测图表SWT画板
     */
    private Canvas uploadChartCanvas;

    /**
     * 下载速度监测图标
     */
    private DynamicTimeSeriesChart downloadChart;

    /**
     * 下载速度监测图标SWT画板
     */
    private Canvas downloadChartCanvas;


    MainScreenModule(SwtViewComponent component) {
        super(Objects.requireNonNull(component));
        submitChartUpdateTask();
    }

    @Override
    protected Shell buildShell() {
        Shell shell = createShell(display, "swtui.main.title", initTitleIcon(), 720, 580);
        shell.setBackground(new Color(display, 255, 255, 255));
        return shell;
    }

    @Override
    protected void initial() {
        this.statusTextArea = initStatusTextArea(shell);
        this.uploadChart = new DynamicTimeSeriesChart("上传", "", "MB/s", 60, DynamicTimeSeriesChart.STYLE_PURPLE);
        this.downloadChart = new DynamicTimeSeriesChart("下载", "", "MB/s", 60, DynamicTimeSeriesChart.STYLE_BLUE);

        this.uploadChartCanvas = initChartCanvas(this.uploadChart, 10, 270, CHART_WIDTH, CHART_HEIGHT);
        this.downloadChartCanvas = initChartCanvas(this.downloadChart, 355, 270, CHART_WIDTH, CHART_HEIGHT);

        this.serverList = initServerChooseList(shell);
        appendStatusText("swtui.main.status.not_connect");
    }

    /**
     * 初始化左上角ICON
     */
    private Image initTitleIcon() {
        try (InputStream is = ResourceManager.openIconImageStream()) {
            return loadImage(is);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * 初始化服务器选择列表
     */
    private ServerList initServerChooseList(Shell shell) {
        createLabel(shell, "swtui.main.serverlist.label", 10, 10, 160, 40, SWT.CENTER).setBackground(createColor(255, 255, 255));
        return new ServerList(180, 10, 340, 40);
    }

    /**
     * 初始化
     */
    private Text initStatusTextArea(Shell shell) {
        Text text = new Text(shell, SWT.READ_ONLY | SWT.WRAP | SWT.LEFT | SWT.V_SCROLL);
        text.setBounds(10, 60, 680, 200);
        return text;
    }


    private Canvas initChartCanvas(DynamicTimeSeriesChart chart, int x, int y, int width, int height) {
        return Utils.createCanvas(shell, chart.parseImage(adaptDPI(width), adaptDPI(height)),
                x, y, width, height);
    }

    private void submitChartUpdateTask() {
        Point downloadSize = downloadChartCanvas.getSize();
        Point uploadSize = uploadChartCanvas.getSize();
        display.timerExec(1000, new Runnable() {
            @Override
            public void run() {
                display.timerExec(1000, this);
                Node usingNode = serverList.usingNode();
                if (usingNode == null) {
                    downloadChart.appendData(0);
                    uploadChart.appendData(0);
                } else {
                    long downloadSpeed = operator.queryProxyServerDownloadThroughput(usingNode);
                    long uploadSpeed = operator.queryProxyServerUploadThroughput(usingNode);
                    downloadChart.appendData(downloadSpeed / (float)(1000 * 1000));
                    uploadChart.appendData(uploadSpeed / (float)(1000 * 1000));
                }

                if (shell.isVisible()) {
                    refreshCanvas(downloadChartCanvas, downloadChart.parseImage(downloadSize.x, downloadSize.y));
                    refreshCanvas(uploadChartCanvas, uploadChart.parseImage(uploadSize.x, uploadSize.y));
                }
            }
        });
    }


    private final class ServerList {
        private final SortedMap<Integer, Node> nodeMap = new TreeMap<>();
        private Node usingNode;
        private final Combo combo;
        private final Button connBtn;
        private boolean disconnect;

        ServerList(int x, int y, int width, int height) {
            this.combo = createCombo(shell, x, y, width, height);

            Button conn = createButton(shell, "", 530, 10, 160, 32);
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

            addButtonSelectionListener(conn, e -> {
                if (disconnect) {
                    operator.setProxyServerUsing(usingNode, false);
                } else {
                    Node n = selectNode();
                    if (n != null) {
                        operator.setProxyServerUsing(n, true);
                        operator.registerConnectionStateListener(n, (_n, cs) -> display.syncExec(() -> {
                            switch (cs) {
                                case NEW:
                                    appendStatusText("swtui.main.status.new");
                                    break;
                                case SSL_INITIAL:
                                    appendStatusText("swtui.main.status.ssl_initial");
                                    break;
                                case SSL_CONNECTING:
                                    appendStatusText("swtui.main.status.ssl_connecting");
                                    break;
                                case SSL_CONNECT_TIMEOUT:
                                    appendStatusText("swtui.main.status.ssl_connect_timeout");
                                    break;
                                case SSL_CONNECT_AUTH_FAILURE:
                                    appendStatusText("swtui.main.status.ssl_connect_auth_failure");
                                    break;
                                case SSL_CONNECT:
                                    appendStatusText("swtui.main.status.ssl_connect");
                                    break;
                                case SSL_CONNECT_DONE:
                                    appendStatusText("swtui.main.status.ssl_connect_done");
                                    break;
                                case SSL_CONNECT_ERROR:
                                    appendStatusText("swtui.main.status.ssl_connect_error");
                                    break;
                                case PROXY_INITIAL:
                                    appendStatusText("swtui.main.status.proxy_initial");
                                    break;
                                case PROXY_CONNECTING:
                                    appendStatusText("swtui.main.status.proxy_connecting");
                                    break;
                                case PROXY_CONNECT_TIMEOUT:
                                    appendStatusText("swtui.main.status.proxy_connect_timeout");
                                    break;
                                case PROXY_CONNECT:
                                    appendStatusText("swtui.main.status.proxy_connect");
                                    break;
                                case PROXY_CONNECT_AUTH_FAILURE:
                                    appendStatusText("swtui.main.status.proxy_connect_auth_failure");
                                    break;
                                case PROXY_CONNECT_ERROR:
                                    appendStatusText("swtui.main.status.proxy_connect_error");
                                    break;
                                case PROXY_DISCONNECT:
                                    appendStatusText("swtui.main.status.proxy_disconnect");
                                    break;
                                case UNUSED:
                                    appendStatusText("swtui.main.status.proxy_unused");
                                    break;
                            }
                        }));
                    } else {
                        showMessageBox(shell, "swtui.main.notice.title", "swtui.main.notice.server_not_select", SWT.ICON_INFORMATION | SWT.OK);
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
                if (i < cnt) {
                    combo.setItem(i, text);
                } else {
                    combo.add(text, i);
                }

                if (nodes[i].isUse()) {
                    combo.select(i);
                    usingNode = nodes[i];
                    use = true;
                }
            }

            changeConnBtn(use);
        }

        private void changeConnBtn(boolean disconnect) {
            if (disconnect) {
                connBtn.setText(i18n("swtui.main.button.disconnect"));
                this.disconnect = true;
            } else {
                connBtn.setText(i18n("swtui.main.button.connect"));
                this.disconnect = false;
            }
        }

        Node selectNode() {
            return nodeMap.get(combo.getSelectionIndex());
        }

        Node usingNode() {
            return usingNode;
        }
    }


    private void appendStatusText(String text) {
        StringBuilder sb = new StringBuilder(35);

        LocalTime time = LocalTime.now();
        sb.append('<').append(STATUS_TEXT_TIME_FORMAT.format(time)).append('>');

        Node selectNode = serverList.selectNode();
        sb.append('[');
        if (selectNode == null) {
            sb.append("NONE");
        } else {
            sb.append(selectNode.getHost()).append(':').append(selectNode.getPort());
        }
        sb.append("] ");

        sb.append(i18n(text));
        sb.append(Text.DELIMITER);

        String str = sb.toString();
        if (statusTextArea.getLineCount() > 5000) {
            statusTextArea.setText(str);
        } else {
            statusTextArea.append(str);
        }
    }
}
