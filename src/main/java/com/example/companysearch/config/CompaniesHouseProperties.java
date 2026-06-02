package com.example.companysearch.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "companies-house")
public class CompaniesHouseProperties {

    private URI baseUrl = URI.create("https://find-and-update.company-information.service.gov.uk");
    private String userAgent = "CompanyDataSearchService/1.0 (interview project; contact: example@example.com)";
    private Duration requestDelay = Duration.ofMillis(500);
    private Duration timeout = Duration.ofSeconds(15);
    private Duration cacheTtl = Duration.ofHours(24);
    private int maxCompaniesPerQuery = 100;

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Duration getRequestDelay() {
        return requestDelay;
    }

    public void setRequestDelay(Duration requestDelay) {
        this.requestDelay = requestDelay;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public int getMaxCompaniesPerQuery() {
        return maxCompaniesPerQuery;
    }

    public void setMaxCompaniesPerQuery(int maxCompaniesPerQuery) {
        this.maxCompaniesPerQuery = maxCompaniesPerQuery;
    }
}
