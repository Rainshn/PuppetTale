package com.swulion.puppettale.repository;

import com.swulion.puppettale.entity.FairyTale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FairyTaleRepository extends JpaRepository<FairyTale, Long> {
    // 상세 조회에서 사용
    Optional<FairyTale> findByIdAndChildIdAndIsDeletedFalse(Long id, Long childId);

    // 목록 조회에서 사용
    List<FairyTale> findAllByChildIdAndIsDeletedFalseOrderByCreatedAtDesc(Long childId);
}
