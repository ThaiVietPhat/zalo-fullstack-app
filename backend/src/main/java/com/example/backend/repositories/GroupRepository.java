package com.example.backend.repositories;

import com.example.backend.Entities.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    // Lấy tất cả nhóm mà user là thành viên
    @Query("""
        SELECT DISTINCT g FROM Group g
        JOIN g.members m
        WHERE m.user.id = :userId
        ORDER BY g.createdDate DESC
    """)
    List<Group> findAllGroupsByUserId(@Param("userId") UUID userId);
}