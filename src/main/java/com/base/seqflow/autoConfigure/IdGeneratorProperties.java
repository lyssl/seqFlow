package com.base.seqflow.autoConfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "id-generator")
public class IdGeneratorProperties {


    private boolean autoInitTable = true;


    private String tableName = "sys_id_generator";

    private Integer step = 100;

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public boolean isAutoInitTable() {
        return autoInitTable;
    }

    public void setAutoInitTable(boolean autoInitTable) {
        this.autoInitTable = autoInitTable;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}