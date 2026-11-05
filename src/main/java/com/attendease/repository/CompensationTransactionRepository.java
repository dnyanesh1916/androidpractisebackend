package com.attendease.repository;

import com.attendease.entity.CompensationTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CompensationTransactionRepository extends JpaRepository<CompensationTransaction, Long> {
    List<CompensationTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CompensationTransaction> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}
