package com.swulion.puppettale.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "Children")
public class Child extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String name;

    private LocalDate birthdate;

    @Column(name = "hospitalization_start_date")
    private LocalDate hospitalizationStartDate;

    @Column(columnDefinition = "TEXT")
    private String profileImageUrl;

    @OneToOne(mappedBy = "child", fetch = FetchType.LAZY)
    private Puppet puppet;

    @OneToMany(mappedBy = "child", fetch = FetchType.LAZY)
    private List<FairyTale> fairyTales = new ArrayList<>();
}
