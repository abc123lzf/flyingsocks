package com.lzf.flyingsocks;

import com.lzf.flyingsocks.util.LifecycleLoggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public abstract class AbstractComponent<T extends Component<?>> extends LifecycleBase implements Component<T> {
    protected final Logger log = LoggerFactory.getLogger(getClass().getSimpleName());

    protected final T parent;

    protected String name;

    private final Map<String, Module<?>> moduleMap = new ConcurrentHashMap<>();

    private final Map<String, Component<?>> componentMap = new ConcurrentSkipListMap<>();

    protected AbstractComponent() {
        super();
        this.parent = null;
        this.name = getClass().getName();

        if(LifecycleLoggerListener.INSTANCE != null)
            addLifecycleEventListener(LifecycleLoggerListener.INSTANCE);
    }

    protected AbstractComponent(String name, T parent) {
        super();
        this.name = Objects.requireNonNull(name);
        this.parent = parent;

        if(LifecycleLoggerListener.INSTANCE != null)
            addLifecycleEventListener(LifecycleLoggerListener.INSTANCE);
    }

    protected AbstractComponent(String name) {
        this(name, null);
    }

    @Override
    public void setName(String name) {
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
    public final Module getModuleByName(String moduleName, boolean searchAtParent) {
        Module<?> m = moduleMap.get(moduleName);
        if(searchAtParent && m == null) {
            try {
                return getParentComponent().getModuleByName(moduleName, true);
            } catch (Exception e) {
                return null;
            }
        }
        return m;
    }

    @Override
    public void addModule(Module module) {
        String name;
        if(moduleMap.get(name = module.getName()) != null) {
            throw new ComponentException(String.format("Component [%s] already has module [%s].", getName(), name));
        }
        moduleMap.put(name, module);
    }

    /**
     * 添加子组件
     * @param component 组件对象
     */
    protected synchronized void addComponent(Component<?> component) {
        LifecycleState state = getState();
        if(state.after(LifecycleState.INITIALIZED))
            throw new ComponentException(String.format("Component [%s] can not add component when component is new.", getName()));

        String name = component.getName();
        if(name == null)
            throw new ComponentException(String.format("Component type [%s] name can not be null.", component.getClass().getName()));
        if(componentMap.get(name) != null)
            throw new ComponentException(String.format("Component [%s] already has child component [%s].", getName(), name));

        componentMap.put(name, component);
    }

    /**
     * 根据组件名称获取组件
     * @param name 组件名
     * @return 组件，若没有找到返回null
     */
    protected Component<?> getComponentByName(String name) {
        return componentMap.get(name);
    }

    @SuppressWarnings("unchecked")
    protected <V extends Component<T>> V getComponentByName(String name, Class<V> requireType) {
        Component<?> c = componentMap.get(name);
        if(c == null)
            return null;

        if(!requireType.isInstance(c))
            throw new ComponentException(new ClassCastException(String.format("Component [%s] is not type of %s.", getName(), requireType.getName())));

        return (V)c;
    }

    /**
     * 移除组件
     * @param name 组件名
     */
    protected void removeComponentByName(String name) {
        componentMap.remove(name);
    }


    @Override
    protected void initInternal() {
        synchronized (componentMap) {
            for(Map.Entry<String, Component<?>> entry : componentMap.entrySet()) {
                try {
                    entry.getValue().init();
                } catch (LifecycleException | ComponentException e) {
                    log.error(String.format("Component [%s] init failure.", entry.getKey()), e);
                }
            }
        }
    }

    @Override
    protected void startInternal() {
        synchronized (componentMap) {
            for(Map.Entry<String, Component<?>> entry : componentMap.entrySet()) {
                try {
                    entry.getValue().start();
                } catch (LifecycleException | ComponentException e) {
                    log.error(String.format("Component [%s] start failure.", entry.getKey()), e);
                }
            }
        }
    }

    @Override
    protected void stopInternal() {
        synchronized (componentMap) {
            for(Map.Entry<String, Component<?>> entry : componentMap.entrySet()) {
                try {
                    entry.getValue().stop();
                } catch (LifecycleException | ComponentException e) {
                    log.error(String.format("Component [%s] stop failure.", entry.getKey()), e);
                }
            }
        }
    }

    @Override
    protected void restartInternal() {
        synchronized (componentMap) {
            for(Map.Entry<String, Component<?>> entry : componentMap.entrySet()) {
                try {
                    entry.getValue().restart();
                } catch (LifecycleException | ComponentException e) {
                    log.error(String.format("Component [%s] restart failure.", entry.getKey()), e);
                }
            }
        }

        super.restartInternal();
    }
}
