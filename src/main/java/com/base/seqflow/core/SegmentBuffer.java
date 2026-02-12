package com.base.seqflow.core;

import com.base.seqflow.model.IdSegment;

public class SegmentBuffer {
    private final IdSegment[] segments = new IdSegment[2];
    private volatile int currentPos = 0;

    public SegmentBuffer(IdSegment segment) {
        this.segments[0] = segment;
        this.segments[1] = null;
    }

    public IdSegment getCurrent() {
        return segments[currentPos];
    }

    public IdSegment getNext() {
        return segments[1 - currentPos];
    }

    public void switchPos() {
        currentPos = 1 - currentPos;
    }

    public void setNext(IdSegment next) {
        segments[1 - currentPos] = next;
    }
}