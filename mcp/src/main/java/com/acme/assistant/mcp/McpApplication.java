package com.acme.assistant.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Remote MCP server over streamable HTTP.
 *
 * <p>Cloud Run IAM is the outer gate ({@code --no-allow-unauthenticated}).
 * A bearer token inside the application is the inner one.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class McpApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpApplication.class, args);
    }
}
