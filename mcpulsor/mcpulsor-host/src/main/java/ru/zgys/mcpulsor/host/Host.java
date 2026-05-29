package ru.zgys.mcpulsor.host;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Host {

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    private String systemPrompt;
    private McpSyncClient client;

    @PostConstruct
    public void init() {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder("http://localhost:8091").endpoint("/mcpulsor").build();
        client = McpClient
                .sync(transport)
                .sampling(createMessageRequest -> {
                    ChatClient samplingChatClient = ChatClient.builder(chatModel)
                            .defaultOptions(OllamaChatOptions.builder()
                                    .numPredict(createMessageRequest.maxTokens())
                                    .temperature(createMessageRequest.temperature())
                                    .build())
                            .build();
                    String samplingAnswer = samplingChatClient
                            .prompt()
                            .system(createMessageRequest.systemPrompt())
                            .user(createMessageRequest.messages().getFirst().content().toString())
                            .call()
                            .content();
                    return McpSchema.CreateMessageResult.builder()
                            .content(new McpSchema.TextContent(samplingAnswer))
                            .build();
                })
                .loggingConsumer(loggingMessageNotification -> System.out.println("Сервер говорит: я получил лог от клиента: " + loggingMessageNotification.data()))
                .capabilities(McpSchema.ClientCapabilities.builder().sampling().build())
                .build();
        client.initialize();
        McpSchema.ListToolsResult listToolsResult = client.listTools();
        systemPrompt = SystemPromptFactory.withTools(listToolsResult);
    }

    public void printAnswerToUser(String question) {
        System.out.println("Хост говорит: клиент задал такой вопрос: " + question);
        AssistantMessage assistantMessage = chatClient.prompt().system(systemPrompt).user(question).call().chatResponse().getResult().getOutput();
        if (CallToolUtil.isToolRequired(assistantMessage.getText())) {
            System.out.println("Хост говорит: модель просит че-то сделать: " + assistantMessage.getText());
            McpSchema.CallToolRequest toolRequest = CallToolUtil.getRequiredTool(assistantMessage.getText());
            String toolResponse = CallToolUtil.wrapResponse(client.callTool(toolRequest).content().getFirst().toString());
            System.out.println("Хост говорит: вот что принес клиент от сервера по просьбе модели: " + toolResponse);
            assistantMessage = chatClient.prompt().system(systemPrompt).messages(List.of(
                    new UserMessage(question),
                    assistantMessage,
                    new UserMessage(toolResponse)
            )).call().chatResponse().getResult().getOutput();

        }

        System.out.println(assistantMessage.getText());
    }
}
