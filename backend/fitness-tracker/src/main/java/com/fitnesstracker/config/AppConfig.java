package com.fitnesstracker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private Security security = new Security();
    private Location location = new Location();
    private Websocket websocket = new Websocket();
    private Cors cors = new Cors();

    @Data
    public static class Security {
        private Jwt jwt = new Jwt();

        @Data
        public static class Jwt {
            private String secret;
            private Long expiration;
            private Long refreshExpiration;
        }
    }

    @Data
    public static class Location {
        private Integer maxAgeMinutes;
        private Double defaultRadiusMeters;
        private Double maxRadiusMeters;
        private Integer batchSize;
    }

    @Data
    public static class Websocket {
        private Integer heartbeatInterval;
        private Integer maxConnectionsPerUser;
        private String[] allowedOrigins;
    }

    @Data
    public static class Cors {
        private String[] allowedOrigins;
        private String[] allowedMethods;
        private String[] allowedHeaders;
        private Boolean allowCredentials;
        private Long maxAge;
    }
}
