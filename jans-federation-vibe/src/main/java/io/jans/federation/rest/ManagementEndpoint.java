package io.jans.federation.rest;

import io.jans.federation.model.EntityData;
import io.jans.federation.model.EntityData.SubordinateEntity;
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

