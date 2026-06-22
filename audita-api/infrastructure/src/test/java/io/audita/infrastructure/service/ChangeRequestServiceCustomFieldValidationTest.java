package io.audita.infrastructure.service;

import io.audita.domain.exception.InvalidRequestException;
import io.audita.domain.exception.NotFoundException;
import io.audita.infrastructure.persistence.entity.ChangeRequestCustomFieldEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.CustomFieldDefinitionEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.*;
import io.audita.infrastructure.security.HtmlSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeRequestServiceCustomFieldValidationTest {

    @Mock
    ChangeRequestRepository changeRequestRepository;
    @Mock
    CrApproverRepository crApproverRepository;
    @Mock
    CrWatcherRepository crWatcherRepository;
    @Mock
    GroupRepository groupRepository;
    @Mock
    GroupMemberRepository groupMemberRepository;
    @Mock
    ChangeRequestCustomFieldRepository customFieldRepository;
    @Mock
    CustomFieldDefinitionRepository customFieldDefinitionRepository;
    @Mock
    ActivityStreamRepository activityStreamRepository;
    @Mock
    AttachmentRepository attachmentRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    OrgSettingRepository orgSettingRepository;
    @Mock
    RequestDeploymentService deploymentService;
    @Mock
    AuditLogService auditLogService;
    @Mock
    HtmlSanitizer htmlSanitizer;
    @Mock
    NotificationService notificationService;
    @Mock
    EmailService emailService;

    @InjectMocks
    ChangeRequestService service;

    private UUID requestId;
    private UUID userId;
    private UserEntity user;
    private ChangeRequestEntity cr;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        userId = UUID.randomUUID();
        user = mock(UserEntity.class);
        lenient().when(user.getId()).thenReturn(userId);
        cr = new ChangeRequestEntity();
        ReflectionTestUtils.setField(cr, "id", requestId);
        cr.setCreatedBy(user);
        when(changeRequestRepository.findById(requestId)).thenReturn(Optional.of(cr));
    }

    private CustomFieldDefinitionEntity numberDef(boolean required, BigDecimal min, BigDecimal max) {
        CustomFieldDefinitionEntity def = new CustomFieldDefinitionEntity(
                "Score", "NUMBER", List.of(), required, 1, min, max);
        ReflectionTestUtils.setField(def, "id", UUID.randomUUID());
        return def;
    }

    @Test
    void should_accept_valid_number_in_range() {
        CustomFieldDefinitionEntity def = numberDef(false, BigDecimal.ZERO, new BigDecimal("100"));
        UUID fieldId = def.getId();
        when(customFieldDefinitionRepository.findById(fieldId)).thenReturn(Optional.of(def));
        doNothing().when(customFieldRepository).deleteByIdChangeRequestId(requestId);
        when(customFieldRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ChangeRequestCustomFieldEntity> result = service.upsertCustomFields(
                requestId, List.of(new ChangeRequestService.FieldValue(fieldId, "42.5")), userId, "ADMIN");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getValue()).isEqualTo("42.5");
    }

    @Test
    void should_reject_non_numeric_value() {
        CustomFieldDefinitionEntity def = numberDef(false, null, null);
        UUID fieldId = def.getId();
        when(customFieldDefinitionRepository.findById(fieldId)).thenReturn(Optional.of(def));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                service.upsertCustomFields(requestId,
                        List.of(new ChangeRequestService.FieldValue(fieldId, "abc")), userId, "ADMIN"));

        assertThat(ex.getMessage()).contains("valid number");
    }

    @Test
    void should_reject_value_below_min() {
        CustomFieldDefinitionEntity def = numberDef(false, new BigDecimal("5"), null);
        UUID fieldId = def.getId();
        when(customFieldDefinitionRepository.findById(fieldId)).thenReturn(Optional.of(def));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                service.upsertCustomFields(requestId,
                        List.of(new ChangeRequestService.FieldValue(fieldId, "3")), userId, "ADMIN"));

        assertThat(ex.getMessage()).contains("at least 5");
    }

    @Test
    void should_reject_value_above_max() {
        CustomFieldDefinitionEntity def = numberDef(false, null, new BigDecimal("100"));
        UUID fieldId = def.getId();
        when(customFieldDefinitionRepository.findById(fieldId)).thenReturn(Optional.of(def));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                service.upsertCustomFields(requestId,
                        List.of(new ChangeRequestService.FieldValue(fieldId, "150")), userId, "ADMIN"));

        assertThat(ex.getMessage()).contains("at most 100");
    }

    @Test
    void should_reject_more_than_2_decimal_places() {
        CustomFieldDefinitionEntity def = numberDef(false, null, null);
        UUID fieldId = def.getId();
        when(customFieldDefinitionRepository.findById(fieldId)).thenReturn(Optional.of(def));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                service.upsertCustomFields(requestId,
                        List.of(new ChangeRequestService.FieldValue(fieldId, "1.234")), userId, "ADMIN"));

        assertThat(ex.getMessage()).contains("2 decimal places");
    }

    @Test
    void should_accept_2_decimal_places() {
        CustomFieldDefinitionEntity def = numberDef(false, null, null);
        UUID fieldId = def.getId();
        when(customFieldDefinitionRepository.findById(fieldId)).thenReturn(Optional.of(def));
        doNothing().when(customFieldRepository).deleteByIdChangeRequestId(requestId);
        when(customFieldRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ChangeRequestCustomFieldEntity> result = service.upsertCustomFields(
                requestId, List.of(new ChangeRequestService.FieldValue(fieldId, "12.99")), userId, "ADMIN");

        assertThat(result).hasSize(1);
    }

    @Test
    void should_reject_null_value_when_required() {
        CustomFieldDefinitionEntity def = numberDef(true, null, null);
        UUID fieldId = def.getId();
        when(customFieldDefinitionRepository.findById(fieldId)).thenReturn(Optional.of(def));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                service.upsertCustomFields(requestId,
                        List.of(new ChangeRequestService.FieldValue(fieldId, null)), userId, "ADMIN"));

        assertThat(ex.getMessage()).contains("is required");
    }

    @Test
    void should_reject_blank_value_when_required() {
        CustomFieldDefinitionEntity def = numberDef(true, null, null);
        UUID fieldId = def.getId();
        when(customFieldDefinitionRepository.findById(fieldId)).thenReturn(Optional.of(def));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                service.upsertCustomFields(requestId,
                        List.of(new ChangeRequestService.FieldValue(fieldId, "   ")), userId, "ADMIN"));

        assertThat(ex.getMessage()).contains("is required");
    }

    @Test
    void should_skip_validation_for_blank_non_required() {
        CustomFieldDefinitionEntity def = numberDef(false, null, null);
        UUID fieldId = def.getId();
        when(customFieldDefinitionRepository.findById(fieldId)).thenReturn(Optional.of(def));
        doNothing().when(customFieldRepository).deleteByIdChangeRequestId(requestId);
        when(customFieldRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<ChangeRequestCustomFieldEntity> result = service.upsertCustomFields(
                requestId, List.of(new ChangeRequestService.FieldValue(fieldId, null)), userId, "ADMIN");

        assertThat(result).hasSize(1);
    }

    @Test
    void should_throw_not_found_for_missing_definition() {
        UUID badFieldId = UUID.randomUUID();
        when(customFieldDefinitionRepository.findById(badFieldId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.upsertCustomFields(requestId,
                        List.of(new ChangeRequestService.FieldValue(badFieldId, "1")), userId, "ADMIN"));
    }
}
