package com.example.companysearch.scraper;

import com.example.companysearch.domain.CompanyEntity;
import com.example.companysearch.domain.OfficerEntity;
import com.example.companysearch.domain.SignificantControlEntity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class CompaniesHouseParser {

    private static final Pattern COMPANY_PATH_PATTERN = Pattern.compile("(?:^|/)company/([A-Z0-9]{2,12})(?:$|[/?#])");
    private static final Set<String> KNOWN_LABELS = Set.of(
            "company number",
            "registered office address",
            "company status",
            "dissolved on",
            "company type",
            "incorporated on",
            "correspondence address",
            "role",
            "date of birth",
            "appointed on",
            "resigned on",
            "nationality",
            "country of residence",
            "notified on",
            "ceased on",
            "nature of control"
    );

    private CompaniesHouseParser() {
    }

    public static List<String> parseCompanyNumbers(Document document, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        LinkedHashSet<String> companyNumbers = new LinkedHashSet<>();

        for (Element link : document.select("a[href]")) {
            Matcher matcher = COMPANY_PATH_PATTERN.matcher(link.attr("href"));
            if (matcher.find()) {
                companyNumbers.add(matcher.group(1));
            }

            if (companyNumbers.size() >= maxResults) {
                break;
            }
        }

        return new ArrayList<>(companyNumbers);
    }

    public static CompanyEntity parseOverview(Document document, String fallbackCompanyNumber) {
        CompanyEntity company = new CompanyEntity();
        company.setCompanyNumber(firstNonBlank(valueForLabel(document, "Company number"), fallbackCompanyNumber));
        company.setName(firstNonBlank(selectFirstText(document, "main h1, h1"), "Unknown company"));
        company.setRegisteredOfficeAddress(valueForLabel(document, "Registered office address"));
        company.setStatus(valueForLabel(document, "Company status"));
        company.setDissolvedOn(valueForLabel(document, "Dissolved on"));
        company.setType(valueForLabel(document, "Company type"));
        company.setIncorporatedOn(valueForLabel(document, "Incorporated on"));
        company.getNatureOfBusiness().addAll(parseNatureOfBusiness(document));
        return company;
    }

    public static List<OfficerEntity> parseOfficers(Document document) {
        List<OfficerEntity> officers = parseStructuredOfficers(document);
        if (!officers.isEmpty()) {
            return officers;
        }
        return parseOfficersFromLines(document);
    }

    public static List<SignificantControlEntity> parsePersonsWithSignificantControl(Document document) {
        List<SignificantControlEntity> persons = parseStructuredPersonsWithSignificantControl(document);
        if (!persons.isEmpty()) {
            return persons;
        }
        return parsePersonsWithSignificantControlFromLines(document);
    }

    private static List<OfficerEntity> parseStructuredOfficers(Document document) {
        List<OfficerEntity> officers = new ArrayList<>();
        for (Element section : sectionsWithLabel(document, "Role", "[id^=appointment-], .appointment")) {
            String name = firstHeadingText(section);
            String role = valueForLabel(section, "Role");
            String appointedOn = valueForLabel(section, "Appointed on");
            if (hasText(name) && (hasText(role) || hasText(appointedOn))) {
                OfficerEntity officer = new OfficerEntity();
                officer.setName(name);
                officer.setRole(role);
                officer.setAppointedOn(appointedOn);
                officers.add(officer);
            }
        }
        return officers;
    }

    private static List<SignificantControlEntity> parseStructuredPersonsWithSignificantControl(Document document) {
        List<SignificantControlEntity> persons = new ArrayList<>();
        List<Element> sections = sectionsWithLabel(
                document,
                "Nature of control",
                "[id^=psc-], [id^=individual-person-with-significant-control-], "
                        + "[id^=corporate-entity-person-with-significant-control-], "
                        + "[id^=corporate-entity-psc], [id^=legal-person-person-with-significant-control-], "
                        + "[id^=legal-person-psc], [id^=super-secure-person-with-significant-control-], "
                        + "[id^=super-secure-psc]"
        );

        for (Element section : sections) {
            String name = firstHeadingText(section);
            List<String> natureOfControl = valuesForLabel(section, "Nature of control");
            if (hasText(name) && !natureOfControl.isEmpty()) {
                SignificantControlEntity person = new SignificantControlEntity();
                person.setName(name);
                person.getNatureOfControl().addAll(natureOfControl);
                persons.add(person);
            }
        }

        return persons;
    }

    private static List<Element> sectionsWithLabel(Document document, String label, String preferredSelector) {
        LinkedHashSet<Element> sections = new LinkedHashSet<>(document.select(preferredSelector));

        for (Element key : document.select("dt, .govuk-summary-list__key")) {
            if (sameLabel(key.text(), label)) {
                Element section = nearestSectionWithHeading(key);
                if (section != null) {
                    sections.add(section);
                }
            }
        }

        return new ArrayList<>(sections);
    }

    private static Element nearestSectionWithHeading(Element element) {
        Element current = element.parent();
        while (current != null) {
            if (current.selectFirst("h2, h3, h4") != null) {
                return current;
            }
            current = current.parent();
        }
        return null;
    }

    private static List<OfficerEntity> parseOfficersFromLines(Document document) {
        List<String> lines = visibleLines(document);
        List<OfficerEntity> officers = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            if (sameLabel(lines.get(index), "Role")) {
                String name = findPersonNameBefore(lines, index);
                String role = valueAfterLabel(lines, index);
                String appointedOn = findValueAfterLabel(lines, "Appointed on", index + 1);

                if (hasText(name) && (hasText(role) || hasText(appointedOn))) {
                    OfficerEntity officer = new OfficerEntity();
                    officer.setName(name);
                    officer.setRole(role);
                    officer.setAppointedOn(appointedOn);
                    officers.add(officer);
                }
            }
        }

        return officers;
    }

    private static List<SignificantControlEntity> parsePersonsWithSignificantControlFromLines(Document document) {
        List<String> lines = visibleLines(document);
        List<SignificantControlEntity> persons = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            if (sameLabel(lines.get(index), "Nature of control")) {
                String name = findPersonNameBefore(lines, index);
                List<String> controls = valuesAfterLabel(lines, index);

                if (hasText(name) && !controls.isEmpty()) {
                    SignificantControlEntity person = new SignificantControlEntity();
                    person.setName(name);
                    person.getNatureOfControl().addAll(controls);
                    persons.add(person);
                }
            }
        }

        return persons;
    }

    private static List<String> parseNatureOfBusiness(Document document) {
        LinkedHashSet<String> values = new LinkedHashSet<>();

        Element heading = findHeading(document, "Nature of business");
        if (heading != null) {
            Element sibling = heading.nextElementSibling();
            while (sibling != null && !sibling.tagName().matches("h[1-6]")) {
                for (Element listItem : sibling.select("li")) {
                    String text = clean(listItem.text());
                    if (hasText(text)) {
                        values.add(text);
                    }
                }
                sibling = sibling.nextElementSibling();
            }
        }

        if (values.isEmpty()) {
            List<String> lines = visibleLines(document);
            for (int index = 0; index < lines.size(); index++) {
                if (lines.get(index).toLowerCase(Locale.ROOT).startsWith("nature of business")) {
                    values.addAll(valuesAfterLabel(lines, index));
                    break;
                }
            }
        }

        return new ArrayList<>(values);
    }

    private static String valueForLabel(Element root, String label) {
        List<String> values = valuesForLabel(root, label);
        return values.isEmpty() ? null : String.join("; ", values);
    }

    private static List<String> valuesForLabel(Element root, String label) {
        for (Element key : root.select("dt, .govuk-summary-list__key")) {
            if (sameLabel(key.text(), label)) {
                Element value = nextValueElement(key);
                if (value != null) {
                    List<String> values = valuesFromElement(value);
                    if (!values.isEmpty()) {
                        return values;
                    }
                }
            }
        }

        List<String> lines = visibleLines(root);
        for (int index = 0; index < lines.size(); index++) {
            if (sameLabel(lines.get(index), label)) {
                return valuesAfterLabel(lines, index);
            }

            String normalizedLine = normalizeLabel(lines.get(index));
            String normalizedLabel = normalizeLabel(label);
            if (normalizedLine.startsWith(normalizedLabel + " ")) {
                String value = clean(lines.get(index)
                        .replaceFirst("(?i)^" + Pattern.quote(label) + "\\s*:?", ""));
                return hasText(value) ? List.of(value) : List.of();
            }
        }

        return List.of();
    }

    private static Element nextValueElement(Element key) {
        Element sibling = key.nextElementSibling();
        while (sibling != null) {
            if (sibling.tagName().equals("dd") || sibling.hasClass("govuk-summary-list__value")) {
                return sibling;
            }
            sibling = sibling.nextElementSibling();
        }
        return null;
    }

    private static List<String> valuesFromElement(Element element) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (Element listItem : element.select("li")) {
            String text = clean(listItem.text());
            if (hasText(text)) {
                values.add(text);
            }
        }

        if (values.isEmpty()) {
            values.addAll(visibleLines(element));
        }

        return new ArrayList<>(values);
    }

    private static List<String> valuesAfterLabel(List<String> lines, int labelIndex) {
        List<String> values = new ArrayList<>();

        for (int index = labelIndex + 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (isBoundaryLine(line)) {
                break;
            }
            values.add(line);
        }

        return values;
    }

    private static String valueAfterLabel(List<String> lines, int labelIndex) {
        List<String> values = valuesAfterLabel(lines, labelIndex);
        return values.isEmpty() ? null : values.get(0);
    }

    private static String findValueAfterLabel(List<String> lines, String label, int startIndex) {
        for (int index = startIndex; index < lines.size(); index++) {
            if (sameLabel(lines.get(index), label)) {
                return valueAfterLabel(lines, index);
            }
            if (sameLabel(lines.get(index), "Role") && index > startIndex) {
                return null;
            }
        }
        return null;
    }

    private static String findPersonNameBefore(List<String> lines, int labelIndex) {
        for (int index = labelIndex - 1; index >= 0; index--) {
            if (sameLabel(lines.get(index), "Correspondence address")) {
                return previousUsefulLine(lines, index - 1);
            }
        }
        return previousUsefulLine(lines, labelIndex - 1);
    }

    private static String previousUsefulLine(List<String> lines, int startIndex) {
        for (int index = startIndex; index >= 0; index--) {
            String line = lines.get(index);
            if (!isBoundaryLine(line) && !line.toLowerCase(Locale.ROOT).contains("officers:")
                    && !line.toLowerCase(Locale.ROOT).contains("persons with significant control")) {
                return line;
            }
        }
        return null;
    }

    private static String firstHeadingText(Element root) {
        for (Element heading : root.select("h2, h3, h4")) {
            String text = clean(heading.text());
            if (hasText(text) && !text.toLowerCase(Locale.ROOT).contains("filter officers")
                    && !text.toLowerCase(Locale.ROOT).contains("persons with significant control")) {
                return text;
            }
        }
        return null;
    }

    private static Element findHeading(Element root, String text) {
        String needle = text.toLowerCase(Locale.ROOT);
        for (Element heading : root.select("h1, h2, h3, h4, h5, h6")) {
            if (heading.text().toLowerCase(Locale.ROOT).contains(needle)) {
                return heading;
            }
        }
        return null;
    }

    private static String selectFirstText(Element root, String selector) {
        Element element = root.selectFirst(selector);
        return element == null ? null : clean(element.text());
    }

    private static List<String> visibleLines(Element root) {
        return root.wholeText().lines()
                .map(CompaniesHouseParser::clean)
                .filter(CompaniesHouseParser::hasText)
                .toList();
    }

    private static boolean isBoundaryLine(String line) {
        String normalized = normalizeLabel(line);
        return KNOWN_LABELS.contains(normalized)
                || normalized.startsWith("company overview for")
                || normalized.startsWith("filing history for")
                || normalized.startsWith("people for")
                || normalized.startsWith("more for")
                || normalized.startsWith("accounts")
                || normalized.startsWith("confirmation statement")
                || normalized.startsWith("nature of business")
                || normalized.startsWith("tell us what you think")
                || normalized.startsWith("is there anything wrong")
                || normalized.startsWith("support links")
                || normalized.startsWith("built by");
    }

    private static boolean sameLabel(String actual, String expected) {
        return normalizeLabel(actual).equals(normalizeLabel(expected));
    }

    private static String normalizeLabel(String value) {
        return clean(value)
                .replace(":", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String first, String second) {
        return hasText(first) ? first : second;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
