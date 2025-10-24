package com.petruth.personal_finance_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class PersonalFinanceTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonalFinanceTrackerApplication.class, args);
	}

}
