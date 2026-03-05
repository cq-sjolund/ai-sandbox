package com.consultant.worklog.repository;

import com.consultant.worklog.model.WorklogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorklogEntryRepository extends JpaRepository<WorklogEntry, Long> {

    List<WorklogEntry> findByEntryDateOrderByCreatedAtDesc(LocalDate entryDate);

    List<WorklogEntry> findByEntryDateBetweenOrderByEntryDateDesc(LocalDate startDate, LocalDate endDate);

    List<WorklogEntry> findByProjectIdOrderByEntryDateDesc(Long projectId);

    // User-based queries (user ownership through project)
    @Query("SELECT e FROM WorklogEntry e WHERE e.project.user.id = :userId ORDER BY e.entryDate DESC")
    List<WorklogEntry> findByUserId(@Param("userId") Long userId);

    @Query("SELECT e FROM WorklogEntry e WHERE e.project.user.id = :userId AND e.entryDate = :date ORDER BY e.createdAt DESC")
    List<WorklogEntry> findByUserIdAndEntryDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT e FROM WorklogEntry e WHERE e.project.user.id = :userId AND e.entryDate BETWEEN :startDate AND :endDate ORDER BY e.entryDate DESC")
    List<WorklogEntry> findByUserIdAndEntryDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    long countByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);

    @Query("SELECT e FROM WorklogEntry e WHERE e.entryDate BETWEEN :startDate AND :endDate " +
           "AND (:projectIds IS NULL OR e.project.id IN :projectIds) " +
           "ORDER BY e.entryDate DESC")
    List<WorklogEntry> findEntriesForSummary(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("projectIds") List<Long> projectIds
    );
}
