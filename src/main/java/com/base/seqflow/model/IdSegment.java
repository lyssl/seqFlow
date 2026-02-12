package com.base.seqflow.model;

public class IdSegment {
    private long current;
    private final long max;
    private volatile boolean loading = false;

    public IdSegment(long current, long max) {
        this.current = current;
        this.max = max;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getMax() {
        return max;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public boolean isOver() {
        return current >= max;
    }
}