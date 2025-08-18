package com.springai.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RagApplication {

	public static void main(String[] args) {
		System.out.println("Application Setup ... OK");
		SpringApplication.run(RagApplication.class, args);
	}

}
