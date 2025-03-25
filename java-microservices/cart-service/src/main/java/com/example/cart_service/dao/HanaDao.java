package com.example.cart_service.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class HanaDao {
    private final JdbcTemplate jdbcTemplate;

    public HanaDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes a SQL statement and returns the result as a list of maps.
     */
    public List<Map<String, Object>> exec(String statement) {
        // For SELECT queries, use queryForList.
        // For INSERT/UPDATE/DELETE, you might want to call update() and then return an empty list.
        if (statement.trim().toLowerCase().startsWith("select")) {
            return jdbcTemplate.queryForList(statement);
        } else {
            jdbcTemplate.update(statement);
            return List.of();
        }
    }
}
