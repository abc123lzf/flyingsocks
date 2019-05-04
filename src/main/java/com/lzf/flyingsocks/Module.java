package com.lzf.flyingsocks;

public interface Module<T extends Component<?>> extends Named {

    void setName(String name);

    T getComponent();

}
