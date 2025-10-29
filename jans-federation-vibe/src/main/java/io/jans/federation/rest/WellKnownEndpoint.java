package io.jans.federation.rest;

import io.jans.federation.model.EntityData;
import io.jans.federation.service.KeyManager;
import io.jans.federation.service.JWTService;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Well-Known Endpoint for OpenID Federation 1.0
 * 
 * This endpoint provides the Entity Configuration (Entity Statement about itself)
 * as defined in Section 3.1 of the OpenID Federation 1.0 specification.
 */
@Path("/.well-known")
@Produces(MediaType.APPLICATION_JSON)
public class WellKnownEndpoint {
    
    private static final Logger logger = LoggerFactory.getLogger(WellKnownEndpoint.class);
    
    /**
     * Get Entity Configuration (Section 3.1)
     * 
     * Returns a self-signed Entity Statement (JWT) containing:
     * - iss: Entity identifier (same as sub for self-signed)
     * - sub: Entity identifier
     * - iat: Issued at time
     * - exp: Expiration time
     * - jwks: JSON Web Key Set
     * - metadata: Entity metadata (optional)
     * - authority_hints: List of superior entities (optional)
     * - trust_marks: Trust marks (optional)
     * 
     * Reference: https://openid.net/specs/openid-federation-1_0.html#section-3.1
     */
    @GET
    @Path("/openid-federation")
    @Produces("application/entity-statement+jwt")
    public Response getEntityConfiguration() {
        EntityData entityData = EntityData.getInstance();
        KeyManager keyManager = KeyManager.getInstance();
        
        logger.info("Entity Configuration requested for: {} ({})", 
            entityData.getEntityName(), entityData.getEntityId());
        
        try {
            // Create Entity Statement claims (self-signed)
            Map<String, Object> claims = new HashMap<>();
            
            // Required claims per spec
            claims.put("iss", entityData.getEntityId());
            claims.put("sub", entityData.getEntityId());
            claims.put("iat", System.currentTimeMillis() / 1000);
            claims.put("exp", System.currentTimeMillis() / 1000 + 31536000); // 1 year
            claims.put("jti", UUID.randomUUID().toString());
            
            // JWKS - required for signature verification (public key only)
            RSAKey publicKey = keyManager.getPublicJWK();
            Map<String, Object> jwks = new HashMap<>();
            List<Map<String, Object>> keys = new ArrayList<>();
            Map<String, Object> keyMap = publicKey.toJSONObject();
            keys.add(keyMap);
            jwks.put("keys", keys);
            claims.put("jwks", jwks);
            
            // Metadata - optional but recommended
            if (entityData.getMetadata() != null && !entityData.getMetadata().isEmpty()) {
                claims.put("metadata", entityData.getMetadata());
            } else {
                // Default metadata for federation entity
                Map<String, Object> metadata = new HashMap<>();
                Map<String, Object> federationEntity = new HashMap<>();
                federationEntity.put("federation_fetch_endpoint", 
                    "http://localhost:" + entityData.getPort() + "/fetch");
                federationEntity.put("federation_list_endpoint",
                    "http://localhost:" + entityData.getPort() + "/manage/subordinates");
                metadata.put("federation_entity", federationEntity);
                claims.put("metadata", metadata);
            }
            
            // Authority hints - list of superior entities (empty for Trust Anchor)
            if (entityData.getAuthorityHints() != null && !entityData.getAuthorityHints().isEmpty()) {
                claims.put("authority_hints", entityData.getAuthorityHints());
            }
            
            // Trust marks - optional
            if (entityData.getTrustMarks() != null && !entityData.getTrustMarks().isEmpty()) {
                claims.put("trust_marks", entityData.getTrustMarks());
            }
            
            // Sign the Entity Statement
            String signedJWT = JWTService.signEntityStatement(claims);
            
            logger.info("✓ Returning signed Entity Configuration for: {}", entityData.getEntityId());
            logger.debug("  Signed JWT (first 100 chars): {}...", 
                signedJWT.substring(0, Math.min(100, signedJWT.length())));
            
            // Return as signed JWT (application/entity-statement+jwt)
            return Response.ok(signedJWT)
                .type("application/entity-statement+jwt")
                .build();
                
        } catch (Exception e) {
            logger.error("Failed to create Entity Configuration", e);
            return Response.serverError()
                .entity(Map.of("error", "Failed to generate Entity Configuration: " + e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }
}
