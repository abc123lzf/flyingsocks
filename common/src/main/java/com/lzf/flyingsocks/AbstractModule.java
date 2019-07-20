package com.lzf.flyingsocks;

/**
 * 模块抽象模板类
 * @param <T> 所属的组件类型
 */
public abstract class AbstractModule<T extends Component<?>> implements Module<T> {

    /**
     * 模块名称
     */
    private String name;

    /**
     * 所属的组件名称
     */
    private final T belongComponent;

    protected AbstractModule(T component) {
        this.name = getClass().getName();
        this.belongComponent = component;
    }

    protected AbstractModule(T component, String name) {
        this.name = name;
        this.belongComponent = component;
    }

    @Override
    public void setName(String name) {
        if(belongComponent instanceof AbstractComponent<?>) {
            ((AbstractComponent) belongComponent).changeModuleComponentName(this.name, name);
        }
        this.name = name;
    }

    @Override
    public final T getComponent() {
        return belongComponent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Module [" + name + "] from component [" + belongComponent.getName() + "]";
    }
}
