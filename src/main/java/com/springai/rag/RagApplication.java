package com.springai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RagApplication {

	private final ChatClient chatClient;

	public RagApplication(ChatClient.Builder chatBuilder) {
		this.chatClient = chatBuilder.build();
	}

	void makeLLMCall() {
		String response = this.chatClient.prompt().user("Who are you!").call().content();
		System.out.println("LLM Response: " + response);
	}

	@Bean
	CommandLineRunner runner() {
		return args -> {
			makeLLMCall();
		};
	}
	public static void main(String[] args) {
		System.out.println("Application Setup ... OK");
		SpringApplication app = new SpringApplication(RagApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		app.run(args);
	}

}
