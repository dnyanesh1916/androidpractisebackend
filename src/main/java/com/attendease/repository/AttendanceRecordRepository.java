package com.attendease.repository;

import com.attendease.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByUserIdOrderByCheckInTimeDesc(Long userId);
    List<AttendanceRecord> findByCheckInTimeBetweenOrderByCheckInTimeDesc(LocalDateTime start, LocalDateTime end);
    List<AttendanceRecord> findAllByOrderByCheckInTimeDesc();
    boolean existsByUserIdAndCheckInTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
