package com.eatzy.restaurant.designpattern.adapter;

import java.math.BigDecimal;

/**
 * ★ DESIGN PATTERN #5: Adapter Pattern
 * 
 * Interface chuan cua he thong Eatzy de truy van thong tin vi tri.
 * Bat ke dung Google Maps, Mapbox, hay Here Maps phia sau,
 * code trong service chi can goi LocationService.calculateDistance().
 */
public interface LocationService {
    /**
     * Tinh khoang cach giua 2 toa do (theo km).
     */
    BigDecimal calculateDistance(BigDecimal fromLat, BigDecimal fromLng,
                                 BigDecimal toLat, BigDecimal toLng);

    /**
     * Lay ten dia chi tu toa do (Reverse Geocoding).
     */
    String getAddressFromCoordinates(BigDecimal latitude, BigDecimal longitude);
}
