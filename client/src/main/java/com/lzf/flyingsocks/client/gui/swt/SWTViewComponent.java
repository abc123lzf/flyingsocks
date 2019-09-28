package com.lzf.flyingsocks.client.gui.swt;

import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.client.Client;
import org.eclipse.swt.widgets.*;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @create 2019.8.13 9:40
 * @description SWT GUI组件
 */
public class SWTViewComponent extends AbstractComponent<Client> {

    /**
     * SWT GUI线程, 必须保证是单线程
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Display display;

    private SocksSettingModule socksSettingModule;

    private ServerSettingModule serverSettingModule;

    private MainScreenModule mainScreenModule;

    public SWTViewComponent(Client parent) {
        super("SWTViewComponent", Objects.requireNonNull(parent));

        Callable<Display> swtBuilder = Display::new;
        try {
            this.display = executor.submit(swtBuilder).get();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    protected void initInternal() {
        executor.submit(() -> {
            try {
                addModule(new TrayModule(this, display));
                addModule(this.serverSettingModule = new ServerSettingModule(this, display));
                addModule(this.socksSettingModule = new SocksSettingModule(this, display));
                addModule(this.mainScreenModule = new MainScreenModule(this, display));
            } catch (Throwable t) {
                log.error("SWT Thread occur a error", t);
                System.exit(1);
            }
        });
    }

    @Override
    protected void startInternal() {
        executor.submit(() -> {
            try {
                Thread t = Thread.currentThread();
                while (!t.isInterrupted()) {
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
                display.dispose();
            } catch (RuntimeException | Error t) {
                log.error("SWT Thread occur a error", t);
                System.exit(1);
            }
        });
    }

    @Override
    protected void stopInternal() {
        executor.shutdownNow();
    }

    void openSocksSettingUI() {
        socksSettingModule.setVisiable(true);
    }

    void openServerSettingUI() {
        serverSettingModule.setVisiable(true);
    }

    void openMainScreenUI() {
        mainScreenModule.setVisiable(true);
    }

    @Override
    protected void restartInternal() {
        throw new UnsupportedOperationException("Can not restart SWT View Component");
    }
}
