package com.foodzie.websocket_manager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GPS ping consumed from the driver-location-pings Kafka topic.
 * Field names match the abbreviated contract set by the driver app.
 *
 * JSON: { "dId": "DRV-8832", "lat": 28.7041, "lon": 77.1025, "zn": "delhi_ncr", "ts": 1715678120 }
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

    @JsonProperty("zn")
    private String zone;

    @JsonProperty("ts")
    private long timestamp;
}
