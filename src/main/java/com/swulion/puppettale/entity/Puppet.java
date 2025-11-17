package com.swulion.puppettale.entity;

import com.swulion.puppettale.constant.PuppetMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "Puppets")
public class Puppet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", unique = true)
    private Child child;

    private String name;

    @Enumerated(EnumType.STRING)
    private PuppetMode mode;

    public void changeName(String newName) {
        this.name = newName;
    }

    public void changeMode(PuppetMode newMode) {
        this.mode = newMode;
    }
}
