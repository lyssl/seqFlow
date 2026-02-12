package com.base.seqflow.autoConfigure;


import com.base.seqflow.core.IdAllocator;
import com.base.seqflow.core.IdSegmentManager;
import com.base.seqflow.jdbc.IdSegmentRepository;
import com.base.seqflow.jdbc.JdbcIdSegmentRepository;


import com.base.seqflow.core.SeqFlow;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;


@EnableConfigurationProperties(IdGeneratorProperties.class)
@ConditionalOnClass({ IdSegmentRepository.class})
public class IdGeneratorAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public IdSegmentRepository idSegmentRepository(DataSource dataSource,IdGeneratorProperties idGeneratorProperties) {
        JdbcIdSegmentRepository jdbcIdSegmentRepository = new JdbcIdSegmentRepository(dataSource);
        jdbcIdSegmentRepository.setTableName(idGeneratorProperties.getTableName());
        return jdbcIdSegmentRepository;
    }

    @Bean
    @ConditionalOnMissingBean
    public IdAllocator idAllocator(IdSegmentRepository idSegmentRepository) {
        return new IdAllocator(idSegmentRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdSegmentManager idSegmentManager(IdAllocator idAllocator, IdGeneratorProperties idGeneratorProperties) {
        return new IdSegmentManager(idAllocator, idGeneratorProperties);
    }


    @Bean
    @ConditionalOnMissingBean
    public IdGeneratorDatabaseInitializer idGeneratorDatabaseInitializer(JdbcTemplate jdbcTemplate, IdGeneratorProperties idGeneratorProperties) {
        return new IdGeneratorDatabaseInitializer(jdbcTemplate, idGeneratorProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SeqFlow seqFlow() {
        return new SeqFlow();
    }

    @Bean
    public ApplicationRunner idGeneratorTableInitializer(IdGeneratorDatabaseInitializer idGeneratorDatabaseInitializer) {
        return args -> idGeneratorDatabaseInitializer.initialize();
    }
}