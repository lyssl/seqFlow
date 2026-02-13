package com.base.seqflow.autoConfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;


public class IdGeneratorDatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(IdGeneratorDatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final IdGeneratorProperties properties;

    public IdGeneratorDatabaseInitializer(JdbcTemplate jdbcTemplate, IdGeneratorProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    public void initialize() {
        if (!properties.isAutoInitTable()) {
            log.debug("Auto table initialization disabled.");
            return;
        }

        String tableName = properties.getTableName();
        if (tableExists(tableName)) {
            log.debug("Table '{}' already exists.", tableName);
            return;
        }

        createTable(tableName);
    }

    private boolean tableExists(String tableName) {
        try {
            return Boolean.TRUE.equals(jdbcTemplate.execute((java.sql.Connection connection) -> {
                DatabaseMetaData metaData = connection.getMetaData();
                try (ResultSet rs = metaData.getTables(
                        connection.getCatalog(),
                        null,
                        tableName,
                        new String[]{"TABLE"})) {
                    return rs.next();
                }
            }));
        } catch (Exception e) {
            log.warn("Failed to check table existence: {}", e.getMessage());
            return false;
        }
    }


    private void createTable(String tableName) {
        String sql = String.format("""
                CREATE TABLE IF NOT EXISTS %s (
                    biz_tag VARCHAR(64) NOT NULL PRIMARY KEY,
                    max_id BIGINT NOT NULL DEFAULT '1',
                    step INT NOT NULL DEFAULT '1000',
                    description VARCHAR(255)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """, tableName);

        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("already exists")) {
                log.info("Table '{}' already exists (concurrent creation).", tableName);
            } else {
                throw new RuntimeException("Failed to create table '" + tableName + "'", e);
            }
        }
    }
}