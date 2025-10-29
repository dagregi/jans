package io.jans.federation.rest;

import io.jans.federation.model.EntityData;
import io.jans.federation.model.EntityData.SubordinateEntity;
import io.jans.federation.service.JWTService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Fetch Endpoint for OpenID Federation 1.0
 * 
 * This endpoint allows fetching Subordinate Statements about entities
 * that are subordinate to this entity.
 * 
 * Reference: OpenID Federation 1.0 Section 3.1
 */
@Path("/fetch")
@Produces(MediaType.APPLICATION_JSON)
public class FetchEndpoint {
    
    private static final Logger logger = LoggerFactory.getLogger(FetchEndpoint.class);
    
    /**
     * Fetch Subordinate Statement
     * 
     * Returns an Entity Statement about a subordinate entity.
     * The statement is signed by this entity (the issuer).
     * 
     * Required parameter: sub (subject - the subordinate entity ID)
     * 
     * Reference: https://openid.net/specs/openid-federation-1_0.html#section-7.1
     */
    @GET
    @Produces("application/entity-statement+jwt")
    public Response fetchSubordinateStatement(@QueryParam("sub") String sub) {
        EntityData entityData = EntityData.getInstance();
        
        logger.info("Fetch request for subordinate: {} from entity: {}", 
            sub, entityData.getEntityName());
        
        if (sub == null || sub.isEmpty()) {
            logger.warn("Missing 'sub' parameter in fetch request");
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Missing required parameter 'sub'"))
                .build();
        }
        
        // Find the subordinate
        SubordinateEntity subordinate = entityData.getSubordinate(sub);
        if (subordinate == null) {
            logger.warn("Subordinate not found: {}", sub);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of(
                    "error", "Unknown subordinate",
                    "sub", sub,
                    "issuer", entityData.getEntityId()
                ))
                .build();
        }
        
        try {
            // Create Subordinate Statement claims per OpenID Federation 1.0 spec
            // This is an Entity Statement where iss != sub
            Map<String, Object> claims = new HashMap<>();
            
            // Standard JWT claims
            claims.put("iss", entityData.getEntityId()); // This entity (superior)
            claims.put("sub", subordinate.getEntityId()); // The subordinate
            claims.put("aud", subordinate.getEntityId()); // Audience is the subordinate
            claims.put("iat", System.currentTimeMillis() / 1000);
            claims.put("exp", System.currentTimeMillis() / 1000 + 31536000); // 1 year
            claims.put("jti", UUID.randomUUID().toString());
            
            // JWKS of the subordinate (if provided)
            if (subordinate.getJwks() != null && !subordinate.getJwks().isEmpty()) {
                try {
                    // Parse JSON string to object
                    claims.put("jwks", parseJson(subordinate.getJwks()));
                } catch (Exception e) {
                    logger.error("Failed to parse subordinate JWKS", e);
                }
            }
            
            // Metadata of the subordinate (if provided)
            if (subordinate.getMetadata() != null && !subordinate.getMetadata().isEmpty()) {
                claims.put("metadata", subordinate.getMetadata());
            }
            
            // Source endpoint - where this statement was fetched from
            claims.put("source_endpoint", 
                "http://localhost:" + entityData.getPort() + "/fetch?sub=" + sub);
            
            // Sign the Subordinate Statement
            String signedJWT = JWTService.signEntityStatement(claims);
            
            logger.info("✓ Returning signed Subordinate Statement: iss={}, sub={}", 
                entityData.getEntityId(), sub);
            logger.debug("  Signed JWT (first 100 chars): {}...", 
                signedJWT.substring(0, Math.min(100, signedJWT.length())));
            
            return Response.ok(signedJWT)
                .type("application/entity-statement+jwt")
                .build();
                
        } catch (Exception e) {
            logger.error("Failed to create Subordinate Statement", e);
            return Response.serverError()
                .entity(Map.of("error", "Failed to generate Subordinate Statement: " + e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }
    
    private Object parseJson(String json) {
        // For now, return as-is. In production, use Jackson to parse
        return json;
    }
}


