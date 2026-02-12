package com.base.seqflow.jdbc;

public interface IdSegmentRepository {

    /**
     * 查询当前最大ID
     */
    Long selectMaxId(String bizTag);

    /**
     * 原子性更新 max_id 并返回新值（MySQL 特有语法）
     */

    int incrementMaxId(String bizTag, int step);


    /**
     * 初始化一条记录（可选）
     */

    int insertInitial(String bizTag,
                      long initialMaxId,
                      int step,
                      String description);
}