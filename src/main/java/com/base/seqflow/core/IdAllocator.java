package com.base.seqflow.core;

import com.base.seqflow.jdbc.IdSegmentRepository;

public class IdAllocator {

    private final IdSegmentRepository mapper;

    public IdAllocator(IdSegmentRepository mapper) {
        this.mapper = mapper;
    }

    public long allocate(String bizTag, int step) {

        int updated = mapper.incrementMaxId(bizTag, step);
        if (updated == 0) {
            throw new RuntimeException("biz_tag not found: " + bizTag + ". Please initialize it in DB.");
        }

        Long newMax = mapper.selectMaxId(bizTag);
        if (newMax == null) {
            throw new RuntimeException("Failed to load max_id after update for: " + bizTag);
        }
        return newMax;
    }

    //自动初始化 bizTag（用于懒创建）
    public boolean tryInitBizTag(String bizTag, int step, String description) {
        try {
            return mapper.insertInitial(bizTag, 0, step, description) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}