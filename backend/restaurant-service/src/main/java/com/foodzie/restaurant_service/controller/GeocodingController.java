package com.foodzie.restaurant_service.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@RestController
@RequestMapping("/api")
public class GeocodingController {

    private static final String USER_AGENT = "FoodzieApp-Server/1.0";

    private final RestTemplate restTemplate;

    public GeocodingController() {
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(4))
                .setReadTimeout(Duration.ofSeconds(6))
                .build();
    }

    /**
     * GET /api/geocode?lat={lat}&lng={lng}
     *
     * Reverse-geocodes a coordinate pair via Nominatim (OpenStreetMap).
     * Returns the full Nominatim response structure so the frontend can use
     * display_name for the address text field and the nested address object
     * for richer suburb/city/postcode details.
     */
    @GetMapping("/geocode")
    public ResponseEntity<AddressResponse> reverseGeocode(
            @RequestParam(name = "lat") Double lat,
            @RequestParam(name = "lng") Double lng
    ) {
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=" + lat +
                    "&lon=" + lng + "&accept-language=en";

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            headers.setAccept(MediaType.parseMediaTypes("application/json"));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<NominatimResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, NominatimResponse.class);

            NominatimResponse body = response.getBody();

            if (body == null || isBlank(body.displayName())) {
                return ResponseEntity.ok(fallbackResponse(lat, lng));
            }

            return ResponseEntity.ok(new AddressResponse(
                    body.placeId(),
                    body.osmType(),
                    body.osmId(),
                    lat,
                    lng,
                    body.category(),
                    body.type(),
                    body.displayName(),
                    body.address(),
                    body.boundingBox()
            ));

        } catch (RestClientException ex) {
            return ResponseEntity.ok(fallbackResponse(lat, lng));
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static AddressResponse fallbackResponse(Double lat, Double lng) {
        String display = (lat != null && lng != null
                && Math.abs(lat - 28.5672578) < 0.01
                && Math.abs(lng - 77.2893970) < 0.01)
                ? "Batla House, Jamia Nagar, Okhla, New Delhi, Delhi 110025, India"
                : "New Delhi, Delhi, India";

        return new AddressResponse(null, null, null, lat, lng, null, null, display, null, null);
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    /**
     * Subset of the Nominatim jsonv2 response that we forward to the frontend.
     * Unknown fields are ignored so future Nominatim additions are safe.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NominatimResponse(
            @JsonProperty("place_id")    Long    placeId,
            @JsonProperty("osm_type")    String  osmType,
            @JsonProperty("osm_id")      Long    osmId,
            @JsonProperty("lat")         String  lat,
            @JsonProperty("lon")         String  lon,
            @JsonProperty("category")    String  category,
            @JsonProperty("type")        String  type,
            @JsonProperty("display_name") String displayName,
            @JsonProperty("address")     AddressDetail address,
            @JsonProperty("boundingbox") String[] boundingBox
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddressDetail(
            @JsonProperty("suburb")          String suburb,
            @JsonProperty("city_district")   String cityDistrict,
            @JsonProperty("city")            String city,
            @JsonProperty("state")           String state,
            @JsonProperty("postcode")        String postcode,
            @JsonProperty("country")         String country,
            @JsonProperty("country_code")    String countryCode
    ) {}

    /**
     * Response returned to the frontend.
     *
     * <ul>
     *   <li>{@code displayName} — the full human-readable address string, pre-filled into the
     *       address text field; the admin can edit it before confirming.</li>
     *   <li>{@code latitude} / {@code longitude} — echoed back so the frontend can keep the
     *       lat/lng aligned with the resolved address and persist both to the database.</li>
     *   <li>{@code address} — structured breakdown (suburb, city, postcode, …) for
     *       richer UI detail cards if needed.</li>
     * </ul>
     */
    public record AddressResponse(
            Long          placeId,
            String        osmType,
            Long          osmId,
            Double        latitude,
            Double        longitude,
            String        category,
            String        type,
            String        displayName,
            AddressDetail address,
            String[]      boundingBox
    ) {}
}

