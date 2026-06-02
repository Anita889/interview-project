package com.example.companysearch.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "significant_control")
public class SignificantControlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String name;

    @ElementCollection
    @CollectionTable(name = "significant_control_nature", joinColumns = @JoinColumn(name = "significant_control_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "description", length = 512)
    private List<String> natureOfControl = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getNatureOfControl() {
        return natureOfControl;
    }

    public void setNatureOfControl(List<String> natureOfControl) {
        this.natureOfControl = natureOfControl;
    }
}
