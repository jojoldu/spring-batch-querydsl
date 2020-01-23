package org.springframework.batch.item.querydsl.integrationtest;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableBatchProcessing
@SpringBootApplication
public class IntegrationTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(IntegrationTestApplication.class, args);
	}

}
