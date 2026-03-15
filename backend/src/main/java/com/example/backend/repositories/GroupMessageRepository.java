package com.example.backend.repositories;

import com.example.backend.Entities.GroupMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, UUID> {

    Page<GroupMessage> findByGroupIdOrderByCreatedDateAsc(UUID groupId, Pageable pageable);

    Optional<GroupMessage> findTop1ByGroupIdOrderByCreatedDateDesc(UUID groupId);
}