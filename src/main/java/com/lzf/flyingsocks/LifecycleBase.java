package com.lzf.flyingsocks;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class LifecycleBase implements Lifecycle {

    private LifecycleState state = LifecycleState.NEW;

    private final List<LifecycleEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public synchronized final void init() throws LifecycleException {
        if(state.after(LifecycleState.INITIALIZED))
            throw new LifecycleException();
        state = LifecycleState.INITIALIZING;
        fireLifecycleEvent(BEFORE_INIT_EVENT, null);
        initInternal();
        fireLifecycleEvent(AFTER_INIT_EVENT, null);
        state = LifecycleState.INITIALIZED;
    }

    protected void initInternal() { }

    @Override
    public synchronized final void start() throws LifecycleException {
        if(state.after(LifecycleState.STARTING))
            throw new LifecycleException();
        state = LifecycleState.STARTING;
        fireLifecycleEvent(BEFORE_START_EVENT, null);
        startInternal();
        fireLifecycleEvent(AFTER_START_EVENT, null);
        state = LifecycleState.STARTED;
    }

    /**
     * 组件启动过程细节，由子类重写
     */
    protected void startInternal() { }

    @Override
    public synchronized final void stop() throws LifecycleException {
        if(state.after(LifecycleState.STOPING))
            throw new LifecycleException();
        state = LifecycleState.STARTING;
        fireLifecycleEvent(BEFORE_STOP_EVENT, null);
        stopInternal();
        fireLifecycleEvent(AFTER_STOP_EVENT, null);
        listeners.clear();
    }

    /**
     * 组件停止过程细节，由子类重写
     */
    protected void stopInternal() { }

    @Override
    public synchronized final void restart() throws LifecycleException {
        state = LifecycleState.RESTARTING;
        fireLifecycleEvent(BEFORE_RESTART_EVENT, null);
        restartInternal();
        fireLifecycleEvent(AFTER_RESTART_EVENT, null);
    }

    /**
     * 组件重新启动过程细节，可由子类重写
     */
    protected void restartInternal() {
        if(state == LifecycleState.STARTED) {
            stop();
        }
        init();
        start();
    }

    @Override
    public final LifecycleState getState() {
        return state;
    }

    @Override
    public final void addLifecycleEventListener(LifecycleEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public final void removeLifecycleEventListener(LifecycleEventListener listener) {
        int i = 0;
        for(LifecycleEventListener eventListener : listeners) {
            if(eventListener == listener || eventListener.equals(listener))
                listeners.remove(i);
            i++;
        }
    }

    protected final void fireLifecycleEvent(String event, Object data) {
        LifecycleEvent eventObj = new LifecycleEvent(this, event, data);
        for(LifecycleEventListener listener : listeners) {
            listener.lifecycleEvent(eventObj);
        }
    }

}
