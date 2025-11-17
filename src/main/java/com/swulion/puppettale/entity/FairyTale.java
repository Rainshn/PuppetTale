package com.swulion.puppettale.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "FairyTales")
public class FairyTale extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    private Child child;

    private String title;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @OneToMany(mappedBy = "fairyTale", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FairyTalePage> pages = new ArrayList<>();

    public void updateTitle(String title) {
        this.title = title;
    }

    public void softDelete() {
        this.isDeleted = true;
    }
}