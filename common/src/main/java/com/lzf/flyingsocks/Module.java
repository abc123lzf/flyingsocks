package com.lzf.flyingsocks;

/**
 * 表示组件中的模块，相当于微型的组件
 *
 * @param <T> 组件类型
 * @see com.lzf.flyingsocks.Named
 * @see com.lzf.flyingsocks.Component
 * @see com.lzf.flyingsocks.AbstractComponent
 */
public interface Module<T extends Component<?>> extends Named {

    /**
     * 设置模块名称
     *
     * @param name 模块名称
     */
    void setName(String name);

    /**
     * 获取这个模块被哪个组件持有
     *
     * @return 组件对象
     */
    T getComponent();

}
