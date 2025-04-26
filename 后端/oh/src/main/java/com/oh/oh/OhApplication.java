package com.oh.oh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.oh")
public class OhApplication {

	public static void main(String[] args) {
		SpringApplication.run(OhApplication.class, args);
	}

}
