package io.jans.federation.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory data store for Federation Entity
 * 
 * This class maintains all data for a single federation entity including:
 * - Entity name and identifier
 * - Subordinate entities
 * - Trust marks
 * - Entity metadata
 * - Authority hints
 */
public class EntityData {
    
    private static EntityData instance;
    
    private String entityName;
    private String entityId;
    private int port;
    private Map<String, SubordinateEntity> subordinates;
    private List<String> authorityHints;
    private Map<String, Object> metadata;
    private List<TrustMark> trustMarks;
    
    private EntityData() {
        this.subordinates = new ConcurrentHashMap<>();
        this.authorityHints = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.trustMarks = new ArrayList<>();
    }
    
    public static synchronized EntityData getInstance() {
        if (instance == null) {
            instance = new EntityData();
        }
        return instance;
    }
    
    public static synchronized void reset() {
        instance = new EntityData();
    }
    
    // Getters and setters
    
    public String getEntityName() {
        return entityName;
    }
    
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public Map<String, SubordinateEntity> getSubordinates() {
        return subordinates;
    }
    
    public void addSubordinate(SubordinateEntity subordinate) {
        this.subordinates.put(subordinate.getEntityId(), subordinate);
    }
    
    public SubordinateEntity getSubordinate(String entityId) {
        return this.subordinates.get(entityId);
    }
    
    public void removeSubordinate(String entityId) {
        this.subordinates.remove(entityId);
    }
    
    public List<String> getAuthorityHints() {
        return authorityHints;
    }
    
    public void setAuthorityHints(List<String> authorityHints) {
        this.authorityHints = authorityHints;
    }
    
    public void addAuthorityHint(String authorityHint) {
        if (!this.authorityHints.contains(authorityHint)) {
            this.authorityHints.add(authorityHint);
        }
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public List<TrustMark> getTrustMarks() {
        return trustMarks;
    }
    
    public void addTrustMark(TrustMark trustMark) {
        this.trustMarks.add(trustMark);
    }
    
    public TrustMark getTrustMarkById(String id) {
        return trustMarks.stream()
            .filter(tm -> tm.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
    
    public void removeTrustMark(String id) {
        trustMarks.removeIf(tm -> tm.getId().equals(id));
    }
    
    /**
     * Subordinate Entity representation
     */
    public static class SubordinateEntity {
        private String entityId;
        private String jwks;
        private Map<String, Object> metadata;
        private List<String> authorityHints;
        private long createdAt;
        
        public SubordinateEntity() {
            this.createdAt = System.currentTimeMillis();
        }
        
        public String getEntityId() {
            return entityId;
        }
        
        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }
        
        public String getJwks() {
            return jwks;
        }
        
        public void setJwks(String jwks) {
            this.jwks = jwks;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
        
        public List<String> getAuthorityHints() {
            return authorityHints;
        }
        
        public void setAuthorityHints(List<String> authorityHints) {
            this.authorityHints = authorityHints;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
    }
    
}


