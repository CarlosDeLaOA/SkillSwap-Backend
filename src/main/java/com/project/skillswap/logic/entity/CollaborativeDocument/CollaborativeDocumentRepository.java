package com.project.skillswap.logic.entity.CollaborativeDocument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollaborativeDocumentRepository extends JpaRepository<CollaborativeDocument, Long> {

    Optional<CollaborativeDocument> findBySessionId(Long sessionId);

    Optional<CollaborativeDocument> findByDocumentId(String documentId);

    @Query("SELECT cd FROM CollaborativeDocument cd WHERE cd.expiresAt < :now AND cd.isActive = true")
    List<CollaborativeDocument> findExpiredDocuments(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM CollaborativeDocument cd WHERE cd.expiresAt < :now")
    int deleteExpiredDocuments(LocalDateTime now);

    List<CollaborativeDocument> findByIsActiveTrue();
}