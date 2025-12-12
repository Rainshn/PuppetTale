package com.swulion.puppettale.repository;

import com.swulion.puppettale.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 특정 sessionId의 모든 대화 기록을 시간 순으로 조회
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
}
