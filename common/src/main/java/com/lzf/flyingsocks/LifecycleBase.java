package com.lzf.flyingsocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public abstract class LifecycleBase implements Lifecycle {

    private volatile LifecycleState state = LifecycleState.NEW;

    private final List<LifecycleEventListener> listeners = new CopyOnWriteArrayList<>();

    protected LifecycleBase() { }

    protected LifecycleBase(LifecycleEventListener... listeners) {
        List<LifecycleEventListener> l = new ArrayList<>();
        Collections.addAll(l, listeners);
        l.removeAll(Collections.singleton(null));
        this.listeners.addAll(l);
    }

    @Override
    public synchronized final void init() throws LifecycleException {
        if(state.after(LifecycleState.INITIALIZED))
            throw new LifecycleException();
        state = LifecycleState.INITIALIZING;
        fireLifecycleEvent(BEFORE_INIT_EVENT, this);
        initInternal();
        fireLifecycleEvent(AFTER_INIT_EVENT, this);
        state = LifecycleState.INITIALIZED;
    }

    protected void initInternal() { }

    @Override
    public synchronized final void start() throws LifecycleException {
        if(state.after(LifecycleState.STARTING))
            throw new LifecycleException();
        state = LifecycleState.STARTING;
        fireLifecycleEvent(BEFORE_START_EVENT, this);
        startInternal();
        fireLifecycleEvent(AFTER_START_EVENT, this);
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
        state = LifecycleState.STOPING;
        fireLifecycleEvent(BEFORE_STOP_EVENT, this);
        stopInternal();
        fireLifecycleEvent(AFTER_STOP_EVENT, this);
        listeners.clear();
    }

    /**
     * 组件停止过程细节，由子类重写
     */
    protected void stopInternal() { }

    @Override
    public synchronized final void restart() throws LifecycleException {
        state = LifecycleState.RESTARTING;
        fireLifecycleEvent(BEFORE_RESTART_EVENT, this);
        restartInternal();
        fireLifecycleEvent(AFTER_RESTART_EVENT, this);
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
        listeners.remove(listener);
    }

    private void fireLifecycleEvent(String event, Object data) {
        LifecycleEvent eventObj = new LifecycleEvent(this, event, data);
        for(LifecycleEventListener listener : listeners) {
            listener.lifecycleEvent(eventObj);
        }
    }
}
