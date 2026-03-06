package com.consultant.worklog.repository;

import com.consultant.worklog.model.DynamicsConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DynamicsConfigRepository extends JpaRepository<DynamicsConfig, Long> {
    Optional<DynamicsConfig> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
