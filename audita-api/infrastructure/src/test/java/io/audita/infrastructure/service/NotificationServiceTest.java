package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.infrastructure.persistence.entity.NotificationEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.NotificationRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    NotificationService notificationService;

    @Test
    void list_and_unread_count_return_repository_data() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("user@example.com", "User One");
        ReflectionTestUtils.setField(user, "id", userId);
        NotificationEntity notification = new NotificationEntity(user, "MENTION", "t", "b", "/x");

        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationRepository.countByRecipientIdAndIsReadFalse(userId)).thenReturn(3L);

        assertThat(notificationService.list(userId, 0, 20)).hasSize(1);
        assertThat(notificationService.unreadCount(userId)).isEqualTo(3L);
    }

    @Test
    void mark_read_rejects_notification_for_other_user() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();

        UserEntity owner = new UserEntity("owner@example.com", "Owner");
        ReflectionTestUtils.setField(owner, "id", ownerId);
        NotificationEntity notification = new NotificationEntity(owner, "MENTION", "title", "body", "/a");
        ReflectionTestUtils.setField(notification, "id", notifId);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markRead(otherId, notifId))
                .isInstanceOf(DomainNotPermittedException.class);
    }

    @Test
    void create_and_push_saves_notification_and_supports_subscription() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity("user@example.com", "User One");
        ReflectionTestUtils.setField(user, "id", userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(inv -> {
            NotificationEntity n = inv.getArgument(0);
            ReflectionTestUtils.setField(n, "id", UUID.randomUUID());
            return n;
        });

        SseEmitter emitter = notificationService.subscribe(userId);
        assertThat(emitter).isNotNull();

        NotificationEntity created = notificationService.createAndPush(
                userId,
                "SLA_WARNING",
                "SLA warning",
                "A CR is close to breach",
                "/change-requests/123"
        );

        assertThat(created.getType()).isEqualTo("SLA_WARNING");
        verify(notificationRepository).save(any(NotificationEntity.class));
    }
}
