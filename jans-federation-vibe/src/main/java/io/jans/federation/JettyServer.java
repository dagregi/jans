package io.jans.federation;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded Jetty server for Jans Federation Vibe
 */
public class JettyServer {
    
    private static final Logger logger = LoggerFactory.getLogger(JettyServer.class);
    private static final int DEFAULT_PORT = 8080;
    
    public static void main(String[] args) throws Exception {
        int port = getPort();
        
        logger.info("Starting Jans Federation Vibe on port {}", port);
        
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
            logger.info("✅ Jans Federation Vibe started successfully on port {}", port);
            logger.info("📍 API available at: http://localhost:{}/", port);
            logger.info("📋 Federation metadata: http://localhost:{}/federation/metadata", port);
            logger.info("🔍 Health check: http://localhost:{}/database/health", port);
            server.join();
        } catch (Exception e) {
            logger.error("❌ Failed to start server", e);
            throw e;
        } finally {
            server.destroy();
        }
    }
    
    private static int getPort() {
        String portStr = System.getenv("PORT");
        if (portStr != null) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid PORT environment variable: {}, using default", portStr);
            }
        }
        return DEFAULT_PORT;
    }
}

