package com.example.companysearch.service;

import com.example.companysearch.api.SearchResponse;
import com.example.companysearch.config.CompaniesHouseProperties;
import com.example.companysearch.domain.SearchCacheEntity;
import com.example.companysearch.repository.SearchCacheRepository;
import com.example.companysearch.scraper.CompaniesHouseScraper;
import com.example.companysearch.scraper.ScrapeResult;
import com.example.companysearch.scraper.ScrapingException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanySearchService {

    private static final int MAX_QUERY_LENGTH = 200;

    private final SearchCacheRepository searchCacheRepository;
    private final CompaniesHouseScraper companiesHouseScraper;
    private final CompaniesHouseProperties properties;
    private final CompanyMapper companyMapper;

    public CompanySearchService(
            SearchCacheRepository searchCacheRepository,
            CompaniesHouseScraper companiesHouseScraper,
            CompaniesHouseProperties properties,
            CompanyMapper companyMapper
    ) {
        this.searchCacheRepository = searchCacheRepository;
        this.companiesHouseScraper = companiesHouseScraper;
        this.properties = properties;
        this.companyMapper = companyMapper;
    }

    @Transactional
    public SearchResponse search(String query, boolean forceRefresh) {
        String trimmedQuery = validateAndTrim(query);
        String normalizedQuery = normalize(trimmedQuery);
        Instant now = Instant.now();

        SearchCacheEntity existingCache = searchCacheRepository.findByNormalizedQuery(normalizedQuery)
                .orElse(null);

        if (existingCache != null && !forceRefresh && isFresh(existingCache, now)) {
            return companyMapper.toSearchResponse(existingCache, true, List.of());
        }

        ScrapeResult scrapeResult;
        try {
            scrapeResult = companiesHouseScraper.search(trimmedQuery);
        } catch (ScrapingException exception) {
            if (existingCache != null && !forceRefresh) {
                return companyMapper.toSearchResponse(
                        existingCache,
                        true,
                        List.of("Returned stale cache because refresh failed: " + exception.getMessage())
                );
            }
            throw exception;
        }

        SearchCacheEntity cache = existingCache == null ? new SearchCacheEntity() : existingCache;
        cache.setOriginalQuery(trimmedQuery);
        cache.setNormalizedQuery(normalizedQuery);
        cache.setFetchedAt(now);
        cache.getCompanies().clear();
        cache.getCompanies().addAll(scrapeResult.companies());

        SearchCacheEntity saved = searchCacheRepository.save(cache);
        return companyMapper.toSearchResponse(saved, false, scrapeResult.warnings());
    }

    private boolean isFresh(SearchCacheEntity cache, Instant now) {
        return cache.getFetchedAt().plus(properties.getCacheTtl()).isAfter(now);
    }

    private String validateAndTrim(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new InvalidSearchRequestException("query must not be blank");
        }

        String trimmedQuery = query.trim();
        if (trimmedQuery.length() > MAX_QUERY_LENGTH) {
            throw new InvalidSearchRequestException("query must be 200 characters or fewer");
        }

        return trimmedQuery;
    }

    private String normalize(String query) {
        return query.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
