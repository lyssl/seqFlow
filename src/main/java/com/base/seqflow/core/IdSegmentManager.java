package com.base.seqflow.core;

import com.base.seqflow.autoConfigure.IdGeneratorProperties;
import com.base.seqflow.model.IdSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class IdSegmentManager {

    private static final Logger log = LoggerFactory.getLogger(IdSegmentManager.class);
    private final IdGeneratorProperties idGeneratorProperties;


    private final IdAllocator allocator;
    private final Map<String, SegmentBuffer> buffers = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public IdSegmentManager(IdAllocator allocator, IdGeneratorProperties idGeneratorProperties) {
        this.allocator = allocator;
        this.idGeneratorProperties = idGeneratorProperties;
    }

    public long nextId(String bizTag) {
        SegmentBuffer buffer = buffers.computeIfAbsent(bizTag, k -> initBuffer(k, idGeneratorProperties.getStep()));
        ReentrantLock lock = locks.computeIfAbsent(bizTag, k -> new ReentrantLock());

        while (true) {
            IdSegment currentSeg = buffer.getCurrent();
            if (currentSeg.isOver()) {
                if (!currentSeg.isLoading()) {
                    lock.lock();
                    try {
                        if (!currentSeg.isLoading()) {
                            currentSeg.setLoading(true);
                            long newMax = allocator.allocate(bizTag, idGeneratorProperties.getStep());
                            IdSegment nextSeg = new IdSegment(currentSeg.getMax(), newMax);
                            buffer.setNext(nextSeg);
                            buffer.switchPos();
                            currentSeg.setLoading(false);
                            log.info("Switched segment for {}: [{}, {})", bizTag, nextSeg.getCurrent(), nextSeg.getMax());
                        }
                    } finally {
                        lock.unlock();
                    }
                } else {
                    Thread.yield();
                    continue;
                }
            }

            synchronized (currentSeg) {
                if (currentSeg.getCurrent() < currentSeg.getMax()) {
                    long id = currentSeg.getCurrent() + 1;
                    currentSeg.setCurrent(id);
                    return id;
                }
            }
        }
    }

    private SegmentBuffer initBuffer(String bizTag, int step) {
        try {
            long maxId = allocator.allocate(bizTag, step);
            return new SegmentBuffer(new IdSegment(maxId - step, maxId));
        } catch (Exception e) {
            // 尝试初始化
            if (allocator.tryInitBizTag(bizTag, step, "Auto-created")) {
                long maxId = allocator.allocate(bizTag, step);
                return new SegmentBuffer(new IdSegment(maxId - step, maxId));
            } else {
                throw new RuntimeException("Failed to initialize biz_tag: " + bizTag, e);
            }
        }
    }
}