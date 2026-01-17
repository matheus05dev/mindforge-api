package com.matheusdev.mindforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableCaching
public class MindForgeApplication {

	public static void main(String[] args) {
		
		SpringApplication.run(MindForgeApplication.class, args);
	}

}
