package com.swulion.puppettale.repository;

import com.swulion.puppettale.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 특정 sessionId의 모든 대화 기록을 시간 순으로 조회
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);

    // 특정 시간 이후의 메시지만 세션별 조회
    List<ChatMessage> findBySessionIdAndTimestampAfterOrderByTimestampAsc(String sessionId, LocalDateTime lastDischargedAt);

    // 아동별 대화 완전히 분리
    List<ChatMessage> findBySessionIdAndChildIdOrderByTimestampAsc(String sessionId, Long childId);
}
