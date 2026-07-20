package com.oneil.wellness.walkplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WalkPlannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WalkPlannerApplication.class, args);
	}

}
