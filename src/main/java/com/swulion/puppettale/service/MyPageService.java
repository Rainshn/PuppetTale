package com.swulion.puppettale.service;

import com.swulion.puppettale.dto.MyPageResponse;
import com.swulion.puppettale.entity.Child;
import com.swulion.puppettale.entity.Puppet;
import com.swulion.puppettale.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class MyPageService {
    private final ChildRepository childRepository;

    public MyPageResponse getMyPage(Long childId) {
        // 아동 조회
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        // 퍼펫 조회
        Puppet puppet = child.getPuppet();
        if (puppet == null) {
            throw new IllegalStateException("해당 아동의 퍼펫을 찾을 수 없습니다.");
        }

        // 나이 계산
        Integer age = calculateAge(child.getBirthdate());

        // 입원 일수 계산
        Long hospitalizationDays = calculateHospitalizationDays(child.getHospitalizationStartDate());

        // 화면 표시용 문자열 생성
        String ageText = (age != null)
                ? age + "살"
                : "나이 미상";
        String hospitalizationText = (hospitalizationDays != null)
                ? "입원 " + hospitalizationDays + "일차"
                : "입원 일수 미상";

        return MyPageResponse.builder()
                .name(child.getName())
                .age(ageText)
                .hospitalizationDays(hospitalizationText)
                .profileImageUrl(child.getProfileImageUrl())
                .puppetName(puppet.getName())
                .puppetMode(puppet.getMode())
                .build();
    }

    // 생년월일 기반 나이 계산
    private Integer calculateAge(LocalDate birthdate) {
        if (birthdate == null) return null;
        return Period.between(birthdate, LocalDate.now()).getYears();
    }

    // 입원일 기반 입원 일수 계산
    private Long calculateHospitalizationDays(LocalDate start) {
        if (start == null) return null;
        return ChronoUnit.DAYS.between(start, LocalDate.now()) + 1;
    }
}