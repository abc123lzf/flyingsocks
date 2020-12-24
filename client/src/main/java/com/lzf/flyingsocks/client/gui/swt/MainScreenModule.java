package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractModule;
import com.lzf.flyingsocks.Config;
import com.lzf.flyingsocks.client.ClientOperator;
import com.lzf.flyingsocks.client.gui.ResourceManager;
import com.lzf.flyingsocks.client.gui.chart.DynamicTimeSeriesChart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
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
import static com.lzf.flyingsocks.client.gui.swt.Utils.loadImage;
import static com.lzf.flyingsocks.client.gui.swt.Utils.refreshCanvas;
import static com.lzf.flyingsocks.client.gui.swt.Utils.showMessageBox;
import static com.lzf.flyingsocks.client.proxy.ProxyServerConfig.Node;

/**
 * 主界面
 */
final class MainScreenModule extends AbstractModule<SWTViewComponent> {

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


    private final Display display;

    private final Shell shell;

    private final ClientOperator operator;

    private final ServerList serverList;

    /**
     * 服务器连接状态文本告示栏
     */
    private final Text statusTextArea;

    /**
     * 上传速度监测图表
     */
    private final DynamicTimeSeriesChart uploadChart;

    /**
     * 上传速度监测图表SWT画板
     */
    private final Canvas uploadChartCanvas;

    /**
     * 下载速度监测图标
     */
    private final DynamicTimeSeriesChart downloadChart;

    /**
     * 下载速度监测图标SWT画板
     */
    private final Canvas downloadChartCanvas;


    MainScreenModule(SWTViewComponent component, Display display) {
        super(Objects.requireNonNull(component), "Main-Screen");
        this.display = display;
        this.operator = getComponent().getParentComponent();

        Shell shell = createShell(display, "主界面", initTitleIcon(), 700, 580);
        shell.setBackground(new Color(display, 255, 255, 255));
        this.shell = shell;

        this.statusTextArea = initStatusTextArea(shell);
        this.uploadChart = new DynamicTimeSeriesChart("UPLOAD", "", "MB/s", 60, DynamicTimeSeriesChart.STYLE_PURPLE);
        this.downloadChart = new DynamicTimeSeriesChart("DOWNLOAD", "", "MB/s", 60, DynamicTimeSeriesChart.STYLE_BLUE);


        this.uploadChartCanvas = initChartCanvas(this.uploadChart, 10, 270, CHART_WIDTH, CHART_HEIGHT);
        this.downloadChartCanvas = initChartCanvas(this.downloadChart, 355, 270, CHART_WIDTH, CHART_HEIGHT);

        this.serverList = initServerChooseList(shell);
        adaptDPI(shell);
        setVisiable(false);
        submitChartUpdateTask();
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
        createLabel(shell, "选择服务器", 10, 10, 160, 40, SWT.CENTER).setBackground(createColor(255, 255, 255));
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
                    refreshCanvas(downloadChartCanvas, downloadChart.parseImage(adaptDPI(CHART_WIDTH), adaptDPI(CHART_HEIGHT)));
                    refreshCanvas(uploadChartCanvas, uploadChart.parseImage(adaptDPI(CHART_WIDTH), adaptDPI(CHART_HEIGHT)));
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
            appendStatusText("未连接");
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
                                    appendStatusText("初始化中...");
                                    break;
                                case SSL_INITIAL:
                                    appendStatusText("准备SSL证书连接...");
                                    break;
                                case SSL_CONNECTING:
                                    appendStatusText("正在发起SSL证书连接...");
                                    break;
                                case SSL_CONNECT_TIMEOUT:
                                    appendStatusText("SSL证书连接超时,请检查服务器配置");
                                    break;
                                case SSL_CONNECT_AUTH_FAILURE:
                                    appendStatusText("未通过服务器认证,请检查认证信息是否正确");
                                    break;
                                case SSL_CONNECT:
                                    appendStatusText("正在获取SSL证书...");
                                    break;
                                case SSL_CONNECT_DONE:
                                    appendStatusText("SSL证书获取完成");
                                    break;
                                case SSL_CONNECT_ERROR:
                                    appendStatusText("SSL证书连接错误");
                                    break;
                                case PROXY_INITIAL:
                                    appendStatusText("准备发起代理连接...");
                                    break;
                                case PROXY_CONNECTING:
                                    appendStatusText("正在连接代理服务...");
                                    break;
                                case PROXY_CONNECT_TIMEOUT:
                                    appendStatusText("代理服务连接超时");
                                    break;
                                case PROXY_CONNECT:
                                    appendStatusText("成功与服务器建立代理服务连接");
                                    break;
                                case PROXY_CONNECT_AUTH_FAILURE:
                                    appendStatusText("代理服务认证失败,请检查认证信息是否正确");
                                    break;
                                case PROXY_CONNECT_ERROR:
                                    appendStatusText("与代理服务连接发生错误");
                                    break;
                                case PROXY_DISCONNECT:
                                    appendStatusText("暂时与服务器断开连接,尝试进行重连...");
                                    break;
                                case UNUSED:
                                    appendStatusText("代理服务器连接已停止");
                                    break;
                            }
                        }));
                    } else {
                        showMessageBox(shell, "提示", "请选择一个有效的服务器", SWT.ICON_INFORMATION | SWT.OK);
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

        Node usingNode() {
            return usingNode;
        }
    }


    void setVisiable(boolean visible) {
        shell.setVisible(visible);
    }


    private void appendStatusText(String text) {
        LocalTime time = LocalTime.now();
        String str = "【" + time.toString() + "】" + text + Text.DELIMITER;
        if (statusTextArea.getLineCount() > 5000) {
            statusTextArea.setText(str);
        } else {
            statusTextArea.append(str);
        }
    }
}
