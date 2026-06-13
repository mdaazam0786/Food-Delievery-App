package com.foodzie.websocket_manager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The JSON payload pushed down the WebSocket to the customer's app.
 * Deliberately lean — the frontend only needs coordinates and a timestamp.
 *
 * JSON: { "lat": 28.7041, "lon": 77.1025, "ts": 1715678120 }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingPayload {
    private double lat;
    private double lon;
    private long ts;
}
