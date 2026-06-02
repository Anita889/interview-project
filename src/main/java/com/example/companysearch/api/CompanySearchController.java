package com.example.companysearch.api;

import com.example.companysearch.service.CompanySearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanySearchController {

    private final CompanySearchService companySearchService;

    public CompanySearchController(CompanySearchService companySearchService) {
        this.companySearchService = companySearchService;
    }

    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam String query,
            @RequestParam(defaultValue = "false") boolean forceRefresh
    ) {
        return companySearchService.search(query, forceRefresh);
    }
}
