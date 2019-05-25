package com.lzf.flyingsocks;

/**
 * 组件接口，每个程序应由多个组件构成
 * @see com.lzf.flyingsocks.Lifecycle
 * @param <T> 父组件类型
 */
public interface Component<T extends Component<?>> extends Lifecycle, Named {

    /**
     * 设置组件名称
     * @param name
     */
    void setName(String name);

    /**
     * 获取父组件
     * @return
     */
    default T getParentComponent() {
        throw new UnsupportedOperationException(String.format("This component[%s] are not support to get parent component.", getName()));
    }



    /**
     * 根据模块名获取模块
     * @param moduleName 模块名
     * @param searchAtParent 如果当前组件无法找到，是否从父组件中搜寻
     * @return 模块
     */
    Module<?> getModuleByName(String moduleName, boolean searchAtParent);

    /**
     * 根据模块名称获取模块，从当前组件获取
     * @param moduleName 模块名称
     * @return 模块
     */
    default Module<?> getModuleByName(String moduleName) {
        return getModuleByName(moduleName, false);
    }

    /**
     * 根据模块名获取模块，从当前组件获取
     * @param moduleName 模块名称
     * @param requireType 模块实现类Class对象
     * @param <V> 模块实现类型
     * @return 模块对象
     */
    default <V extends Module<?>> V getModuleByName(String moduleName, Class<V> requireType) {
        return getModuleByName(moduleName, false, requireType);
    }

    /**
     * 根据模块名获取模块，从当前组件获取
     * @param moduleName 模块名称
     * @param searchAtParent 如果当前组件无法找到，是否从父组件中搜寻
     * @param requireType 模块实现类Class对象
     * @param <V> 模块实现类型
     * @return 模块对象
     */
    @SuppressWarnings("unchecked")
    default <V extends Module<?>> V getModuleByName(String moduleName, boolean searchAtParent, Class<V> requireType) {
        Module<?> m = getModuleByName(moduleName, searchAtParent);
        if(m == null)
            return null;

        if(requireType.isInstance(m))
            return (V)m;

        throw new ComponentException(new ClassCastException(String.format("Module [%s] is not type of %s.", getName(), requireType.getName())));
    }

    /**
     * 添加模块
     * @param module 模块
     */
    void addModule(Module<?> module);
}
