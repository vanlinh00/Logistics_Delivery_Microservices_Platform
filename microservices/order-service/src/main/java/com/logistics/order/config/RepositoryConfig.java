package com.logistics.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.logistics.order.repository")
@EnableElasticsearchRepositories(basePackages = "com.logistics.order.elasticsearch")
public class RepositoryConfig {
}
