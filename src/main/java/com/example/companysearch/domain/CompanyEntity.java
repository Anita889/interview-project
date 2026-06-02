package com.example.companysearch.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "company")
public class CompanyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String companyNumber;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(length = 128)
    private String status;

    @Column(name = "company_type", length = 255)
    private String type;

    @Column(length = 128)
    private String incorporatedOn;

    @Column(length = 128)
    private String dissolvedOn;

    @Column(length = 1024)
    private String registeredOfficeAddress;

    @ElementCollection
    @CollectionTable(name = "company_nature_of_business", joinColumns = @JoinColumn(name = "company_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "description", length = 512)
    private List<String> natureOfBusiness = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "company_id", nullable = false)
    @OrderColumn(name = "sort_order")
    private List<OfficerEntity> officers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "company_id", nullable = false)
    @OrderColumn(name = "sort_order")
    private List<SignificantControlEntity> personsWithSignificantControl = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIncorporatedOn() {
        return incorporatedOn;
    }

    public void setIncorporatedOn(String incorporatedOn) {
        this.incorporatedOn = incorporatedOn;
    }

    public String getDissolvedOn() {
        return dissolvedOn;
    }

    public void setDissolvedOn(String dissolvedOn) {
        this.dissolvedOn = dissolvedOn;
    }

    public String getRegisteredOfficeAddress() {
        return registeredOfficeAddress;
    }

    public void setRegisteredOfficeAddress(String registeredOfficeAddress) {
        this.registeredOfficeAddress = registeredOfficeAddress;
    }

    public List<String> getNatureOfBusiness() {
        return natureOfBusiness;
    }

    public void setNatureOfBusiness(List<String> natureOfBusiness) {
        this.natureOfBusiness = natureOfBusiness;
    }

    public List<OfficerEntity> getOfficers() {
        return officers;
    }

    public void setOfficers(List<OfficerEntity> officers) {
        this.officers = officers;
    }

    public List<SignificantControlEntity> getPersonsWithSignificantControl() {
        return personsWithSignificantControl;
    }

    public void setPersonsWithSignificantControl(List<SignificantControlEntity> personsWithSignificantControl) {
        this.personsWithSignificantControl = personsWithSignificantControl;
    }
}
