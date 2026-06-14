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
import org.owasp.html.HtmlPolicyBuilder;
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

    private static final Pattern EMAIL_MENTION_PATTERN = Pattern
            .compile("@([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    private final ChangeRequestRepository changeRequestRepository;
    private final CommentRepository commentRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final UserRepository userRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final MentionNotifier mentionNotifier;
    private final PolicyFactory htmlPolicy = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS)
            .and(new HtmlPolicyBuilder()
                    .allowElements("span")
                    .allowAttributes("class", "data-type", "data-id", "data-label", "data-mention-suggestion-char")
                    .onElements("span")
                    .toFactory());

    public CommentService(ChangeRequestRepository changeRequestRepository,
            CommentRepository commentRepository,
            CommentMentionRepository commentMentionRepository,
            UserRepository userRepository,
            ActivityStreamRepository activityStreamRepository,
            NotificationService notificationService,
            EmailService emailService,
            MentionNotifier mentionNotifier) {
        this.changeRequestRepository = changeRequestRepository;
        this.commentRepository = commentRepository;
        this.commentMentionRepository = commentMentionRepository;
        this.userRepository = userRepository;
        this.activityStreamRepository = activityStreamRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.mentionNotifier = mentionNotifier;
    }

    @Transactional(readOnly = true)
    public List<CommentEntity> list(UUID changeRequestId) {
        ensureChangeRequestExists(changeRequestId);
        List<CommentEntity> comments = commentRepository.findByChangeRequestIdOrderByCreatedAtDesc(changeRequestId);
        comments.forEach(this::initializeAuthor);
        return comments;
    }

    public CommentEntity create(UUID changeRequestId, UUID authorId, String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new DomainNotPermittedException("INVALID_INPUT", "Comment body is required.");
        }

        ChangeRequestEntity changeRequest = ensureChangeRequestExists(changeRequestId);
        UserEntity author = userRepository.findById(authorId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Comment author not found."));

        Set<UserEntity> mentionedUsers = mentionNotifier.processMentions(rawBody.trim(), authorId,
                changeRequest.getTitle(), "/change-requests/" + changeRequest.getId());
        String sanitisedBody = htmlPolicy.sanitize(rawBody.trim());
        CommentEntity comment = commentRepository.save(new CommentEntity(changeRequest, author, sanitisedBody));
        for (UserEntity user : mentionedUsers) {
            commentMentionRepository.save(new CommentMentionEntity(comment, user));
        }

        activityStreamRepository.save(new ActivityStreamEntity(
                changeRequest,
                author,
                "CR_COMMENT_ADDED",
                Map.of("commentId", comment.getId().toString(), "mentions", mentionedUsers.size())));

        initializeAuthor(comment);
        return comment;
    }

    private void initializeAuthor(CommentEntity comment) {
        UserEntity author = comment.getAuthor();
        if (author == null) {
            return;
        }

        author.getEmail();
        author.getFullName();
        author.getRoles().size();
        if (author.getRole() != null) {
            author.getRole().getId();
            author.getRole().getName();
        }
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
