package com.lzf.flyingsocks.util;

import java.util.Set;

public interface ReturnableSet<E> extends Set<E> {

    E getIfContains(Object object);

}
