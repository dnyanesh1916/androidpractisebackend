package com.attendease.repository;

import com.attendease.entity.AttendanceLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceLocationRepository extends JpaRepository<AttendanceLocation, Long> {
    List<AttendanceLocation> findAllByOrderByNameAsc();
    List<AttendanceLocation> findAllByActiveTrue();
}
