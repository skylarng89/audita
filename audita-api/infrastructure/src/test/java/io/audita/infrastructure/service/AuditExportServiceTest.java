package io.audita.infrastructure.service;

import io.audita.application.port.AuditTrailPort;
import io.audita.infrastructure.persistence.entity.AuditExportRequestEntity;
import io.audita.infrastructure.persistence.repository.AuditExportRequestRepository;
import io.audita.infrastructure.persistence.repository.OrgSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditExportServiceTest {

    @Mock
    AuditTrailPort auditTrailPort;
    @Mock
    AuditExportRequestRepository auditExportRequestRepository;
    @Mock
    EmailService emailService;
    @Mock
    OrgSettingRepository orgSettingRepository;

    @InjectMocks
    AuditExportService auditExportService;

    @Test
    void cleanupForCurrentTenant_expires_ready_tokens_and_deletes_stale_files() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();

        AuditExportRequestEntity expiredReady = new AuditExportRequestEntity();
        expiredReady.setStatus(AuditExportRequestEntity.Status.READY);
        expiredReady.setDownloadToken("token-abc");
        expiredReady.setTokenExpiresAt(now.minusMinutes(1));

        Path staleFile = Files.createTempFile("audita-audit-export-", ".csv.gz");
        AuditExportRequestEntity staleExpired = new AuditExportRequestEntity();
        staleExpired.setStatus(AuditExportRequestEntity.Status.EXPIRED);
        staleExpired.setCompletedAt(now.minusDays(8));
        staleExpired.setFileStoragePath(staleFile.toString());

        when(auditExportRequestRepository.findByStatusAndTokenExpiresAtBefore(
                AuditExportRequestEntity.Status.READY,
                now)).thenReturn(List.of(expiredReady));
        when(auditExportRequestRepository.findByStatusInAndCompletedAtBefore(
                List.of(AuditExportRequestEntity.Status.EXPIRED, AuditExportRequestEntity.Status.FAILED),
                now.minusHours(168))).thenReturn(List.of(staleExpired));

        auditExportService.cleanupForCurrentTenant(now, 168);

        assertThat(expiredReady.getStatus()).isEqualTo(AuditExportRequestEntity.Status.EXPIRED);
        assertThat(expiredReady.getDownloadToken()).isNull();
        assertThat(Files.exists(staleFile)).isFalse();

        verify(auditExportRequestRepository).save(expiredReady);
        verify(auditExportRequestRepository).delete(staleExpired);
    }
}
