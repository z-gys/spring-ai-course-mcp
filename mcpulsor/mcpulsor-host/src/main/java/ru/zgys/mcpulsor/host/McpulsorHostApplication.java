package ru.zgys.mcpulsor.host;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpulsorHostApplication {


    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OllamaChatOptions.builder()
                        .temperature(0.1)
                        .topK(10)
                        .topP(0.95)
                        .repeatPenalty(1.0)
                        .build())
                .build();
    }

    public static void main(String[] args) {

        String firstQuestion = "какой у меня пульс за последние 6 дней?";
        String secondQuestion = "как дела?";
        String thirdQuestion = "какой у меня был пульс за последние 10 дней, если к нему прибавить 1000?";
        Host host = SpringApplication.run(McpulsorHostApplication.class, args).getBean(Host.class);
        host.printAnswerToUser(firstQuestion);
        host.printAnswerToUser(secondQuestion);
        host.printAnswerToUser(thirdQuestion);
    }

}
