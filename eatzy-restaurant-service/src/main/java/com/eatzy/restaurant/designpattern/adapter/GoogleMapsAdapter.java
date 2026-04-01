package com.eatzy.restaurant.designpattern.adapter;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * ★ DESIGN PATTERN #5: Adapter Pattern
 * 
 * GoogleMapsAdapter chuyen doi API cua Google Maps (gia lap) thanh interface LocationService
 * ma he thong Eatzy mong muon.
 * 
 * Trong thuc te:
 *   - API cua Google Maps co format rieng (JSON phuc tap, key khac nhau)
 *   - API cua Mapbox co format hoan toan khac
 *   - Adapter se "dich" tat ca ve chung 1 interface chuan (LocationService)
 * 
 * Khi can doi tu Google Maps sang Mapbox:
 *   - Tao MapboxAdapter implements LocationService
 *   - Doi @Component sang class moi
 *   - KHONG CAN SUA bat ky dong code nao trong cac Service dang goi LocationService
 */
@Component
@Slf4j
public class GoogleMapsAdapter implements LocationService {

    @Override
    public BigDecimal calculateDistance(BigDecimal fromLat, BigDecimal fromLng,
                                        BigDecimal toLat, BigDecimal toLng) {
        // Gia lap: Tinh khoang cach theo cong thuc Haversine (don gian hoa)
        log.info("🗺️ [ADAPTER - Google Maps] Calculating distance from ({},{}) to ({},{})",
                fromLat, fromLng, toLat, toLng);

        double lat1 = fromLat.doubleValue();
        double lon1 = fromLng.doubleValue();
        double lat2 = toLat.doubleValue();
        double lon2 = toLng.doubleValue();

        // Haversine formula
        double earthRadius = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;

        BigDecimal result = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
        log.info("🗺️ [ADAPTER - Google Maps] Distance: {} km", result);
        return result;
    }

    @Override
    public String getAddressFromCoordinates(BigDecimal latitude, BigDecimal longitude) {
        // Gia lap reverse geocoding
        log.info("🗺️ [ADAPTER - Google Maps] Reverse geocoding for ({}, {})", latitude, longitude);
        return "Simulated Address from Google Maps for (" + latitude + ", " + longitude + ")";
    }
}
