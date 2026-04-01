package com.eatzy.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

/**
 * Service to fetch weather data from OpenWeatherMap API WITHOUT Redis caching.
 * Used for dynamic pricing - delivery fees increase during bad weather.
 */
@Service
@Slf4j
public class WeatherService {

    @Value("${openweathermap.api-key:}")
    private String apiKey;

    @Value("${openweathermap.base-url:https://api.openweathermap.org/data/2.5}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WeatherService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get current weather for a location from OpenWeatherMap API.
     * 
     * @param latitude  Location latitude
     * @param longitude Location longitude
     * @return WeatherData object containing weather info, or null if API call fails
     */
    public WeatherData getCurrentWeather(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            log.warn("Cannot get weather: latitude or longitude is null");
            return null;
        }

        try {
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/weather")
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric") // Temperature in Celsius
                    .toUriString();

            log.debug("Fetching weather from OpenWeatherMap: lat={}, lon={}", latitude, longitude);
            String response = restTemplate.getForObject(url, String.class);

            if (response == null) {
                log.warn("OpenWeatherMap API returned null response");
                return null;
            }

            // Parse response
            JsonNode root = objectMapper.readTree(response);
            WeatherData weatherData = parseWeatherResponse(root);

            log.info("Weather fetched: {} (code: {}) for location ({}, {})",
                    weatherData.getDescription(), weatherData.getWeatherCode(), latitude, longitude);

            return weatherData;

        } catch (Exception e) {
            log.error("Failed to fetch weather from OpenWeatherMap: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get weather-based surge multiplier for delivery fee calculation.
     * 
     * @param latitude  Location latitude
     * @param longitude Location longitude
     * @return Surge multiplier (1.0 to 1.5)
     */
    public BigDecimal getWeatherMultiplier(BigDecimal latitude, BigDecimal longitude) {
        WeatherData weather = getCurrentWeather(latitude, longitude);

        if (weather == null) {
            log.warn("Could not get weather data, using default multiplier 1.0");
            return BigDecimal.ONE;
        }

        int code = weather.getWeatherCode();
        BigDecimal multiplier;

        // Weather code mapping based on OpenWeatherMap API:
        // https://openweathermap.org/weather-conditions
        if (code >= 200 && code < 300) {
            // Thunderstorm
            multiplier = new BigDecimal("1.5");
        } else if (code >= 300 && code < 400) {
            // Drizzle
            multiplier = new BigDecimal("1.1");
        } else if (code >= 500 && code < 505) {
            // Light to moderate rain (500-504)
            multiplier = new BigDecimal("1.2");
        } else if (code >= 505 && code < 600) {
            // Heavy rain, shower rain (505-531)
            multiplier = new BigDecimal("1.4");
        } else if (code >= 600 && code < 700) {
            // Snow
            multiplier = new BigDecimal("1.3");
        } else if (code >= 700 && code < 800) {
            // Atmosphere (mist, fog, etc.)
            multiplier = new BigDecimal("1.1");
        } else {
            // Clear sky (800) or Clouds (801-804)
            multiplier = BigDecimal.ONE;
        }

        log.debug("Weather multiplier for code {}: {}", code, multiplier);
        return multiplier;
    }

    /**
     * Parse OpenWeatherMap API response into WeatherData object
     */
    private WeatherData parseWeatherResponse(JsonNode root) {
        WeatherData data = new WeatherData();

        // Get weather array (first element)
        JsonNode weatherArray = root.get("weather");
        if (weatherArray != null && weatherArray.isArray() && !weatherArray.isEmpty()) {
            JsonNode weather = weatherArray.get(0);
            data.setWeatherCode(weather.get("id").asInt());
            data.setMain(weather.get("main").asText());
            data.setDescription(weather.get("description").asText());
        }

        // Get main temperature data
        JsonNode main = root.get("main");
        if (main != null) {
            data.setTemperature(main.get("temp").asDouble());
            data.setHumidity(main.get("humidity").asInt());
        }

        // Get wind data
        JsonNode wind = root.get("wind");
        if (wind != null) {
            data.setWindSpeed(wind.get("speed").asDouble());
        }

        return data;
    }

    /**
     * Weather data holder class
     */
    public static class WeatherData implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private int weatherCode;
        private String main;
        private String description;
        private double temperature;
        private int humidity;
        private double windSpeed;

        // Getters and Setters
        public int getWeatherCode() {
            return weatherCode;
        }

        public void setWeatherCode(int weatherCode) {
            this.weatherCode = weatherCode;
        }

        public String getMain() {
            return main;
        }

        public void setMain(String main) {
            this.main = main;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getHumidity() {
            return humidity;
        }

        public void setHumidity(int humidity) {
            this.humidity = humidity;
        }

        public double getWindSpeed() {
            return windSpeed;
        }

        public void setWindSpeed(double windSpeed) {
            this.windSpeed = windSpeed;
        }

        @Override
        public String toString() {
            return "WeatherData{" +
                    "code=" + weatherCode +
                    ", main='" + main + '\'' +
                    ", desc='" + description + '\'' +
                    ", temp=" + temperature +
                    "}";
        }
    }
}
