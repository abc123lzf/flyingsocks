package com.lzf.flyingsocks;

public final class VoidComponent extends AbstractComponent<VoidComponent> {

    private VoidComponent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VoidComponent getParentComponent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void initInternal() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void startInternal() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void stopInternal() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void restartInternal() {
        throw new UnsupportedOperationException();
    }
}
