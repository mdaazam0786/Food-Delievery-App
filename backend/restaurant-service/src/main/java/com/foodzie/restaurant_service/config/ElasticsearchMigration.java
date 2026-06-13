package com.foodzie.restaurant_service.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch migration to recreate the index with updated schema.
 * 
 * DISABLED: This component has been disabled to prevent automatic deletion of the
 * Elasticsearch "restaurants" index on service restart. Manual index updates will be
 * performed instead through direct field additions.
 * 
 * To re-enable, add @Component back to the class definition.
 */
@Slf4j
// @Component  ← DISABLED TO PREVENT AUTO-INDEX DELETION
@RequiredArgsConstructor
public class ElasticsearchMigration {

    private final ElasticsearchClient esClient;
    private static final String INDEX_NAME = "restaurants";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @EventListener(ApplicationReadyEvent.class)
    public void recreateIndex() {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                log.info("Elasticsearch migration (attempt {}/{}): Checking index '{}'", 
                        attempt + 1, MAX_RETRIES, INDEX_NAME);

                // Check if index exists
                boolean indexExists = esClient.indices()
                        .exists(ExistsRequest.of(r -> r.index(INDEX_NAME)))
                        .value();

                if (indexExists) {
                    log.info("Elasticsearch migration: Deleting existing index '{}' to apply new schema", INDEX_NAME);
                    // Delete the existing index so Spring Data can recreate it with new mapping
                    esClient.indices().delete(DeleteIndexRequest.of(r -> r.index(INDEX_NAME)));
                    log.info("Elasticsearch migration: Index '{}' deleted successfully", INDEX_NAME);
                    
                    // Wait a bit for deletion to propagate
                    Thread.sleep(500);
                    
                    // Verify it's gone
                    boolean stillExists = esClient.indices()
                            .exists(ExistsRequest.of(r -> r.index(INDEX_NAME)))
                            .value();
                    
                    if (stillExists) {
                        log.warn("Elasticsearch migration: Index still exists after deletion, retrying...");
                        Thread.sleep(RETRY_DELAY_MS);
                        attempt++;
                        continue;
                    }
                } else {
                    log.info("Elasticsearch migration: Index '{}' does not exist yet", INDEX_NAME);
                }

                // Success - index is either gone or never existed
                log.info("Elasticsearch migration: Index '{}' will be auto-created with updated schema on first document index", INDEX_NAME);
                return;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Elasticsearch migration interrupted: {}", e.getMessage());
                return;
            } catch (Exception e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.warn("Elasticsearch migration failed after {} attempts: {}", MAX_RETRIES, e.getMessage());
                    log.warn("The index may still exist with old schema - first document index may fail");
                    return;
                }
                log.warn("Elasticsearch migration attempt {} failed, retrying in {}ms: {}", 
                        attempt, RETRY_DELAY_MS, e.getMessage());
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
