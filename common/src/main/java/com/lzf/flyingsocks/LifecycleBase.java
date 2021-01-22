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
package com.lzf.flyingsocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public abstract class LifecycleBase implements Lifecycle {

    private volatile LifecycleState state = LifecycleState.NEW;

    private final List<LifecycleEventListener> listeners = new CopyOnWriteArrayList<>();

    protected LifecycleBase() {
    }

    protected LifecycleBase(LifecycleEventListener... listeners) {
        List<LifecycleEventListener> l = new ArrayList<>();
        Collections.addAll(l, listeners);
        l.removeAll(Collections.singleton(null));
        this.listeners.addAll(l);
    }

    @Override
    public synchronized final void init() throws LifecycleException {
        if (state.after(LifecycleState.INITIALIZED)) {
            throw new LifecycleException();
        }

        state = LifecycleState.INITIALIZING;
        fireLifecycleEvent(BEFORE_INIT_EVENT, this);
        initInternal();
        fireLifecycleEvent(AFTER_INIT_EVENT, this);
        state = LifecycleState.INITIALIZED;
    }

    protected void initInternal() {
    }

    @Override
    public synchronized final void start() throws LifecycleException {
        if (state.after(LifecycleState.STARTING)) {
            throw new LifecycleException();
        }

        state = LifecycleState.STARTING;
        fireLifecycleEvent(BEFORE_START_EVENT, this);
        startInternal();
        fireLifecycleEvent(AFTER_START_EVENT, this);
        state = LifecycleState.STARTED;
    }

    /**
     * 组件启动过程细节，由子类重写
     */
    protected void startInternal() {
    }

    @Override
    public synchronized final void stop() throws LifecycleException {
        if (state.after(LifecycleState.STOPING)) {
            throw new LifecycleException();
        }

        state = LifecycleState.STOPING;
        fireLifecycleEvent(BEFORE_STOP_EVENT, this);
        stopInternal();
        fireLifecycleEvent(AFTER_STOP_EVENT, this);
        listeners.clear();
    }

    /**
     * 组件停止过程细节，由子类重写
     */
    protected void stopInternal() {
    }

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
        if (state == LifecycleState.STARTED) {
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
        for (LifecycleEventListener listener : listeners) {
            listener.lifecycleEvent(eventObj);
        }
    }
}
