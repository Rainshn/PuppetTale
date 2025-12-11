package com.swulion.puppettale.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "FairyTalePages")
public class FairyTalePage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fairytale_id")
    private FairyTale fairyTale;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String text;
}
