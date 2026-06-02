package com.example.companysearch.api;

import java.util.List;

public record SignificantControlResponse(
        String name,
        List<String> natureOfControl
) {
}
