package com.example.companysearch.api;

import java.time.Instant;
import java.util.List;

public record SearchResponse(
        String query,
        boolean cached,
        Instant fetchedAt,
        int count,
        List<String> warnings,
        List<CompanyResponse> companies
) {
}
