package com.swulion.puppettale.service;

import com.swulion.puppettale.entity.ChatMessage;
import com.swulion.puppettale.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DischargeService {

    private final ChatMessageRepository chatMessageRepository;

    // 특정 세션의 모든 대화 기록을 조회하여 동화책 생성 준비
    @Transactional(readOnly = true)
    public List<ChatMessage> getConversationHistory(String sessionId) {
        // 모든 대화 기록을 시간 순으로 조회
        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);

        if (history.isEmpty()) {
            throw new IllegalArgumentException("해당 세션 ID(" + sessionId + ")에 대한 대화 기록이 존재하지 않습니다.");
        }

        /* 추후 여기서 동화책 생성 로직 (요약, 키워드 추출 등)이 추가 */
        return history;
    }

    /* 추후 동화책 생성 후 세션 완료 상태를 처리하는 로직을 추가*/
}