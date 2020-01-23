package org.springframework.batch.item.querydsl.integrationtest;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.querydsl.integrationtest.config.QuerydslConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Created by jojoldu@gmail.com on 23/01/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@Configuration
@EnableAutoConfiguration
@EnableBatchProcessing
@Import(QuerydslConfiguration.class)
public class TestBatchConfig {

}
