package com.health.management;

import com.health.management.config.FoodVisionProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@MapperScan("com.health.management.mapper")
@SpringBootApplication
@EnableConfigurationProperties(FoodVisionProperties.class)
public class HealthBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthBackendApplication.class, args);
	}

}
