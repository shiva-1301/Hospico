package com.hospitalfinder.backend.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Table(name = "clinic", uniqueConstraints = @UniqueConstraint(columnNames = { "name", "address", "city" }))
@Entity
public class Clinic {
    @Id
    @GeneratedValue
    @Getter
    @Setter
    private Long id;
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String address;
    @Getter
    @Setter
    private String city;
    @Getter
    @Setter
    private Double latitude;
    @Getter
    @Setter
    private Double longitude;
    @ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.MERGE })
    @JoinTable(name = "clinic_specializations", joinColumns = @JoinColumn(name = "clinic_id"), inverseJoinColumns = @JoinColumn(name = "specializations_id") // Changed
                                                                                                                                                             // to
                                                                                                                                                             // specializations_id
                                                                                                                                                             // to
                                                                                                                                                             // match
                                                                                                                                                             // DB
                                                                                                                                                             // schema
    )
    @Getter
    @Setter
    private Collection<Specialization> specializations = new ArrayList<>();
    @Getter
    @Setter
    private String phone;
    @Getter
    @Setter
    private String website;
    @Getter
    @Setter
    private String timings;
    @Getter
    @Setter
    private Double rating;
    @Getter
    @Setter
    private Integer reviews;
    @Getter
    @Setter
    @OneToMany(mappedBy = "clinic", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Doctor> doctors = new ArrayList<>();
    @Getter
    @Setter
    private String imageUrl;
}