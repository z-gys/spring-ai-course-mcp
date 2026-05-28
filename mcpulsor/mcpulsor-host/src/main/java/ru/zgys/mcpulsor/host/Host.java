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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Host {

    private final ChatClient chatClient;

    private String systemPrompt;
    private McpSyncClient client;

    @PostConstruct
    public void init() {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder("http://localhost:8091").endpoint("/mcpulsor").build();
        client = McpClient.sync(transport).build();
        client.initialize();
        McpSchema.ListToolsResult listToolsResult = client.listTools();
        systemPrompt = SystemPromptFactory.withTools(listToolsResult);
    }

    public void printAnswerToUser(String question) {
        AssistantMessage assistantMessage = chatClient.prompt().system(systemPrompt).user(question).call().chatResponse().getResult().getOutput();
        if (CallToolUtil.isToolRequired(assistantMessage.getText())) {

            McpSchema.CallToolRequest toolRequest = CallToolUtil.getRequiredTool(assistantMessage.getText());
            String toolResponse = CallToolUtil.wrapResponse(client.callTool(toolRequest).content().getFirst().toString());

            assistantMessage = chatClient.prompt().system(systemPrompt).messages(List.of(
                    new UserMessage(question),
                    assistantMessage,
                    new UserMessage(toolResponse)
            )).call().chatResponse().getResult().getOutput();

        }

        System.out.println(assistantMessage.getText());
    }
}
