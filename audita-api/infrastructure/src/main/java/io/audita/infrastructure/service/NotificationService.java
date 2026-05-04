package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.infrastructure.persistence.entity.NotificationEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.NotificationRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class NotificationService {

    private static final long HEARTBEAT_INTERVAL_SECONDS = 25;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private final Map<SseEmitter, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> list(UUID recipientId, int page, int size) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                recipientId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    public void markRead(UUID recipientId, UUID notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Notification not found."));
        if (!notification.getRecipient().getId().equals(recipientId)) {
            throw new DomainNotPermittedException("NOT_FOUND", "Notification not found.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllRead(UUID recipientId) {
        notificationRepository.markAllReadForUser(recipientId);
    }

    public NotificationEntity createAndPush(UUID recipientId,
                                            String type,
                                            String title,
                                            String body,
                                            String link) {
        UserEntity recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Notification recipient not found."));

        NotificationEntity notification = notificationRepository.save(
                new NotificationEntity(recipient, type, title, body, link)
        );
        pushToUser(recipientId, notification);
        return notification;
    }

    public SseEmitter subscribe(UUID recipientId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByUser.computeIfAbsent(recipientId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(recipientId, emitter));
        emitter.onTimeout(() -> removeEmitter(recipientId, emitter));
        emitter.onError(err -> removeEmitter(recipientId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
            scheduleHeartbeat(recipientId, emitter);
        } catch (IOException _) {
            removeEmitter(recipientId, emitter);
        }
        return emitter;
    }

    @PreDestroy
    void shutdownHeartbeatExecutor() {
        heartbeatExecutor.shutdownNow();
    }

    private void pushToUser(UUID recipientId, NotificationEntity notification) {
        List<SseEmitter> emitters = emittersByUser.get(recipientId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(Map.of(
                        "id", notification.getId(),
                        "type", notification.getType(),
                        "title", notification.getTitle(),
                        "body", notification.getBody(),
                        "link", notification.getLink(),
                        "read", notification.isRead(),
                        "isRead", notification.isRead(),
                        "createdAt", notification.getCreatedAt()
                )));
            } catch (IOException _) {
                removeEmitter(recipientId, emitter);
            }
        }
    }

    private void removeEmitter(UUID recipientId, SseEmitter emitter) {
        ScheduledFuture<?> heartbeatTask = heartbeatTasks.remove(emitter);
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
        }

        List<SseEmitter> emitters = emittersByUser.get(recipientId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUser.remove(recipientId);
        }
    }

    private void scheduleHeartbeat(UUID recipientId, SseEmitter emitter) {
        ScheduledFuture<?> task = heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("keepalive").data("ping"));
            } catch (IOException _) {
                removeEmitter(recipientId, emitter);
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        heartbeatTasks.put(emitter, task);
    }
}
