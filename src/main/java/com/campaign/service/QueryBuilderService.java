package com.campaign.service;

import com.campaign.dto.QueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryBuilderService {

    private final JdbcTemplate jdbcTemplate;

    // In-memory mapping store
    // In production this should be persisted to DB
    private Map<String, String> currentMapping = new HashMap<>();
    private String currentSql = "";

    // Allowed tables — security whitelist
    private static final List<String> ALLOWED_TABLES = List.of(
            "recipients", "campaigns", "delivery_logs", "smtp_profile"
    );

    // Blocked keywords — prevent destructive queries
    private static final List<String> BLOCKED_KEYWORDS = List.of(
            "DROP", "DELETE", "TRUNCATE", "INSERT", "UPDATE",
            "ALTER", "CREATE", "REPLACE", "ATTACH", "DETACH"
    );

    public List<String> getAvailableTables() {
        return ALLOWED_TABLES;
    }

    public QueryResult executeQuery(String sql) {
        if (sql == null || sql.isBlank()) {
            return QueryResult.builder()
                    .error("SQL query cannot be empty.")
                    .build();
        }

        String upperSql = sql.trim().toUpperCase();

        // Security: only allow SELECT
        if (!upperSql.startsWith("SELECT")) {
            return QueryResult.builder()
                    .error("Only SELECT queries are allowed.")
                    .build();
        }

        // Security: block dangerous keywords
        for (String keyword : BLOCKED_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                return QueryResult.builder()
                        .error("Query contains blocked keyword: " + keyword)
                        .build();
            }
        }

        // Security: only allow whitelisted tables
        boolean hasAllowedTable = ALLOWED_TABLES.stream()
                .anyMatch(t -> upperSql.contains(t.toUpperCase()));
        if (!hasAllowedTable) {
            return QueryResult.builder()
                    .error("Query must reference one of: " + String.join(", ", ALLOWED_TABLES))
                    .build();
        }

        try {
            // Limit results to 100 rows for safety
            String limitedSql = sql.trim();
            if (!upperSql.contains("LIMIT")) {
                limitedSql = limitedSql + " LIMIT 100";
            }

            currentSql = sql;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(limitedSql);

            if (rows.isEmpty()) {
                return QueryResult.builder()
                        .columns(new ArrayList<>())
                        .rows(new ArrayList<>())
                        .totalRows(0)
                        .build();
            }

            List<String> columns = new ArrayList<>(rows.get(0).keySet());

            return QueryResult.builder()
                    .columns(columns)
                    .rows(rows)
                    .totalRows(rows.size())
                    .build();

        } catch (Exception e) {
            log.error("Query execution error: {}", e.getMessage());
            return QueryResult.builder()
                    .error("SQL Error: " + e.getMessage())
                    .build();
        }
    }

    public void saveMapping(Map<String, String> mapping) {
        this.currentMapping = new HashMap<>(mapping);
        log.info("Variable mapping saved: {}", mapping);
    }

    public Map<String, Object> getCurrentMapping() {
        return Map.of(
                "mapping", currentMapping,
                "sql", currentSql
        );
    }

    /**
     * Applies the saved mapping to personalize content for a recipient.
     * Called by CampaignService when sending emails.
     */
    public String applyMapping(String content, Map<String, Object> recipientRow) {
        if (currentMapping.isEmpty() || recipientRow == null) return content;

        String result = content;
        for (Map.Entry<String, String> entry : currentMapping.entrySet()) {
            String column   = entry.getKey();   // e.g. "name"
            String variable = entry.getValue(); // e.g. "{FirstName}"
            Object value    = recipientRow.get(column);
            if (value != null) {
                result = result.replace(variable, value.toString());
            }
        }
        return result;
    }

    /**
     * Fetches a single recipient row from the query results by email.
     */
    public Map<String, Object> getRowByEmail(String email) {
        if (currentSql == null || currentSql.isBlank()) return null;
        try {
            String sql = currentSql.trim();
            String upper = sql.toUpperCase();
            String lookupSql = upper.contains("WHERE")
                    ? sql + " AND email = ?"
                    : sql + " WHERE email = ?";
            lookupSql = lookupSql + " LIMIT 1";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(lookupSql, email);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.warn("Could not fetch row for email {}: {}", email, e.getMessage());
            return null;
        }
    }
}