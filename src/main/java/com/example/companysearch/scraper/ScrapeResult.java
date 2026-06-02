package com.example.companysearch.scraper;

import com.example.companysearch.domain.CompanyEntity;
import java.util.List;

public record ScrapeResult(
        List<CompanyEntity> companies,
        List<String> warnings
) {
}
