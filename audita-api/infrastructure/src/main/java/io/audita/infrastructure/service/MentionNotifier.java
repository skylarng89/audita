package io.audita.infrastructure.service;

import io.audita.infrastructure.persistence.entity.NotificationEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MentionNotifier {

    private static final Pattern EMAIL_MENTION_PATTERN = Pattern.compile(
            "@([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public MentionNotifier(UserRepository userRepository,
                           NotificationService notificationService,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    /**
     * Extracts @email mentions from comment HTML, resolves users, and sends
     * in-app notifications + emails to each mentioned user.
     *
     * @param body        the comment HTML body
     * @param commenterId the UUID of the user who wrote the comment
     * @param resourceTitle the title of the resource (CR/UAT/Deployment)
     * @param resourceLink the frontend link to the resource with comment anchor
     * @return set of resolved UserEntity objects (for mention persistence)
     */
    public Set<UserEntity> processMentions(String body, UUID commenterId,
                                            String resourceTitle, String resourceLink) {
        if (body == null || body.isBlank()) {
            return Collections.emptySet();
        }

        UserEntity commenter = userRepository.findById(commenterId).orElse(null);
        String commenterName = commenter != null ? commenter.getFullName() : "Someone";

        Set<UserEntity> mentioned = new HashSet<>();
        Matcher matcher = EMAIL_MENTION_PATTERN.matcher(body);
        while (matcher.find()) {
            String email = matcher.group(1).toLowerCase();
            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                if (!mentioned.contains(user)) {
                    mentioned.add(user);

                    notificationService.createAndPush(
                            user.getId(),
                            "MENTION",
                            commenterName + " mentioned you",
                            "In: " + resourceTitle,
                            resourceLink);

                    emailService.sendMentionEmail(
                            user.getEmail(),
                            user.getFullName(),
                            resourceTitle,
                            resourceLink);
                }
            });
        }
        return mentioned;
    }
}
