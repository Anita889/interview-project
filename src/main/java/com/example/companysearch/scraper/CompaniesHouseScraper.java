package com.example.companysearch.scraper;

import com.example.companysearch.config.CompaniesHouseProperties;
import com.example.companysearch.domain.CompanyEntity;
import com.example.companysearch.domain.OfficerEntity;
import com.example.companysearch.domain.SignificantControlEntity;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CompaniesHouseScraper {

    private final CompaniesHouseProperties properties;
    private Instant lastRequestAt = Instant.EPOCH;

    public CompaniesHouseScraper(CompaniesHouseProperties properties) {
        this.properties = properties;
    }

    public ScrapeResult search(String query) {
        Document searchDocument = fetch(searchUrl(query));
        List<String> companyNumbers = CompaniesHouseParser.parseCompanyNumbers(
                searchDocument,
                properties.getMaxCompaniesPerQuery()
        );
        List<CompanyEntity> companies = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (String companyNumber : companyNumbers) {
            try {
                CompanyEntity company = CompaniesHouseParser.parseOverview(
                        fetch(companyUrl(companyNumber)),
                        companyNumber
                );
                company.getOfficers().addAll(fetchOfficers(companyNumber, warnings));
                company.getPersonsWithSignificantControl().addAll(fetchPersonsWithSignificantControl(companyNumber, warnings));
                companies.add(company);
            } catch (ScrapingException exception) {
                warnings.add("Skipped company " + companyNumber + ": " + exception.getMessage());
            }
        }

        return new ScrapeResult(companies, warnings);
    }

    private List<OfficerEntity> fetchOfficers(
            String companyNumber,
            List<String> warnings
    ) {
        try {
            return CompaniesHouseParser.parseOfficers(fetch(companyUrl(companyNumber) + "/officers"));
        } catch (ScrapingException exception) {
            warnings.add("Could not fetch officers for " + companyNumber + ": " + exception.getMessage());
            return List.of();
        }
    }

    private List<SignificantControlEntity> fetchPersonsWithSignificantControl(
            String companyNumber,
            List<String> warnings
    ) {
        try {
            return CompaniesHouseParser.parsePersonsWithSignificantControl(
                    fetch(companyUrl(companyNumber) + "/persons-with-significant-control")
            );
        } catch (ScrapingException exception) {
            warnings.add("Could not fetch persons with significant control for " + companyNumber + ": " + exception.getMessage());
            return List.of();
        }
    }

    private Document fetch(String url) {
        waitForRequestSlot();
        try {
            return Jsoup.connect(url)
                    .userAgent(properties.getUserAgent())
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-GB,en;q=0.9")
                    .timeout(Math.toIntExact(properties.getTimeout().toMillis()))
                    .get();
        } catch (HttpStatusException exception) {
            throw new ScrapingException("Companies House returned HTTP " + exception.getStatusCode(), exception);
        } catch (IOException exception) {
            throw new ScrapingException("Could not read Companies House page", exception);
        }
    }

    private synchronized void waitForRequestSlot() {
        Duration delay = properties.getRequestDelay();
        if (!lastRequestAt.equals(Instant.EPOCH)) {
            Duration elapsed = Duration.between(lastRequestAt, Instant.now());
            if (elapsed.compareTo(delay) < 0) {
                sleep(delay.minus(elapsed));
            }
        }
        lastRequestAt = Instant.now();
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ScrapingException("Interrupted while respecting request delay", exception);
        }
    }

    private String searchUrl(String query) {
        return UriComponentsBuilder.fromUri(properties.getBaseUrl())
                .path("/search/companies")
                .queryParam("q", query)
                .build()
                .encode()
                .toUriString();
    }

    private String companyUrl(String companyNumber) {
        return UriComponentsBuilder.fromUri(properties.getBaseUrl())
                .pathSegment("company", companyNumber)
                .build()
                .toUriString();
    }
}
