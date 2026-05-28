package ru.zgys.spring_ai_course_mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

public class ClientDemoMain {
    public static void main(String[] args) {
        HttpClientStreamableHttpTransport clientTransport = HttpClientStreamableHttpTransport.builder("http://localhost:8091")
                .endpoint("/mcpulsor")
                .build();
        McpSyncClient client = McpClient.sync(clientTransport)
                .build();

        client.initialize();
        client.listTools().tools().forEach(System.out::println);
        client.callTool(McpSchema.CallToolRequest
                .builder()
                .name("bioSensor")
                .arguments(Map.of("days", 7))
                .build()
        ).content().forEach(System.out::println);
    }
}
