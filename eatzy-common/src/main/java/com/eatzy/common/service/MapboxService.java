package com.eatzy.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MapboxService {

    private final RestTemplate restTemplate;

    @Value("${mapbox.access.token}")
    private String accessToken;

    public MapboxService() {
        this.restTemplate = new RestTemplate();
    }

    @SuppressWarnings("unchecked")
    public BigDecimal getDrivingDistance(BigDecimal startLat, BigDecimal startLon, BigDecimal endLat, BigDecimal endLon) {
        if (startLat == null || startLon == null || endLat == null || endLon == null) {
            return null;
        }

        // Bỏ qua gọi API Mapbox nếu 2 tọa độ trùng nhau
        if (startLat.compareTo(endLat) == 0 && startLon.compareTo(endLon) == 0) {
            return BigDecimal.ZERO;
        }

        try {
            String url = UriComponentsBuilder.fromUriString("https://api.mapbox.com/directions/v5/mapbox/driving/")
                .path(startLon + "," + startLat + ";" + endLon + "," + endLat)
                .queryParam("access_token", accessToken)
                .queryParam("geometries", "geojson")
                .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("routes")) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (!routes.isEmpty()) {
                    Double distanceInMeters = (Double) routes.get(0).get("distance");
                    // Convert to km: meters / 1000
                    return BigDecimal.valueOf(distanceInMeters / 1000.0);
                }
            }
        } catch (Exception e) {
            log.error("Error calling Mapbox API", e);
        }

        return null;
    }
}
