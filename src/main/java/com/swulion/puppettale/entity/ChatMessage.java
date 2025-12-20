package com.swulion.puppettale.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ChatMessages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId; // 사용자 식별자

    private Long childId;

    @Enumerated(EnumType.STRING)
    private Speaker speaker; // 발화 주체: USER, AI

    @Column(columnDefinition = "TEXT") // 대화 내용은 길 수 있음
    private String message;

    private LocalDateTime timestamp;

    private LocalDate logDate;

    private String keywords; // 키워드
}