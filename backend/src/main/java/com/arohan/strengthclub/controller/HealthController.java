package com.arohan.strengthclub.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exposes a lightweight liveness check with database connectivity status. */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "AROHAN Strength Club API");
        result.put("timestamp", Instant.now());

        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            result.put("database", "UP");
        } catch (Exception ex) {
            // Keep the diagnostic endpoint available while reporting a degraded database dependency.
            result.put("status", "DEGRADED");
            result.put("database", "DOWN");
        }
        return result;
    }
}
