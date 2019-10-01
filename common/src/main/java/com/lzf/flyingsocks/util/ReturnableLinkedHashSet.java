package com.lzf.flyingsocks.util;


import java.util.*;

public class ReturnableLinkedHashSet<E> extends AbstractSet<E> implements ReturnableSet<E> {

    private final LinkedHashMap<E, E> map;

    public ReturnableLinkedHashSet() {
        map = new LinkedHashMap<>();
    }

    public ReturnableLinkedHashSet(int size) {
        map = new LinkedHashMap<>(size);
    }

    public ReturnableLinkedHashSet(int size, float loadFactor) {
        map = new LinkedHashMap<>(size, loadFactor);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return map.keySet().toArray(a);
    }

    @Override
    public boolean add(E e) {
        return map.put(e, e) != null;
    }

    @Override
    public E getIfContains(Object object) {
        return map.get(object);
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return map.keySet().containsAll(c);
    }

    @Override
    public void clear() {
        map.clear();
    }
}
