package com.swulion.puppettale.repository;

import com.swulion.puppettale.entity.Child;
import com.swulion.puppettale.entity.FairyTale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FairyTaleRepository extends JpaRepository<FairyTale, Long> {
    // 상세 조회에서 사용
    Optional<FairyTale> findByIdAndChildIdAndIsDeletedFalse(Long id, Long childId);

    // 목록 조회에서 사용
    List<FairyTale> findAllByChildIdAndIsDeletedFalseOrderByCreatedAtDesc(Long childId);

    // 오늘 생성된 동화 개수 카운트 (삭제된 것 제외)
    long countByChildIdAndIsDeletedFalseAndCreatedAtAfter(Long childId, java.time.LocalDateTime startOfDay);
}
