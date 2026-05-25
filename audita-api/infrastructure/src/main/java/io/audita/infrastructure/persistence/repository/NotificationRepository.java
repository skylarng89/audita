package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    long deleteByIsSampleTrue();
    long countByIsSampleTrue();

    Page<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    List<NotificationEntity> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(UUID recipientId);

    long countByRecipientIdAndIsReadFalse(UUID recipientId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = TRUE WHERE n.recipient.id = :recipientId")
    void markAllReadForUser(UUID recipientId);
}
