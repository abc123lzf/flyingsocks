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

/**
 * 模块抽象模板类
 *
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
    protected final T belongComponent;

    protected AbstractModule(T component) {
        this.name = getClass().getName();
        this.belongComponent = component;
    }

    protected AbstractModule(T component, String name) {
        this.name = name;
        this.belongComponent = component;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setName(String name) {
        if (belongComponent instanceof AbstractComponent<?>) {
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
