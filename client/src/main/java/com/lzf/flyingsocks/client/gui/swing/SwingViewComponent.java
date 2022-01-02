package com.lzf.flyingsocks.client.gui.swing;

import com.bulenkov.darcula.DarculaLaf;
import com.lzf.flyingsocks.AbstractComponent;
import com.lzf.flyingsocks.Lifecycle;
import com.lzf.flyingsocks.LifecycleEvent;
import com.lzf.flyingsocks.LifecycleEventListener;
import com.lzf.flyingsocks.LifecycleState;
import com.lzf.flyingsocks.client.Client;
import com.lzf.flyingsocks.client.gui.swing.localproxy.LocalProxyConfigureModule;
import com.lzf.flyingsocks.client.gui.swing.serverconfigure.ServerConfigureModule;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * @author lizifan 695199262@qq.com
 * @since 2021.8.21 23:27
 */
public class SwingViewComponent extends AbstractComponent<Client> {

    private final ScheduledExecutorService scheduledService = Executors.newScheduledThreadPool(1);

    public SwingViewComponent(Client parent) {
        super("SwingViewComponent", parent);
    }

    @Override
    protected void initInternal() {
        syncInvoke(() -> {
            try {
                UIManager.setLookAndFeel(new DarculaLaf());
            } catch (Exception e) {
                throw new Error(e);
            }

            addModule(new SystemTrayModule(this));
            addModule(new LocalProxyConfigureModule(this));
            addModule(new ServerConfigureModule(this));
        });

        super.initInternal();
    }

    @Override
    protected void startInternal() {
        super.startInternal();
    }

    @Override
    protected void stopInternal() {
        scheduledService.shutdownNow();
        super.stopInternal();
    }

    public void asyncInvoke(Runnable runnable) {
        Objects.requireNonNull(runnable);
        SwingUtilities.invokeLater(runnable);
    }


    public void syncInvoke(Runnable runnable) {
        Objects.requireNonNull(runnable);
        try {
            SwingUtilities.invokeAndWait(runnable);
        } catch (InterruptedException | InvocationTargetException e) {
            log.warn("Swing thread exception", e);
        }
    }

    public void scheduledAtFixedRate(Runnable runnable, long rate, TimeUnit timeUnit) {
        Runnable task = () -> SwingUtilities.invokeLater(runnable);
        if (getState() == LifecycleState.STARTED) {
            scheduledService.scheduleAtFixedRate(task, 0, rate, timeUnit);
        } else if (getState().after(LifecycleState.STARTED)) {
            addLifecycleEventListener(new LifecycleEventListener() {
                @Override
                public void lifecycleEvent(LifecycleEvent event) {
                    if (Objects.equals(event.getType(), Lifecycle.AFTER_START_EVENT)) {
                        scheduledService.scheduleAtFixedRate(task, 0, rate, timeUnit);
                        removeLifecycleEventListener(this);
                    }
                }
            });
        } else {
            throw new IllegalStateException();
        }
    }
}
