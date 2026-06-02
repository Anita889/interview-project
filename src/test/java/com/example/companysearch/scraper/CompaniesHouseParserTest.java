package com.example.companysearch.scraper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.companysearch.domain.CompanyEntity;
import com.example.companysearch.domain.OfficerEntity;
import com.example.companysearch.domain.SignificantControlEntity;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class CompaniesHouseParserTest {

    @Test
    void parsesSearchResultsInOrderAndRemovesDuplicates() {
        String html = """
                <main>
                  <a href="/company/14367667">OPENAI UK LTD</a>
                  <a href="/company/14367667">OPENAI UK LTD duplicate link</a>
                  <a href="/company/14595544">OPENAI LEARNING &amp; EDUCATION LIMITED</a>
                  <a href="/officers/foo">Officer result</a>
                </main>
                """;

        List<String> numbers = CompaniesHouseParser.parseCompanyNumbers(Jsoup.parse(html), 100);

        assertThat(numbers).containsExactly("14367667", "14595544");
    }

    @Test
    void respectsConfiguredSearchLimit() {
        String html = """
                <main>
                  <a href="/company/11111111">One</a>
                  <a href="/company/22222222">Two</a>
                  <a href="/company/33333333">Three</a>
                </main>
                """;

        List<String> numbers = CompaniesHouseParser.parseCompanyNumbers(Jsoup.parse(html), 2);

        assertThat(numbers).containsExactly("11111111", "22222222");
    }

    @Test
    void parsesOverviewFields() {
        String html = """
                <main>
                  <h1>OPENAI UK LTD</h1>
                  <p>Company number 14367667</p>
                  <dl>
                    <dt>Registered office address</dt>
                    <dd>Suite 1, 7th Floor 50 Broadway, London, United Kingdom, SW1H 0BL</dd>
                    <dt>Company status</dt>
                    <dd>Active</dd>
                    <dt>Company type</dt>
                    <dd>Private limited Company</dd>
                    <dt>Incorporated on</dt>
                    <dd>21 September 2022</dd>
                  </dl>
                  <h2>Nature of business (SIC)</h2>
                  <ul>
                    <li>63990 - Other information service activities not elsewhere classified</li>
                  </ul>
                </main>
                """;

        CompanyEntity company = CompaniesHouseParser.parseOverview(Jsoup.parse(html), "fallback");

        assertThat(company.getCompanyNumber()).isEqualTo("14367667");
        assertThat(company.getName()).isEqualTo("OPENAI UK LTD");
        assertThat(company.getStatus()).isEqualTo("Active");
        assertThat(company.getType()).isEqualTo("Private limited Company");
        assertThat(company.getIncorporatedOn()).isEqualTo("21 September 2022");
        assertThat(company.getRegisteredOfficeAddress()).contains("50 Broadway");
        assertThat(company.getNatureOfBusiness()).containsExactly(
                "63990 - Other information service activities not elsewhere classified"
        );
    }

    @Test
    void parsesOfficers() {
        String html = """
                <main>
                  <div id="appointment-1">
                    <h2><a href="/officers/example">DOE, Jane</a></h2>
                    <dl>
                      <dt>Role</dt>
                      <dd>Director</dd>
                      <dt>Appointed on</dt>
                      <dd>1 January 2024</dd>
                    </dl>
                  </div>
                </main>
                """;

        List<OfficerEntity> officers = CompaniesHouseParser.parseOfficers(Jsoup.parse(html));

        assertThat(officers).hasSize(1);
        assertThat(officers.get(0).getName()).isEqualTo("DOE, Jane");
        assertThat(officers.get(0).getRole()).isEqualTo("Director");
        assertThat(officers.get(0).getAppointedOn()).isEqualTo("1 January 2024");
    }

    @Test
    void parsesPersonsWithSignificantControl() {
        String html = """
                <main>
                  <div id="psc-1">
                    <h2>Jane Doe</h2>
                    <dl>
                      <dt>Nature of control</dt>
                      <dd>
                        <ul>
                          <li>Ownership of shares - 75% or more</li>
                          <li>Ownership of voting rights - 75% or more</li>
                        </ul>
                      </dd>
                    </dl>
                  </div>
                </main>
                """;

        List<SignificantControlEntity> persons = CompaniesHouseParser.parsePersonsWithSignificantControl(Jsoup.parse(html));

        assertThat(persons).hasSize(1);
        assertThat(persons.get(0).getName()).isEqualTo("Jane Doe");
        assertThat(persons.get(0).getNatureOfControl()).containsExactly(
                "Ownership of shares - 75% or more",
                "Ownership of voting rights - 75% or more"
        );
    }
}
