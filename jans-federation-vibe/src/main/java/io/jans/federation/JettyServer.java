package io.jans.federation;

import io.jans.federation.model.EntityData;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded Jetty server for Jans Federation Vibe
 * Each instance represents a Federation Entity
 */
public class JettyServer {
    
    private static final Logger logger = LoggerFactory.getLogger(JettyServer.class);
    private static final int DEFAULT_PORT = 8080;
    
    public static void main(String[] args) throws Exception {
        // Get node name from arguments
        String nodeName = "node1"; // default
        if (args.length > 0) {
            nodeName = args[0];
        }
        
        // Get port (can be overridden by PORT env variable or auto-assigned)
        int port = getPort(nodeName);
        
        // Map node name to entity ID
        String entityId = getEntityId(nodeName);
        
        // Initialize entity data
        EntityData entityData = EntityData.getInstance();
        entityData.setEntityName(nodeName);
        entityData.setEntityId(entityId);
        entityData.setPort(port);
        
        logger.info("=".repeat(60));
        logger.info("Starting Jans Federation Vibe");
        logger.info("Entity Name: {}", nodeName);
        logger.info("Entity ID: {}", entityData.getEntityId());
        logger.info("Port: {}", port);
        logger.info("=".repeat(60));
        
        // Initialize cryptographic keys
        try {
            io.jans.federation.service.KeyManager keyManager = 
                io.jans.federation.service.KeyManager.getInstance();
            keyManager.initialize(nodeName);
        } catch (Exception e) {
            logger.error("Failed to initialize cryptographic keys", e);
            throw new RuntimeException("Key initialization failed", e);
        }
        
        Server server = new Server(port);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        
        // Configure Jersey servlet
        ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(0);
        jerseyServlet.setInitParameter(
            "jersey.config.server.provider.packages",
            "io.jans.federation.rest"
        );
        jerseyServlet.setInitParameter(
            "jersey.config.server.provider.classnames",
            "org.glassfish.jersey.media.json.JsonJacksonFeature"
        );
        
        try {
            server.start();
            logger.info("✅ Federation Entity '{}' started successfully", nodeName);
            logger.info("📍 Entity ID: {}", entityData.getEntityId());
            logger.info("🌐 API URL: http://localhost:{}/", port);
            logger.info("📋 Entity Configuration: http://localhost:{}/.well-known/openid-federation", port);
            logger.info("🔧 Management API: http://localhost:{}/manage", port);
            logger.info("=".repeat(60));
            server.join();
        } catch (Exception e) {
            logger.error("❌ Failed to start server", e);
            throw e;
        } finally {
            server.destroy();
        }
    }
    
    private static int getPort(String nodeName) {
        // Check for PORT environment variable
        String portStr = System.getenv("PORT");
        if (portStr != null) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid PORT environment variable: {}, using auto-assignment", portStr);
            }
        }
        
        // Map specific node names to ports (Appendix A entities)
        switch (nodeName.toLowerCase()) {
            case "edugain": return 8080;
            case "swamid": return 8081;
            case "umu": return 8082;
            case "op-umu": case "opumu": return 8083;
            case "ligo": return 8084;
        }
        
        // Auto-assign ports based on node name
        // node1 -> 8080, node2 -> 8081, node3 -> 8082, etc.
        if (nodeName.startsWith("node")) {
            try {
                int nodeNum = Integer.parseInt(nodeName.substring(4));
                return 8080 + (nodeNum - 1);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        
        return DEFAULT_PORT;
    }
    
    private static String getEntityId(String nodeName) {
        // Map specific node names to entity IDs (Appendix A entities)
        switch (nodeName.toLowerCase()) {
            case "edugain": return "https://edugain.geant.org";
            case "swamid": return "https://swamid.se";
            case "umu": return "https://umu.se";
            case "op-umu": case "opumu": return "https://op.umu.se";
            case "ligo": return "https://ligo.example.org";
        }
        
        // Default: node name to entity ID
        return "https://" + nodeName + ".example.com";
    }
}

