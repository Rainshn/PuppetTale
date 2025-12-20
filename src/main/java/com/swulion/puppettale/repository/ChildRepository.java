package com.swulion.puppettale.repository;

import com.swulion.puppettale.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ChildRepository extends JpaRepository<Child, Long> {

    @Modifying
    @Query("UPDATE Child c SET c.isWarningState = false")
    void resetAllWarningStates();
}
