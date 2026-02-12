package com.base.seqflow.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class JdbcIdSegmentRepository implements IdSegmentRepository {

    private final JdbcTemplate jdbcTemplate;
    private String tableName = "sys_id_generator";  // 改为你的表名

    public JdbcIdSegmentRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public JdbcIdSegmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public Long selectMaxId(String bizTag) {
        String sql = "SELECT max_id FROM " + tableName + " WHERE biz_tag = ?";
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, bizTag);
        } catch (Exception e) {
            return null;  // 记录不存在返回null
        }
    }

    @Override
    public int incrementMaxId(String bizTag, int step) {
        String sql = "UPDATE " + tableName +
                " SET max_id = max_id + ? " +
                " WHERE biz_tag = ?";
        return jdbcTemplate.update(sql, step, bizTag);
    }

    @Override
    public int insertInitial(String bizTag, long initialMaxId, int step, String description) {
        String sql = "INSERT INTO " + tableName +
                " (biz_tag, max_id, step, description) " +
                "VALUES (?, ?, ?, ?)";
        return jdbcTemplate.update(sql, bizTag, 0, step, description);
    }

    /**
     * 建表语句
     */
    public static String getCreateTableDDL() {
        return "CREATE TABLE IF NOT EXISTS `sys_id_generator` (" +
                "  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键'," +
                "  `biz_tag` varchar(64) NOT NULL COMMENT '业务标识'," +
                "  `max_id` bigint(20) NOT NULL COMMENT '当前最大ID'," +
                "  `step` int(11) NOT NULL COMMENT '步长'," +
                "  `description` varchar(255) DEFAULT NULL COMMENT '描述'," +
                "  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "  PRIMARY KEY (`id`)," +
                "  UNIQUE KEY `uk_biz_tag` (`biz_tag`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ID发号器表';";
    }
}