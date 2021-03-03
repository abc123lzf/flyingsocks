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

import com.lzf.flyingsocks.misc.LifecycleLoggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * 组件抽象模板类，实现了Component接口和Lifecycle接口
 *
 * @param <T> 父组件类型
 * @see com.lzf.flyingsocks.Component
 * @see com.lzf.flyingsocks.Lifecycle
 * @see com.lzf.flyingsocks.LifecycleBase
 */
public abstract class AbstractComponent<T extends Component<?>> extends LifecycleBase implements Component<T> {

    /**
     * Slf4j日志
     */
    protected final Logger log;

    /**
     * 父组件引用
     */
    protected final T parent;

    /**
     * 该组件的名称
     */
    protected String name;

    /**
     * 该组件持有的模块，键为该模块的名称
     */
    private final Map<String, Module<?>> moduleMap = new ConcurrentHashMap<>();

    /**
     * 该组件持有的子组件，键为该组件的名称
     */
    private final Map<String, Component<?>> componentMap = new ConcurrentSkipListMap<>();

    protected AbstractComponent() {
        super(LifecycleLoggerListener.INSTANCE);
        this.parent = null;
        this.name = getClass().getName();
        log = LoggerFactory.getLogger(this.name);
    }

    /**
     * 构造组件
     *
     * @param name   组件名
     * @param parent 父组件，如果没有父组件则泛型参数T需为VoidComponent并且值为null
     */
    protected AbstractComponent(String name, T parent) {
        super(LifecycleLoggerListener.INSTANCE);
        this.name = Objects.requireNonNull(name);
        this.parent = parent;
        log = LoggerFactory.getLogger(name);
    }

    protected AbstractComponent(String name) {
        this(name, null);
    }

    @Override
    public void setName(String name) {
        if (parent instanceof AbstractComponent<?>) {
            AbstractComponent<?> c = (AbstractComponent<?>) parent;
            c.changeChildComponentName(this.name, name);
        }
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T getParentComponent() {
        return parent;
    }

    @Override
    public final Module<?> getModuleByName(String moduleName, boolean searchAtParent) {
        Module<?> m = moduleMap.get(moduleName);
        if (searchAtParent && m == null) {
            try {
                return getParentComponent().getModuleByName(moduleName, true);
            } catch (Exception e) {
                return null;
            }
        }
        return m;
    }

    @Override
    public void addModule(Module<?> module) {
        String name;
        if (moduleMap.get(name = module.getName()) != null) {
            throw new ComponentException(String.format("Component [%s] already has module [%s].", getName(), name));
        }
        moduleMap.put(name, module);
    }

    /**
     * 添加子组件
     *
     * @param component 组件对象
     */
    protected synchronized void addComponent(Component<?> component) {
        String name = component.getName();
        if (name == null) {
            throw new ComponentException(String.format("Component type [%s] name can not be null.", component.getClass().getName()));
        }
        if (componentMap.get(name) != null) {
            throw new ComponentException(String.format("Component [%s] already has child component [%s].", getName(), name));
        }

        componentMap.put(name, component);
    }

    /**
     * 根据组件名称获取组件
     *
     * @param name 组件名
     * @return 组件，若没有找到返回null
     */
    protected Component<?> getComponentByName(String name) {
        return componentMap.get(name);
    }

    @SuppressWarnings("unchecked")
    protected <V extends Component<?>> V getComponentByName(String name, Class<V> requireType) {
        Component<?> c = componentMap.get(name);
        if (c == null) {
            return null;
        }

        if (!requireType.isInstance(c)) {
            throw new ComponentException(new ClassCastException(String.format("Component [%s] is not type of %s.", getName(), requireType.getName())));
        }

        return (V) c;
    }


    /**
     * 移除组件
     *
     * @param name 组件名
     */
    protected void removeComponentByName(String name) {
        componentMap.remove(name);
    }


    @Override
    protected void initInternal() {
        synchronized (componentMap) {
            for (Map.Entry<String, Component<?>> entry : componentMap.entrySet()) {
                try {
                    entry.getValue().init();
                } catch (Exception e) {
                    log.error(String.format("Component [%s] init failure.", entry.getKey()), e);
                }
            }
        }
    }

    @Override
    protected void startInternal() {
        synchronized (componentMap) {
            for (Map.Entry<String, Component<?>> entry : componentMap.entrySet()) {
                try {
                    entry.getValue().start();
                } catch (Exception e) {
                    log.error(String.format("Component [%s] start failure.", entry.getKey()), e);
                }
            }
        }
    }

    @Override
    protected void stopInternal() {
        synchronized (componentMap) {
            for (Map.Entry<String, Component<?>> entry : componentMap.entrySet()) {
                try {
                    entry.getValue().stop();
                } catch (Exception e) {
                    log.error(String.format("Component [%s] stop failure.", entry.getKey()), e);
                }
            }
        }
    }

    @Override
    protected void restartInternal() {
        synchronized (componentMap) {
            for (Map.Entry<String, Component<?>> entry : componentMap.entrySet()) {
                try {
                    entry.getValue().restart();
                } catch (Exception e) {
                    log.error(String.format("Component [%s] restart failure.", entry.getKey()), e);
                }
            }
        }

        super.restartInternal();
    }

    /**
     * 当子模块需要修改名称时由子模块调用
     *
     * @param oldName 子模块旧名称
     * @param newName 子模块新名称
     */
    void changeModuleComponentName(String oldName, String newName) {
        Module<?> c = moduleMap.get(oldName);
        if (c == null) {
            throw new ComponentException("Can not find module: " + oldName + " at " + name);
        }
        if (moduleMap.containsKey(newName)) {
            throw new ComponentException("Module name: " + newName + " is already in " + name);
        }
        moduleMap.remove(oldName);
        moduleMap.put(newName, c);
    }

    /**
     * 当子组件需要修改名称时由子组件调用
     *
     * @param oldName 子组件旧名称
     * @param newName 子组件新名称
     */
    private void changeChildComponentName(String oldName, String newName) {
        Component<?> c = componentMap.get(oldName);
        if (c == null) {
            throw new ComponentException("Can not find component: " + oldName + " at " + name);
        }
        if (componentMap.containsKey(newName)) {
            throw new ComponentException("Component name: " + newName + " is already in " + name);
        }

        componentMap.remove(oldName);
        componentMap.put(newName, c);
    }


    /**
     * @return 配置管理器
     */
    protected ConfigManager<?> getConfigManager() {
        Module<?> module = getModuleByName(DefaultConfigManager.NAME, true);
        if (module instanceof ConfigManager) {
            return (ConfigManager<?>) module;
        } else {
            return null;
        }
    }
}
