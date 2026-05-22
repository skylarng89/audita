package io.audita.infrastructure.service;

import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.CommentEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.CommentMentionRepository;
import io.audita.infrastructure.persistence.repository.CommentRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock ChangeRequestRepository changeRequestRepository;
    @Mock CommentRepository commentRepository;
    @Mock CommentMentionRepository commentMentionRepository;
    @Mock UserRepository userRepository;
    @Mock ActivityStreamRepository activityStreamRepository;
    @Mock NotificationService notificationService;
    @Mock EmailService emailService;

    @InjectMocks
    CommentService commentService;

    @Test
    void create_sanitises_body_and_dispatches_mention_notifications() {
        UUID crId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID mentionedId = UUID.randomUUID();

        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", crId);
        cr.setTitle("DB migration");

        UserEntity author = new UserEntity("author@example.com", "Author One");
        ReflectionTestUtils.setField(author, "id", authorId);

        UserEntity mentioned = new UserEntity("bob@example.com", "Bob Two");
        ReflectionTestUtils.setField(mentioned, "id", mentionedId);

        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(mentioned));
        when(commentRepository.save(any(CommentEntity.class))).thenAnswer(inv -> {
            CommentEntity c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });

        CommentEntity created = commentService.create(
                crId,
                authorId,
                "Hello team @bob@example.com"
        );

        assertThat(created.getBody()).contains("Hello");
        assertThat(created.getBody()).doesNotContain("<script>");
        verify(commentMentionRepository).save(any());
        verify(notificationService).createAndPush(
            mentionedId,
            "MENTION",
            "Author One mentioned you",
            "In change request: DB migration",
            "/change-requests/" + crId
        );
        verify(emailService).sendMentionEmail(
            eq("bob@example.com"),
            eq("Bob Two"),
            eq("DB migration"),
            eq(crId.toString()),
            anyString(),
            eq("Author One")
        );
        verify(activityStreamRepository).save(any());
    }

    @Test
    void list_returns_change_request_comments_ordered() {
        UUID crId = UUID.randomUUID();
        ChangeRequestEntity cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", crId);

        CommentEntity c1 = new CommentEntity(cr, null, "one");
        ReflectionTestUtils.setField(c1, "createdAt", OffsetDateTime.now());
        CommentEntity c2 = new CommentEntity(cr, null, "two");
        ReflectionTestUtils.setField(c2, "createdAt", OffsetDateTime.now().minusMinutes(1));

        when(changeRequestRepository.findById(crId)).thenReturn(Optional.of(cr));
        when(commentRepository.findByChangeRequestIdOrderByCreatedAtDesc(crId)).thenReturn(List.of(c1, c2));

        List<CommentEntity> results = commentService.list(crId);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getBody()).isEqualTo("one");
        assertThat(results.get(1).getBody()).isEqualTo("two");
    }
}
