package com.jojoldu.springbatchquerydsl;

import com.jojoldu.springbatchquerydsl.config.QuerydslConfiguration;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * Created by jojoldu@gmail.com on 15/08/2018
 * Blog : http://jojoldu.tistory.com
 * Github : https://github.com/jojoldu
 */

@Configuration
@EnableAutoConfiguration
@EnableBatchProcessing
@Import(QuerydslConfiguration.class)
public class TestBatchConfig {

}
