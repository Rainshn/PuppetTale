package com.swulion.puppettale.service;

import com.swulion.puppettale.constant.PuppetMode;
import com.swulion.puppettale.dto.UpdatePuppetModeRequest;
import com.swulion.puppettale.dto.UpdatePuppetNameRequest;
import com.swulion.puppettale.entity.Child;
import com.swulion.puppettale.entity.Puppet;
import com.swulion.puppettale.repository.ChildRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PuppetService {
    private final ChildRepository childRepository;

    // 퍼펫 이름 수정
    @Transactional
    public void updatePuppetName(Long childId, UpdatePuppetNameRequest request) {

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        Puppet puppet = child.getPuppet();
        if (puppet == null) {
            throw new IllegalStateException("해당 아동의 퍼펫을 찾을 수 없습니다.");
        }

        puppet.changeName(request.getPuppetName());
    }

    // 퍼펫 모드 수정
    @Transactional
    public void updatePuppetMode(Long childId, UpdatePuppetModeRequest request) {

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        Puppet puppet = child.getPuppet();
        if (puppet == null) {
            throw new IllegalStateException("해당 아동의 퍼펫을 찾을 수 없습니다.");
        }

        puppet.changeMode(request.getPuppetMode());
    }

    // 퍼펫 모드 조회
    public PuppetMode getPuppetMode(Long childId) {
        return childRepository.findById(childId)
                .map(child -> {
                    Puppet puppet = child.getPuppet();
                    return puppet != null ? puppet.getMode() : PuppetMode.AFFECTIONATE;
                })
                .orElse(PuppetMode.AFFECTIONATE);
    }
}
