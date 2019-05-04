package com.lzf.flyingsocks;

public abstract class AbstractModule<T extends Component<?>> implements Module<T> {

    private String name;

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
