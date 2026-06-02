package com.example.companysearch.service;

import com.example.companysearch.api.CompanyResponse;
import com.example.companysearch.api.OfficerResponse;
import com.example.companysearch.api.SearchResponse;
import com.example.companysearch.api.SignificantControlResponse;
import com.example.companysearch.domain.CompanyEntity;
import com.example.companysearch.domain.OfficerEntity;
import com.example.companysearch.domain.SearchCacheEntity;
import com.example.companysearch.domain.SignificantControlEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public SearchResponse toSearchResponse(SearchCacheEntity cache, boolean cached, List<String> warnings) {
        List<CompanyResponse> companies = cache.getCompanies().stream()
                .map(this::toCompanyResponse)
                .toList();

        return new SearchResponse(
                cache.getOriginalQuery(),
                cached,
                cache.getFetchedAt(),
                companies.size(),
                List.copyOf(warnings),
                companies
        );
    }

    private CompanyResponse toCompanyResponse(CompanyEntity company) {
        return new CompanyResponse(
                company.getCompanyNumber(),
                company.getName(),
                company.getStatus(),
                company.getType(),
                company.getIncorporatedOn(),
                company.getDissolvedOn(),
                company.getRegisteredOfficeAddress(),
                List.copyOf(company.getNatureOfBusiness()),
                company.getOfficers().stream().map(this::toOfficerResponse).toList(),
                company.getPersonsWithSignificantControl().stream()
                        .map(this::toSignificantControlResponse)
                        .toList()
        );
    }

    private OfficerResponse toOfficerResponse(OfficerEntity officer) {
        return new OfficerResponse(
                officer.getName(),
                officer.getRole(),
                officer.getAppointedOn()
        );
    }

    private SignificantControlResponse toSignificantControlResponse(SignificantControlEntity person) {
        return new SignificantControlResponse(
                person.getName(),
                List.copyOf(person.getNatureOfControl())
        );
    }
}
