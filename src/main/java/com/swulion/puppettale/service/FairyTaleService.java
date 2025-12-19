package com.swulion.puppettale.service;

import com.swulion.puppettale.dto.*;
import com.swulion.puppettale.entity.Child;
import com.swulion.puppettale.entity.FairyTale;
import com.swulion.puppettale.entity.FairyTalePage;
import com.swulion.puppettale.repository.ChildRepository;
import com.swulion.puppettale.repository.FairyTalePageRepository;
import com.swulion.puppettale.repository.FairyTaleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FairyTaleService {
    private final FairyTaleRepository fairyTaleRepository;
    private final ChildRepository childRepository;
    private final FairyTalePageRepository fairyTalePageRepository;

    // 동화 저장
    @Transactional
    public FairyTale saveFairyTale(Long childId, String title, List<FairyTalePageData> pages) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아이 정보를 찾을 수 없습니다."));

        FairyTale fairyTale = new FairyTale();
        fairyTale.setChild(child);

        if(title != null && !title.isBlank()) {
            fairyTale.setTitle(title);
        } else {
            fairyTale.setTitle("제목 없음");
        }

        // 표지=마지막 페이지 이미지
        if (pages != null && !pages.isEmpty()) {
            String lastImage = pages.get(pages.size() - 1).getImageUrl();
            fairyTale.setThumbnailUrl(lastImage);
        }
        fairyTaleRepository.save(fairyTale); // ID 생성

        if (pages != null) {
            int idx = 1;
            for (FairyTalePageData p : pages) {
                FairyTalePage page = new FairyTalePage();
                page.setPageNumber(idx++);
                page.setImageUrl(p.getImageUrl());
                page.setText(p.getText());
                page.setFairyTale(fairyTale);
                fairyTalePageRepository.save(page);
                fairyTale.getPages().add(page);
            }
        }
        return fairyTale;
    }

    // 동화 목록 조회
    @Transactional
    public FairyTaleListResponse getFairyTaleList(Long childId) {

        List<FairyTale> fairyTales =
                fairyTaleRepository.findAllByChildIdAndIsDeletedFalseOrderByCreatedAtDesc(childId);

        List<FairyTaleListItem> items = fairyTales.stream()
                .map(f -> FairyTaleListItem.builder()
                        .id(f.getId())
                        .title(f.getTitle())
                        .thumbnailUrl(f.getThumbnailUrl())
                        .createdAt(f.getCreatedAt().toString())
                        .build())
                .toList();

        return new FairyTaleListResponse(items);
    }

    // 동화 상세 조회
    @Transactional
    public FairyTaleDetailResponse getFairyTaleDetail(Long childId, Long fairyTaleId) {

        FairyTale fairyTale = fairyTaleRepository
                .findByIdAndChildIdAndIsDeletedFalse(fairyTaleId, childId)
                .orElseThrow(() -> new IllegalArgumentException("동화를 찾을 수 없습니다."));

        return FairyTaleDetailResponse.builder()
                .id(fairyTale.getId())
                .title(fairyTale.getTitle())
                .thumbnailUrl(fairyTale.getThumbnailUrl())
                .pages(
                        fairyTale.getPages().stream()
                                .sorted(Comparator.comparingInt(p -> p.getPageNumber()))
                                .map(p -> FairyTaleDetailResponse.FairyTalePageInfo.builder()
                                        .pageNumber(p.getPageNumber())
                                        .imageUrl(p.getImageUrl())
                                        .text(p.getText())
                                        .build()
                                ).toList()
                )
                .build();
    }

    // 동화 제목 수정
    @Transactional
    public void updateFairyTaleTitle(Long childId, Long fairyTaleId, UpdateFairyTaleTitleRequest request) {
        FairyTale fairyTale = fairyTaleRepository
                .findByIdAndChildIdAndIsDeletedFalse(fairyTaleId, childId)
                .orElseThrow(() -> new IllegalArgumentException("동화를 찾을 수 없습니다."));

        fairyTale.updateTitle(request.getTitle());
    }

    // 동화 삭제
    @Transactional
    public void deleteFairyTale(Long childId, Long fairyTaleId) {

        FairyTale fairyTale = fairyTaleRepository
                .findByIdAndChildIdAndIsDeletedFalse(fairyTaleId, childId)
                .orElseThrow(() -> new IllegalArgumentException("동화를 찾을 수 없습니다."));

        fairyTale.softDelete();
    }

    // 오늘 날짜의 시작 시간
    @Transactional
    public long getTodayStoryCount(Long childId){
        java.time.LocalDateTime startOfToday = java.time.LocalDate.now().atStartOfDay();
        return fairyTaleRepository.countByChildIdAndIsDeletedFalseAndCreatedAtAfter(childId, startOfToday);
    }
}