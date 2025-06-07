package com.airtribe.SimpleCloudStorage.service;

import com.airtribe.SimpleCloudStorage.repository.AnalyticsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsEventRepository analyticsEventRepository;

    public Long getStorageUsage(Integer userId) {
        return analyticsEventRepository.getTotalStorageUsedByUserId(userId);
    }

    public Map<String, Long> getFileTypeDistribution(Integer userId) {
        List<Object[]> results = analyticsEventRepository.countFileTypesByUserId(userId);
        log.error("countFileTypesByUserId repo:{}",results);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // fileType
                        row -> (Long) row[1]    // count
                ));
    }

    public Map<String, Long> getFileAccessFrequency(Integer userId, String eventType) {
        List<Object[]> results = analyticsEventRepository.getEventFrequencyByUserIdAndEventType(userId, eventType);
        log.error("getEventFrequencyByUserIdAndEventType repo:{}",results);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(), // timestamp (truncated to day)
                        row -> (Long) row[1]     // count
                ));
    }

}