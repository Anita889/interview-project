package com.example.companysearch.api;

import java.util.List;

public record CompanyResponse(
        String companyNumber,
        String name,
        String status,
        String type,
        String incorporatedOn,
        String dissolvedOn,
        String registeredOfficeAddress,
        List<String> natureOfBusiness,
        List<OfficerResponse> officers,
        List<SignificantControlResponse> personsWithSignificantControl
) {
}
