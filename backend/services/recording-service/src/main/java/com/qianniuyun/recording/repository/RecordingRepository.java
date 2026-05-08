package com.qianniuyun.recording.repository;

import com.qianniuyun.recording.entity.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RecordingRepository extends JpaRepository<Recording, Long> {
    Optional<Recording> findByCallId(String callId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Recording r WHERE r.expiresAt < :now")
    int deleteExpiredRecordings(LocalDateTime now);
}
