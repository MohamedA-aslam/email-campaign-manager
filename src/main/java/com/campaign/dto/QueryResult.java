package com.campaign.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class QueryResult {
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private String error;
    private int totalRows;
}