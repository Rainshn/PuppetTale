package com.swulion.puppettale.service;

import com.swulion.puppettale.repository.ChildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final ChildRepository childRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void resetWarningStates() {
        childRepository.resetAllWarningStates();
        log.info("모든 아이들의 Red Flag 경고 상태가 초기화되었습니다.");
    }
}