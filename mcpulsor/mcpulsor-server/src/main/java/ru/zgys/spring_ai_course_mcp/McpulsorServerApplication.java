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
import java.util.List;
import java.util.Map;

import static ru.zgys.spring_ai_course_mcp.MedicalProfileProvider.getMedicalProfile;
import static ru.zgys.spring_ai_course_mcp.PulseCalculator.getPulse;

public class McpulsorServerApplication {

    private static final String SAMPLING_SYSTEM_PROMPT = """
            Ты ставишь диагноз одним словом.
            
            На вход всегда получаешь медицинскую карту человека и его текущий пульс.
            
            Твоя задача — выдать ровно одно:
            название существующей болезни (может быть 1-3 слова, можно редкие или забавно звучащие),
            или
            
            Ответ: -сказать что пациент здоров.
            Правила:
            — Анализируй карту пациента и пульс и выбирай подходящую болезнь.
            — Отвечай только названием болезни или фразой что пациент здоров.
            — Никаких пояснений, никакого текста вокруг.
            """;

    @SneakyThrows
    public static void main(String... args) {
        System.out.println("Server application started");

        var transportProvider = HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint("/mcpulsor")
                .build();

        McpSchema.Tool diagnostatorTool = McpSchema.Tool.builder()
                .name("diagnostator")
                .title("Диагностика по имени")
                .description("Используется для получения диагноза по имени человека. Всегда возвращает либо название болезни, либо сообщение, что человек ничем не болеет.")
                .inputSchema(new JacksonMcpJsonMapper(new ObjectMapper()), createDiagnostatorInputSchema())
                .build();

        McpServerFeatures.SyncToolSpecification diagnostatorToolSpec = McpServerFeatures.SyncToolSpecification.builder()
                .tool(diagnostatorTool)
                .callHandler((mcpSyncServerExchange, callToolRequest) -> {
                            System.out.println("Может делать семплинг? :" + mcpSyncServerExchange.getClientCapabilities().sampling().toString());
                            String name = callToolRequest.arguments().get("name").toString();
                            int pulse = getPulse(name);
                            String medicalProfile = getMedicalProfile(name);
                            String samplingPrompt = "Вот такой у нас пациент, вот его медицинская карта " + medicalProfile + " а вот его текущий пульс " + pulse;

                            McpSchema.CreateMessageRequest samplingMessageRequest = McpSchema.CreateMessageRequest.builder()
                                    .systemPrompt(SAMPLING_SYSTEM_PROMPT)
                                    .temperature(0.1)
                                    .maxTokens(50)
                                    .messages(List.of(new McpSchema.SamplingMessage(McpSchema.Role.USER, new McpSchema.TextContent(samplingPrompt))))
                                    .build();
                            McpSchema.CreateMessageResult samplingResult = mcpSyncServerExchange.createMessage(samplingMessageRequest);

                            McpSchema.LoggingMessageNotification.builder().data("я сервер и рещил спросить вот это "+ samplingPrompt
                            + "\n а в ответ получил "+ samplingResult.content()).build();

                            return McpSchema.CallToolResult.builder()
                                    .addContent(samplingResult.content())
                                    .build();
                        }
                ).build();

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
                            String serverLogMessage = "Сервер говорит: я тут получил такой запрос на вызов тула: " + callToolRequest.toString();
                            System.out.println(serverLogMessage);
                            mcpSyncServerExchange.loggingNotification(McpSchema.LoggingMessageNotification.builder().data(serverLogMessage).build());
                            Integer days = (Integer) callToolRequest.arguments().get("days");
                            return calculateResult(days);
                        }
                )
                .build();

        McpServer.sync(transportProvider)
                .serverInfo("mcpulsor mcp server", "1.0.0.RELEASE")
                .capabilities(createServerCapabilities())
                .tools(bioSensorToolSpec, diagnostatorToolSpec)
                .build();

        Server server = new Server(8091);

        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(transportProvider), "/*");

        server.setHandler(contextHandler);
        server.start();
        server.join();

    }

    private static String createDiagnostatorInputSchema() {
        ObjectNode root = new ObjectMapper().createObjectNode().put("type", "object");
        root.putObject("properties")
                .putObject("name")
                .put("type", "string")
                .put("description", "Имя пациента, по которому необходимо определить текущий диагноз.");
        root.putArray("required").add("name");
        return root.toString();
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
