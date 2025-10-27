package io.jans.federation.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Well-Known Endpoint for OpenID Federation 1.0
 * This endpoint MUST be at the root level as per the specification
 */
@Path("/.well-known")
@Produces(MediaType.APPLICATION_JSON)
public class WellKnownEndpoint {
    
    private static final Logger logger = LoggerFactory.getLogger(WellKnownEndpoint.class);
    
    /**
     * Get entity configuration (Section 3.1 of OpenID Federation 1.0)
     * 
     * This is the primary endpoint for discovering entity configurations.
     * It MUST be available at /.well-known/openid-federation
     */
    @GET
    @Path("/openid-federation")
    public Response getEntityConfiguration(@QueryParam("iss") String issuer) {
        logger.info("Getting entity configuration for issuer: {}", issuer);
        
        if (issuer == null || issuer.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "issuer parameter is required"))
                .build();
        }
        
        Map<String, Object> config = createMockEntityConfiguration(issuer);
        return Response.ok(config).build();
    }
    
    private Map<String, Object> createMockEntityConfiguration(String issuer) {
        Map<String, Object> config = new HashMap<>();
        config.put("iss", issuer);
        config.put("sub", issuer);
        config.put("aud", "federation");
        config.put("exp", System.currentTimeMillis() / 1000 + 31536000); // 1 year from now
        config.put("iat", System.currentTimeMillis() / 1000);
        config.put("jti", UUID.randomUUID().toString());
        config.put("authority_hints", Arrays.asList("https://authority.example.com"));
        
        Map<String, Object> jwks = new HashMap<>();
        jwks.put("keys", Arrays.asList(
            Map.of("kty", "RSA", "kid", "key-1", "use", "sig", "alg", "RS256")
        ));
        config.put("jwks", jwks);
        
        Map<String, Object> metadata = new HashMap<>();
        if (issuer.contains("op")) {
            Map<String, Object> opMetadata = new HashMap<>();
            opMetadata.put("issuer", issuer);
            opMetadata.put("authorization_endpoint", issuer + "/auth");
            opMetadata.put("token_endpoint", issuer + "/token");
            opMetadata.put("userinfo_endpoint", issuer + "/userinfo");
            opMetadata.put("jwks_uri", issuer + "/jwks");
            metadata.put("openid_provider", opMetadata);
        } else if (issuer.contains("rp")) {
            Map<String, Object> rpMetadata = new HashMap<>();
            rpMetadata.put("client_id", "rp-client-1");
            rpMetadata.put("redirect_uris", Arrays.asList(issuer + "/callback"));
            rpMetadata.put("response_types", Arrays.asList("code"));
            rpMetadata.put("grant_types", Arrays.asList("authorization_code"));
            metadata.put("openid_relying_party", rpMetadata);
        }
        config.put("metadata", metadata);
        
        return config;
    }
}

