package com.airtribe.SimpleCloudStorage.repository;

// com.airtribe.SimpleCloudStorage.repository.AnalyticsEventRepository
import com.airtribe.SimpleCloudStorage.entity.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {
    List<AnalyticsEvent> findByUserId(Integer userId);

    // Query for total storage used by a user
    @Query("SELECT SUM(ae.fileSize) FROM AnalyticsEvent ae WHERE ae.userId = :userId AND ae.eventType = 'UPLOAD'")
    Long getTotalStorageUsedByUserId(@Param("userId") Integer userId);

    // Query for file type distribution
    @Query("SELECT ae.fileType, COUNT(ae) FROM AnalyticsEvent ae WHERE ae.userId = :userId GROUP BY ae.fileType")
    List<Object[]> countFileTypesByUserId(@Param("userId") Integer userId);

    // Query for frequency of file access (e.g., downloads over time)
    @Query("SELECT FUNCTION('DATE_TRUNC', 'day', ae.timestamp), COUNT(ae) FROM AnalyticsEvent ae WHERE ae.userId = :userId AND ae.eventType = :eventType GROUP BY FUNCTION('DATE_TRUNC', 'day', ae.timestamp) ORDER BY FUNCTION('DATE_TRUNC', 'day', ae.timestamp)")
    List<Object[]> getEventFrequencyByUserIdAndEventType(@Param("userId") Integer userId, @Param("eventType") String eventType);
}