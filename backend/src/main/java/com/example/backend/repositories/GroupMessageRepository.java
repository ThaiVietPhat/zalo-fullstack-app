package com.example.backend.repositories;

import com.example.backend.Entities.GroupMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, UUID> {

    Page<GroupMessage> findByGroupIdAndDeletedFalseOrderByCreatedDateAsc(UUID groupId, Pageable pageable);

    Optional<GroupMessage> findTop1ByGroupIdAndDeletedFalseOrderByCreatedDateDesc(UUID groupId);

    // Giữ lại để tương thích với code cũ gọi không có deleted filter
    Optional<GroupMessage> findTop1ByGroupIdOrderByCreatedDateDesc(UUID groupId);

    long countByDeletedFalse();

    @Query("SELECT CAST(m.createdDate AS date), COUNT(m) FROM GroupMessage m " +
            "WHERE m.createdDate >= :since AND m.deleted = false " +
            "GROUP BY CAST(m.createdDate AS date) ORDER BY CAST(m.createdDate AS date) ASC")
    List<Object[]> countDailyGroupMessages(@Param("since") LocalDateTime since);

    @Query("SELECT m.sender.id, COUNT(m) FROM GroupMessage m WHERE m.deleted = false " +
            "GROUP BY m.sender.id ORDER BY COUNT(m) DESC")
    List<Object[]> findTopGroupSenderIds(Pageable pageable);
}
