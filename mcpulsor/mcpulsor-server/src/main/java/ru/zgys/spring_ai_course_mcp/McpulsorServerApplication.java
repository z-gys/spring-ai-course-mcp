package ru.zgys.spring_ai_course_mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.SneakyThrows;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.HashMap;
import java.util.Map;

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
                .outputSchema(new JacksonMcpJsonMapper(new ObjectMapper()), createBioSensorOutputSchema())
                .build();

        McpServerFeatures.SyncToolSpecification bioSensorToolSpec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(bioSensorTool)
                .callHandler((mcpSyncServerExchange, callToolRequest) -> {
                    Integer days = (Integer) callToolRequest.arguments().get("days");
                    return calculateResult(days);
                        }
                )
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

    private static McpSchema.CallToolResult calculateResult(Integer days) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("pulse", "твой пульс " + 42 + days);
        properties.put("state", "тебе кабзда");
        properties.put("sleepDeprivation", true);
        return McpSchema.CallToolResult.builder()
                .structuredContent(properties).build();
    }

    private static String createBioSensorOutputSchema() {
        ObjectNode root = new ObjectMapper().createObjectNode().put("type", "object");
        ObjectNode properties = root.putObject("properties");
        properties.putObject("pulse")
                .put("type", "string")
                .put("description", "average heart rate of the user for the last days");
        properties.putObject("state")
                .put("type", "string")
                .put("description", "current state of the user");
        properties.putObject("sleepDeprivation")
                .put("type", "boolean")
                .put("description", "indicates if the user is sleep deprived");
        return root.toString();
    }

    private static String createBioSensorInputSchema() {
        ObjectNode root = new ObjectMapper().createObjectNode().put("type", "object");
        root.putObject("properties")
                .putObject("days")
                .put("type", "integer")
                .put("description", "Number of past days to include in the pulse reading request");
        root.putArray("required").add("days");
        return root.toString();
    }

    private static McpSchema.ServerCapabilities createServerCapabilities() {
        return McpSchema.ServerCapabilities.builder()
                .tools(true)
                .build();
    }
}
