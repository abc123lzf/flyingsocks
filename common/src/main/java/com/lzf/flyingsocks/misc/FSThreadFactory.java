package com.lzf.flyingsocks.misc;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author : denglinhai
 * @date : 21:43 2022/2/20
 */
public class FSThreadFactory implements ThreadFactory {
    // 名称前缀
    private String namePrefix;
    // 线程计数器
    private final AtomicLong counter;

    public FSThreadFactory() {
        counter = new AtomicLong();
    }

    @Override
    public Thread newThread(Runnable r) {
        ThreadFactory backThreadFactory = Executors.defaultThreadFactory();
        Thread newThread = backThreadFactory.newThread(r);
        newThread.setName(namePrefix + counter.getAndIncrement());
        return newThread;
    }

    public ThreadFactory setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
        return this;
    }
}
