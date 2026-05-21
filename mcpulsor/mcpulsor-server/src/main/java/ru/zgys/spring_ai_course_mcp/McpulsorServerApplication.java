package ru.zgys.spring_ai_course_mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.SneakyThrows;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class McpulsorServerApplication {

    @SneakyThrows
    public static void main(String... args) {
        System.out.println("Server application started");

        var transportProvider = HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint("/mcpulsor")
                .build();

        McpSchema.Tool bioSensorTool = McpSchema.Tool.builder()
                .name("bioSensor")
                .title("Human Vital Pulsor Sensor")
                .description("Returns the current heart rate of the user as a simple string value.")
                .inputSchema(new JacksonMcpJsonMapper(new ObjectMapper()), createBioSensorInputSchema())
                .build();

        McpServerFeatures.SyncToolSpecification bioSensorToolSpec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(bioSensorTool)
                .callHandler((mcpSyncServerExchange, callToolRequest) ->
                        new McpSchema.CallToolResult("пульс пользователя 42", false))
                .build();

        McpServer.sync(transportProvider)
                .serverInfo("mcpulsor mcp server", "1.0.0.RELEASE")
                .capabilities(createServerCapabilities())
                .tools(bioSensorToolSpec)
                .build();

        Server server = new Server(8091);

        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(transportProvider), "/*");

        server.setHandler(contextHandler);
        server.start();
        server.join();

    }

    private static String createBioSensorInputSchema() {
        return new ObjectMapper().createObjectNode().put("type", "object").toString();
    }

    private static McpSchema.ServerCapabilities createServerCapabilities() {
        return McpSchema.ServerCapabilities.builder()
                .tools(true)
                .build();
    }
}
