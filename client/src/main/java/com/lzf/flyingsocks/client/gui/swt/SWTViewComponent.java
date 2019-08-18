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
     * SWT GUI线程
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();


    private final Display display;


    public SWTViewComponent(Client parent) {
        super("SWTViewComponent", Objects.requireNonNull(parent));
        try {
            this.display = executor.submit(new Callable<Display>() {
                @Override
                public Display call() {
                    return new Display();
                }
            }).get();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    protected void initInternal() {
        executor.submit(() -> {
            addModule(new TrayModule(this, display));
            addModule(new ServerSettingModule(this, display));
        });

    }

    @Override
    protected void startInternal() {
        executor.submit(() -> {
            Thread t = Thread.currentThread();
            while (!t.isInterrupted()) {
                if(!display.readAndDispatch()) {
                    display.sleep();
                }
            }

            display.dispose();
        });
    }

    @Override
    protected void stopInternal() {
        executor.shutdownNow();
    }

    @Override
    protected void restartInternal() {
        throw new UnsupportedOperationException("Can not restart SWT View Component");
    }
}
