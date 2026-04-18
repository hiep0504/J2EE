package com.example.Backend_J2EE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendJ2EeApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendJ2EeApplication.class, args);
	}

}
