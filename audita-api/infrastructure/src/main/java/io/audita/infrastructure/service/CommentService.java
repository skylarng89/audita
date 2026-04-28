package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.CommentEntity;
import io.audita.infrastructure.persistence.entity.CommentMentionEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.CommentMentionRepository;
import io.audita.infrastructure.persistence.repository.CommentRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class CommentService {

    private static final Pattern EMAIL_MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    private final ChangeRequestRepository changeRequestRepository;
    private final CommentRepository commentRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final UserRepository userRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final PolicyFactory htmlPolicy = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS).and(Sanitizers.LINKS);

    public CommentService(ChangeRequestRepository changeRequestRepository,
                          CommentRepository commentRepository,
                          CommentMentionRepository commentMentionRepository,
                          UserRepository userRepository,
                          ActivityStreamRepository activityStreamRepository,
                          NotificationService notificationService,
                          EmailService emailService) {
        this.changeRequestRepository = changeRequestRepository;
        this.commentRepository = commentRepository;
        this.commentMentionRepository = commentMentionRepository;
        this.userRepository = userRepository;
        this.activityStreamRepository = activityStreamRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<CommentEntity> list(UUID changeRequestId) {
        ensureChangeRequestExists(changeRequestId);
        return commentRepository.findByChangeRequestIdOrderByCreatedAtDesc(changeRequestId);
    }

    public CommentEntity create(UUID changeRequestId, UUID authorId, String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new DomainNotPermittedException("INVALID_INPUT", "Comment body is required.");
        }

        ChangeRequestEntity changeRequest = ensureChangeRequestExists(changeRequestId);
        UserEntity author = userRepository.findById(authorId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Comment author not found."));

        Set<UserEntity> mentionedUsers = resolveMentionedUsers(rawBody.trim(), authorId);
        String sanitisedBody = htmlPolicy.sanitize(rawBody.trim());
        CommentEntity comment = commentRepository.save(new CommentEntity(changeRequest, author, sanitisedBody));
        for (UserEntity user : mentionedUsers) {
            commentMentionRepository.save(new CommentMentionEntity(comment, user));
            notificationService.createAndPush(
                    user.getId(),
                    "MENTION",
                    author.getFullName() + " mentioned you",
                    "In change request: " + changeRequest.getTitle(),
                    "/change-requests/" + changeRequest.getId()
            );
            emailService.sendMentionEmail(
                    user.getEmail(),
                    user.getFullName(),
                    changeRequest.getTitle(),
                    changeRequest.getId().toString(),
                    author.getFullName()
            );
        }

        activityStreamRepository.save(new ActivityStreamEntity(
                changeRequest,
                author,
                "CR_COMMENT_ADDED",
                Map.of("commentId", comment.getId().toString(), "mentions", mentionedUsers.size())
        ));

        return comment;
    }

    private ChangeRequestEntity ensureChangeRequestExists(UUID changeRequestId) {
        return changeRequestRepository.findById(changeRequestId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Change request not found."));
    }

    private Set<UserEntity> resolveMentionedUsers(String body, UUID authorId) {
        String plainText = body.replaceAll("<[^>]+>", " ");
        Matcher matcher = EMAIL_MENTION_PATTERN.matcher(plainText);
        Set<UserEntity> users = new HashSet<>();

        while (matcher.find()) {
            String email = matcher.group(1).toLowerCase();
            userRepository.findByEmailIgnoreCase(email)
                    .filter(user -> !user.getId().equals(authorId))
                    .ifPresent(users::add);
        }
        return users;
    }
}
