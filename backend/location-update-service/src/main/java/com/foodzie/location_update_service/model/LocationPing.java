package com.foodzie.location_update_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lean GPS ping payload consumed from the driver-location-pings Kafka topic.
 *
 * Field names are deliberately abbreviated to minimise payload size.
 * At 100k drivers pinging every 5 seconds = 20 million messages/minute,
 * every saved byte reduces Kafka storage and network costs.
 *
 * JSON contract:
 * { "dId": "DRV-8832", "lat": 28.7041, "lon": 77.1025, "zn": "delhi_ncr", "ts": 1715678120 }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationPing {

    @JsonProperty("dId")
    private String driverId;

    @JsonProperty("lat")
    private double latitude;

    @JsonProperty("lon")
    private double longitude;

    /** City/zone key — partitions the Redis geo set, e.g. "delhi_ncr" */
    @JsonProperty("zn")
    private String zone;

    /** Unix epoch timestamp (seconds) — set by the driver app at capture time */
    @JsonProperty("ts")
    private long timestamp;
}
