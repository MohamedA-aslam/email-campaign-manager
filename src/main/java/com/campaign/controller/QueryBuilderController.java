package com.campaign.controller;

import com.campaign.dto.QueryResult;
import com.campaign.service.QueryBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/query-builder")
@RequiredArgsConstructor
public class QueryBuilderController {

    private final QueryBuilderService queryBuilderService;

    @GetMapping
    public String queryBuilderPage(Model model) {
        model.addAttribute("tables", queryBuilderService.getAvailableTables());
        return "query-builder";
    }

    @PostMapping("/execute")
    @ResponseBody
    public ResponseEntity<QueryResult> executeQuery(
            @RequestBody Map<String, String> body) {

        String sql = body.get("sql");
        QueryResult result = queryBuilderService.executeQuery(sql);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/save-mapping")
    @ResponseBody
    public ResponseEntity<Map<String, String>> saveMapping(
            @RequestBody Map<String, String> mapping) {
        queryBuilderService.saveMapping(mapping);
        return ResponseEntity.ok(Map.of("status", "saved"));
    }

    @GetMapping("/current-mapping")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCurrentMapping() {
        return ResponseEntity.ok(queryBuilderService.getCurrentMapping());
    }
}