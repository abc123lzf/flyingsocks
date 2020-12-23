package com.lzf.flyingsocks;

public enum LifecycleState {
    NEW(0),
    INITIALIZING(1),
    INITIALIZED(2),
    STARTING(3),
    STARTED(4),
    STOPING(5),
    STOPED(6),
    RESTARTING(-1);

    private final int step;

    private LifecycleState(int step) {
        this.step = step;
    }

    /**
     * @param state 指定状态枚举
     * @return 状态是否在指定state后
     */
    public boolean after(LifecycleState state) {
        if (this.step == -1) {
            if (state.step >= 4)
                return false;
            return true;
        }
        if (this.step >= state.step)
            return true;
        return false;
    }
}
