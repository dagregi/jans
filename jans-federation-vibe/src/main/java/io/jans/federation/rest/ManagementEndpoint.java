package io.jans.federation.rest;

import io.jans.federation.model.EntityData;
import io.jans.federation.model.EntityData.SubordinateEntity;
import io.jans.federation.model.TrustMark;
import io.jans.federation.service.TrustMarkService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Management API for Federation Entity
 * 
 * This endpoint provides CRUD operations for managing subordinate entities.
 * It is not part of the OpenID Federation 1.0 specification but is needed
 * for administrative operations.
 */
@Path("/manage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ManagementEndpoint {
    
    private static final Logger logger = LoggerFactory.getLogger(ManagementEndpoint.class);
    
    /**
     * Get information about this entity
     */
    @GET
    @Path("/entity")
    public Response getEntityInfo() {
        EntityData entityData = EntityData.getInstance();
        
        Map<String, Object> info = new HashMap<>();
        info.put("entity_name", entityData.getEntityName());
        info.put("entity_id", entityData.getEntityId());
        info.put("port", entityData.getPort());
        info.put("subordinates_count", entityData.getSubordinates().size());
        info.put("authority_hints", entityData.getAuthorityHints());
        
        logger.info("Entity info requested for: {}", entityData.getEntityName());
        
        return Response.ok(info).build();
    }
    
    /**
     * Set authority hints for this entity
     */
    @POST
    @Path("/entity/authority-hints")
    public Response setAuthorityHints(Map<String, Object> request) {
        EntityData entityData = EntityData.getInstance();
        
        if (!request.containsKey("authority_hints")) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "authority_hints field is required"))
                .build();
        }
        
        List<String> hints = (List<String>) request.get("authority_hints");
        entityData.setAuthorityHints(hints);
        
        logger.info("Authority hints set for {}: {}", entityData.getEntityName(), hints);
        
        return Response.ok(Map.of(
            "entity_id", entityData.getEntityId(),
            "authority_hints", hints,
            "status", "updated"
        )).build();
    }
    
    /**
     * List all subordinates (READ)
     */
    @GET
    @Path("/subordinates")
    public Response listSubordinates() {
        EntityData entityData = EntityData.getInstance();
        
        List<Map<String, Object>> subordinates = entityData.getSubordinates().values().stream()
            .map(this::subordinateToMap)
            .collect(Collectors.toList());
        
        logger.info("Listed {} subordinates for entity: {}", 
            subordinates.size(), entityData.getEntityName());
        
        return Response.ok(subordinates).build();
    }
    
    /**
     * Get a specific subordinate (READ)
     */
    @GET
    @Path("/subordinates/{entityId : .+}")
    public Response getSubordinate(@PathParam("entityId") String entityId) {
        EntityData entityData = EntityData.getInstance();
        
        SubordinateEntity subordinate = entityData.getSubordinate(entityId);
        if (subordinate == null) {
            logger.warn("Subordinate not found: {}", entityId);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Subordinate not found", "entity_id", entityId))
                .build();
        }
        
        logger.info("Retrieved subordinate: {}", entityId);
        return Response.ok(subordinateToMap(subordinate)).build();
    }
    
    /**
     * Add a subordinate (CREATE)
     * 
     * Request body:
     * {
     *   "entity_id": "https://subordinate.example.com",
     *   "jwks": {...},
     *   "metadata": {...},
     *   "authority_hints": [...]
     * }
     */
    @POST
    @Path("/subordinates")
    public Response addSubordinate(Map<String, Object> request) {
        EntityData entityData = EntityData.getInstance();
        
        String entityId = (String) request.get("entity_id");
        if (entityId == null || entityId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "entity_id is required"))
                .build();
        }
        
        // Check if already exists - if so, update instead of error
        SubordinateEntity existing = entityData.getSubordinate(entityId);
        if (existing != null) {
            logger.info("Subordinate {} already exists, updating instead", entityId);
            // Update existing subordinate
            if (request.containsKey("jwks")) {
                try {
                    existing.setJwks(objectToJson(request.get("jwks")));
                } catch (Exception e) {
                    logger.error("Invalid JWKS format", e);
                }
            }
            if (request.containsKey("metadata")) {
                existing.setMetadata((Map<String, Object>) request.get("metadata"));
            }
            
            Map<String, Object> response = subordinateToMap(existing);
            response.put("status", "updated");
            return Response.ok(response).build();
        }
        
        SubordinateEntity subordinate = new SubordinateEntity();
        subordinate.setEntityId(entityId);
        
        // Set JWKS (as JSON string)
        if (request.containsKey("jwks")) {
            try {
                subordinate.setJwks(objectToJson(request.get("jwks")));
            } catch (Exception e) {
                logger.error("Invalid JWKS format", e);
            }
        }
        
        // Set metadata
        if (request.containsKey("metadata")) {
            subordinate.setMetadata((Map<String, Object>) request.get("metadata"));
        }
        
        // Set authority hints - should include this entity
        List<String> authorityHints = new ArrayList<>();
        if (request.containsKey("authority_hints")) {
            authorityHints.addAll((List<String>) request.get("authority_hints"));
        }
        // Add this entity as authority if not already present
        if (!authorityHints.contains(entityData.getEntityId())) {
            authorityHints.add(entityData.getEntityId());
        }
        subordinate.setAuthorityHints(authorityHints);
        
        entityData.addSubordinate(subordinate);
        
        logger.info("Added subordinate: {} to entity: {}", entityId, entityData.getEntityName());
        
        Map<String, Object> response = subordinateToMap(subordinate);
        response.put("status", "created");
        
        return Response.status(Response.Status.CREATED).entity(response).build();
    }
    
    /**
     * Update a subordinate (UPDATE)
     */
    @PUT
    @Path("/subordinates/{entityId : .+}")
    public Response updateSubordinate(@PathParam("entityId") String entityId, Map<String, Object> request) {
        EntityData entityData = EntityData.getInstance();
        
        SubordinateEntity subordinate = entityData.getSubordinate(entityId);
        if (subordinate == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Subordinate not found", "entity_id", entityId))
                .build();
        }
        
        // Update fields if provided
        if (request.containsKey("jwks")) {
            try {
                subordinate.setJwks(objectToJson(request.get("jwks")));
            } catch (Exception e) {
                logger.error("Invalid JWKS format", e);
            }
        }
        
        if (request.containsKey("metadata")) {
            subordinate.setMetadata((Map<String, Object>) request.get("metadata"));
        }
        
        if (request.containsKey("authority_hints")) {
            subordinate.setAuthorityHints((List<String>) request.get("authority_hints"));
        }
        
        logger.info("Updated subordinate: {}", entityId);
        
        Map<String, Object> response = subordinateToMap(subordinate);
        response.put("status", "updated");
        
        return Response.ok(response).build();
    }
    
    /**
     * Delete a subordinate (DELETE)
     */
    @DELETE
    @Path("/subordinates/{entityId : .+}")
    public Response deleteSubordinate(@PathParam("entityId") String entityId) {
        EntityData entityData = EntityData.getInstance();
        
        SubordinateEntity subordinate = entityData.getSubordinate(entityId);
        if (subordinate == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Subordinate not found", "entity_id", entityId))
                .build();
        }
        
        entityData.removeSubordinate(entityId);
        
        logger.info("Deleted subordinate: {} from entity: {}", entityId, entityData.getEntityName());
        
        return Response.ok(Map.of(
            "status", "deleted",
            "entity_id", entityId
        )).build();
    }
    
    /**
     * Get subordinate's Entity Statement
     * This is the subordinate list endpoint per OpenID Federation spec
     */
    @GET
    @Path("/fetch")
    public Response fetchSubordinateStatement(@QueryParam("sub") String sub) {
        EntityData entityData = EntityData.getInstance();
        
        if (sub == null || sub.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "'sub' parameter is required"))
                .build();
        }
        
        SubordinateEntity subordinate = entityData.getSubordinate(sub);
        if (subordinate == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Subordinate not found", "sub", sub))
                .build();
        }
        
        // Create Subordinate Statement per OpenID Federation 1.0 spec
        Map<String, Object> statement = new HashMap<>();
        statement.put("iss", entityData.getEntityId()); // Issuer is this entity
        statement.put("sub", subordinate.getEntityId()); // Subject is the subordinate
        statement.put("aud", sub); // Audience is the subordinate
        statement.put("iat", System.currentTimeMillis() / 1000);
        statement.put("exp", System.currentTimeMillis() / 1000 + 31536000); // 1 year
        statement.put("jti", UUID.randomUUID().toString());
        
        // Add JWKS if present
        if (subordinate.getJwks() != null) {
            try {
                statement.put("jwks", jsonToObject(subordinate.getJwks()));
            } catch (Exception e) {
                logger.error("Failed to parse JWKS", e);
            }
        }
        
        // Add metadata if present
        if (subordinate.getMetadata() != null && !subordinate.getMetadata().isEmpty()) {
            statement.put("metadata", subordinate.getMetadata());
        }
        
        logger.info("Fetched subordinate statement for: {} from entity: {}", 
            sub, entityData.getEntityName());
        
        return Response.ok(statement).build();
    }
    
    // ==================== Trust Mark Management ====================
    
    /**
     * Issue a Trust Mark to an entity
     * 
     * POST /manage/trust-marks
     * Body: {
     *   "trust_mark_id": "https://refeds.org/sirtfi",
     *   "subject": "https://op.umu.se",
     *   "expires_in": 31536000  // Optional, seconds
     * }
     */
    @POST
    @Path("/trust-marks")
    public Response issueTrustMark(Map<String, Object> request) {
        String trustMarkId = (String) request.get("trust_mark_id");
        String subject = (String) request.get("subject");
        
        if (trustMarkId == null || trustMarkId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "trust_mark_id is required"))
                .build();
        }
        
        if (subject == null || subject.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "subject is required"))
                .build();
        }
        
        Long expiresIn = null;
        if (request.containsKey("expires_in")) {
            Object expiresInObj = request.get("expires_in");
            if (expiresInObj instanceof Number) {
                expiresIn = ((Number) expiresInObj).longValue();
            }
        }
        
        try {
            String signedJWT = TrustMarkService.issueTrustMark(trustMarkId, subject, expiresIn);
            
            EntityData entityData = EntityData.getInstance();
            logger.info("✓ Trust Mark issued: {} to {} by {}", 
                trustMarkId, subject, entityData.getEntityId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "created");
            response.put("trust_mark_id", trustMarkId);
            response.put("issuer", entityData.getEntityId());
            response.put("subject", subject);
            response.put("signed_jwt", signedJWT);
            
            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (Exception e) {
            logger.error("Failed to issue Trust Mark", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to issue Trust Mark: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * List all Trust Marks issued by this entity
     * 
     * GET /manage/trust-marks
     */
    @GET
    @Path("/trust-marks")
    public Response listTrustMarks() {
        List<TrustMark> trustMarks = TrustMarkService.getIssuedTrustMarks();
        
        List<Map<String, Object>> response = trustMarks.stream()
            .map(tm -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", tm.getId());
                map.put("issuer", tm.getIssuer());
                map.put("subject", tm.getSubject());
                map.put("issued_at", tm.getIssuedAt());
                map.put("expires_at", tm.getExpiresAt());
                map.put("expired", tm.isExpired());
                map.put("signed_jwt", tm.getSignedJWT());
                return map;
            })
            .collect(Collectors.toList());
        
        EntityData entityData = EntityData.getInstance();
        logger.info("Listed {} Trust Marks for entity: {}", response.size(), entityData.getEntityName());
        
        return Response.ok(response).build();
    }
    
    /**
     * Get a specific Trust Mark
     * 
     * GET /manage/trust-marks/{trustMarkId}
     */
    @GET
    @Path("/trust-marks/{trustMarkId : .+}")
    public Response getTrustMark(@PathParam("trustMarkId") String trustMarkId) {
        TrustMark trustMark = TrustMarkService.getTrustMark(trustMarkId);
        
        if (trustMark == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Trust Mark not found", "trust_mark_id", trustMarkId))
                .build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", trustMark.getId());
        response.put("issuer", trustMark.getIssuer());
        response.put("subject", trustMark.getSubject());
        response.put("issued_at", trustMark.getIssuedAt());
        response.put("expires_at", trustMark.getExpiresAt());
        response.put("expired", trustMark.isExpired());
        response.put("signed_jwt", trustMark.getSignedJWT());
        
        return Response.ok(response).build();
    }
    
    /**
     * Revoke a Trust Mark (for issuer)
     * 
     * DELETE /manage/trust-marks/{trustMarkId}
     */
    @DELETE
    @Path("/trust-marks/{trustMarkId : .+}")
    public Response revokeTrustMark(@PathParam("trustMarkId") String trustMarkId) {
        TrustMark trustMark = TrustMarkService.getTrustMark(trustMarkId);
        
        if (trustMark == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Trust Mark not found", "trust_mark_id", trustMarkId))
                .build();
        }
        
        TrustMarkService.revokeTrustMark(trustMarkId);
        
        logger.info("✓ Trust Mark revoked: {}", trustMarkId);
        
        return Response.ok(Map.of(
            "status", "revoked",
            "trust_mark_id", trustMarkId
        )).build();
    }
    
    /**
     * Add a received Trust Mark to this entity
     * This is called by the subordinate entity after receiving a Trust Mark from a superior
     * 
     * POST /manage/entity/trust-marks
     * Body: {
     *   "signed_jwt": "eyJ..."  // The signed Trust Mark JWT
     * }
     */
    @POST
    @Path("/entity/trust-marks")
    public Response addReceivedTrustMark(Map<String, Object> request) {
        String signedJWT = (String) request.get("signed_jwt");
        
        if (signedJWT == null || signedJWT.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "signed_jwt is required"))
                .build();
        }
        
        try {
            // Parse the Trust Mark JWT to extract claims
            com.nimbusds.jwt.SignedJWT jwt = com.nimbusds.jwt.SignedJWT.parse(signedJWT);
            com.nimbusds.jwt.JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();
            
            String trustMarkId = claimsSet.getStringClaim("id");
            String issuer = claimsSet.getIssuer();
            String subject = claimsSet.getSubject();
            long iat = claimsSet.getIssueTime() != null ? claimsSet.getIssueTime().getTime() / 1000 : 0;
            Long exp = claimsSet.getExpirationTime() != null ? claimsSet.getExpirationTime().getTime() / 1000 : null;
            
            EntityData entityData = EntityData.getInstance();
            
            // Verify this Trust Mark is for THIS entity
            if (!subject.equals(entityData.getEntityId())) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Trust Mark subject does not match this entity",
                        "expected", entityData.getEntityId(),
                        "got", subject))
                    .build();
            }
            
            // Create and store Trust Mark
            io.jans.federation.model.TrustMark trustMark = 
                new io.jans.federation.model.TrustMark(trustMarkId, issuer, subject);
            trustMark.setIssuedAt(iat);
            trustMark.setExpiresAt(exp);
            trustMark.setSignedJWT(signedJWT);
            
            entityData.addTrustMark(trustMark);
            
            logger.info("✓ Trust Mark added to entity: id={}, issuer={}, subject={}", 
                trustMarkId, issuer, subject);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "added");
            response.put("trust_mark_id", trustMarkId);
            response.put("issuer", issuer);
            response.put("subject", subject);
            
            return Response.status(Response.Status.CREATED).entity(response).build();
            
        } catch (Exception e) {
            logger.error("Failed to parse Trust Mark JWT", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Invalid Trust Mark JWT: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * List Trust Marks that this entity has received (about itself)
     * 
     * GET /manage/entity/trust-marks
     */
    @GET
    @Path("/entity/trust-marks")
    public Response listEntityTrustMarks() {
        EntityData entityData = EntityData.getInstance();
        
        List<Map<String, Object>> response = entityData.getTrustMarks().stream()
            .filter(tm -> tm.getSubject().equals(entityData.getEntityId()))
            .map(tm -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", tm.getId());
                map.put("issuer", tm.getIssuer());
                map.put("issued_at", tm.getIssuedAt());
                map.put("expires_at", tm.getExpiresAt());
                map.put("expired", tm.isExpired());
                map.put("signed_jwt", tm.getSignedJWT());
                return map;
            })
            .collect(Collectors.toList());
        
        logger.info("Listed {} Trust Marks for entity: {}", response.size(), entityData.getEntityName());
        
        return Response.ok(response).build();
    }
    
    // Helper methods
    
    private Map<String, Object> subordinateToMap(SubordinateEntity subordinate) {
        Map<String, Object> map = new HashMap<>();
        map.put("entity_id", subordinate.getEntityId());
        
        if (subordinate.getJwks() != null) {
            try {
                map.put("jwks", jsonToObject(subordinate.getJwks()));
            } catch (Exception e) {
                map.put("jwks", subordinate.getJwks());
            }
        }
        
        if (subordinate.getMetadata() != null) {
            map.put("metadata", subordinate.getMetadata());
        }
        
        if (subordinate.getAuthorityHints() != null) {
            map.put("authority_hints", subordinate.getAuthorityHints());
        }
        
        map.put("created_at", subordinate.getCreatedAt());
        
        return map;
    }
    
    private String objectToJson(Object obj) {
        // Simple JSON conversion for testing
        // In production, use Jackson ObjectMapper
        if (obj instanceof Map || obj instanceof List) {
            return obj.toString();
        }
        return String.valueOf(obj);
    }
    
    private Object jsonToObject(String json) {
        // Simple parsing - in production use Jackson
        return json;
    }
}

