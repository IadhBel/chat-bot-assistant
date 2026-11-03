package com.acme.assistant.rag.index;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "acme.rag.backend", havingValue = "local", matchIfMissing = true)
public class ObjectIndexConfiguration {

    @Bean
    ObjectIndex objectIndex(ObjectProvider<DataSource> dataSource, ObjectProvider<JdbcTemplate> jdbcTemplate) {
        if (dataSource.getIfAvailable() == null || jdbcTemplate.getIfAvailable() == null) {
            return new MemoryObjectIndex();
        }
        return new JdbcObjectIndex(jdbcTemplate.getObject());
    }
}
