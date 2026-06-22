package io.audita.infrastructure.service;

import io.audita.application.port.SampleDataPort;
import io.audita.domain.model.*;
import io.audita.infrastructure.persistence.entity.*;
import io.audita.infrastructure.persistence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@Transactional
public class SampleDataService implements SampleDataPort {

    private static final Logger log = LoggerFactory.getLogger(SampleDataService.class);
    private static final String SAMPLE_DATA_IMPORTED_KEY = "sample_data.imported";
    private static final String SAMPLE_DATA_DEFAULT_PASSWORD = "Password@2026";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final CrApproverRepository crApproverRepository;
    private final CrWatcherRepository crWatcherRepository;
    private final CommentRepository commentRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final NotificationRepository notificationRepository;
    private final AttachmentRepository attachmentRepository;
    private final ChangeRequestCustomFieldRepository changeRequestCustomFieldRepository;
    private final OrgSettingRepository orgSettingRepository;
    private final RequestUatRepository requestUatRepository;
    private final RequestUatWatcherRepository requestUatWatcherRepository;
    private final RequestDeploymentRepository requestDeploymentRepository;
    private final PasswordEncoder passwordEncoder;

    public SampleDataService(UserRepository userRepository,
            RoleRepository roleRepository,
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            CustomFieldDefinitionRepository customFieldDefinitionRepository,
            ChangeRequestRepository changeRequestRepository,
            CrApproverRepository crApproverRepository,
            CrWatcherRepository crWatcherRepository,
            CommentRepository commentRepository,
            CommentMentionRepository commentMentionRepository,
            ActivityStreamRepository activityStreamRepository,
            NotificationRepository notificationRepository,
            AttachmentRepository attachmentRepository,
            ChangeRequestCustomFieldRepository changeRequestCustomFieldRepository,
            OrgSettingRepository orgSettingRepository,
            RequestUatRepository requestUatRepository,
            RequestUatWatcherRepository requestUatWatcherRepository,
            RequestDeploymentRepository requestDeploymentRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.customFieldDefinitionRepository = customFieldDefinitionRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.crApproverRepository = crApproverRepository;
        this.crWatcherRepository = crWatcherRepository;
        this.commentRepository = commentRepository;
        this.commentMentionRepository = commentMentionRepository;
        this.activityStreamRepository = activityStreamRepository;
        this.notificationRepository = notificationRepository;
        this.attachmentRepository = attachmentRepository;
        this.changeRequestCustomFieldRepository = changeRequestCustomFieldRepository;
        this.orgSettingRepository = orgSettingRepository;
        this.requestUatRepository = requestUatRepository;
        this.requestUatWatcherRepository = requestUatWatcherRepository;
        this.requestDeploymentRepository = requestDeploymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSampleDataImported(String tenantSlug) {
        return orgSettingRepository.findById(SAMPLE_DATA_IMPORTED_KEY)
                .map(e -> "true".equalsIgnoreCase(e.getValue()))
                .orElse(false);
    }

    @Override
    public SampleDataSummary importSampleData(String tenantSlug) {
        if (isSampleDataImported(tenantSlug)) {
            return currentSampleDataCounts("Sample data is already imported.");
        }

        String passwordHash = passwordEncoder.encode(SAMPLE_DATA_DEFAULT_PASSWORD);
        OffsetDateTime now = OffsetDateTime.now();

        Map<String, UserEntity> sampleUsers = createSampleUsers(passwordHash, now);
        Map<String, GroupEntity> sampleGroups = createSampleGroups(sampleUsers, now);
        createSampleGroupMembers(sampleUsers, sampleGroups);
        Map<String, CustomFieldDefinitionEntity> sampleFields = createSampleCustomFields(now);
        Map<String, ChangeRequestEntity> sampleCRs = createSampleChangeRequests(sampleUsers, now);
        createSampleApprovers(sampleCRs, sampleUsers, now);
        createSampleWatchers(sampleCRs, sampleUsers, now);
        createSampleDeployments(sampleCRs, sampleUsers, now);
        createSampleCustomFieldValues(sampleCRs, sampleFields);
        createSampleComments(sampleCRs, sampleUsers, now);
        createSampleActivityStream(sampleCRs, sampleUsers, now);
        createSampleNotifications(sampleUsers, sampleCRs, now);
        createSampleAttachments(sampleCRs, sampleUsers, now);

        orgSettingRepository.save(new OrgSettingEntity(SAMPLE_DATA_IMPORTED_KEY, "true"));

        log.info("Sample data imported for tenant: {}", tenantSlug);
        return new SampleDataSummary(
                sampleUsers.size(),
                sampleGroups.size(),
                sampleCRs.size(),
                (int) commentRepository.countByIsSampleTrue(),
                sampleFields.size(),
                "Sample data imported successfully. Default password for all users: Password@2026");
    }

    @Override
    public SampleDataSummary removeSampleData(String tenantSlug) {
        if (!isSampleDataImported(tenantSlug)) {
            return new SampleDataSummary(0, 0, 0, 0, 0, "No sample data to remove.");
        }

        long usersCount = userRepository.countByIsSampleTrue();
        long groupsCount = groupRepository.countByIsSampleTrue();
        long crsCount = changeRequestRepository.countByIsSampleTrue();
        long commentsCount = commentRepository.countByIsSampleTrue();
        long cfCount = customFieldDefinitionRepository.countByIsSampleTrue();

        commentMentionRepository.deleteByIsSampleTrue();
        notificationRepository.deleteByIsSampleTrue();
        activityStreamRepository.deleteByIsSampleTrue();
        crApproverRepository.deleteByIsSampleTrue();
        crWatcherRepository.deleteByIsSampleTrue();
        requestUatWatcherRepository.deleteByIsSampleTrue();
        changeRequestCustomFieldRepository.deleteByIsSampleTrue();
        commentRepository.deleteByIsSampleTrue();
        attachmentRepository.deleteByIsSampleTrue();
        changeRequestRepository.deleteByIsSampleTrue();
        customFieldDefinitionRepository.deleteByIsSampleTrue();
        groupMemberRepository.deleteByIsSampleTrue();
        groupRepository.deleteByIsSampleTrue();
        userRepository.deleteByIsSampleTrue();

        orgSettingRepository.deleteById(SAMPLE_DATA_IMPORTED_KEY);

        log.info("Sample data removed for tenant: {}", tenantSlug);
        return new SampleDataSummary(
                (int) usersCount, (int) groupsCount, (int) crsCount,
                (int) commentsCount, (int) cfCount,
                "Sample data removed successfully.");
    }

    private SampleDataSummary currentSampleDataCounts(String message) {
        return new SampleDataSummary(
                (int) userRepository.countByIsSampleTrue(),
                (int) groupRepository.countByIsSampleTrue(),
                (int) changeRequestRepository.countByIsSampleTrue(),
                (int) commentRepository.countByIsSampleTrue(),
                (int) customFieldDefinitionRepository.countByIsSampleTrue(),
                message);
    }

    private Map<String, UserEntity> createSampleUsers(String passwordHash, OffsetDateTime now) {
        RoleEntity adminRole = roleRepository.findByName("Admin")
                .orElseThrow(() -> new IllegalStateException("Admin role not found"));
        RoleEntity requesterRole = roleRepository.findByName("Requester")
                .orElseThrow(() -> new IllegalStateException("Requester role not found"));
        RoleEntity auditorRole = roleRepository.findByName("Auditor")
                .orElseThrow(() -> new IllegalStateException("Auditor role not found"));

        Map<String, UserEntity> users = new LinkedHashMap<>();
        users.put("sarah_chen", persistUser("sarah.chen@acme-demo.io", "Sarah Chen", adminRole, passwordHash));
        users.put("james_wilson", persistUser("james.wilson@acme-demo.io", "James Wilson", requesterRole, passwordHash));
        users.put("maria_garcia", persistUser("maria.garcia@acme-demo.io", "Maria Garcia", requesterRole, passwordHash));
        users.put("david_kim", persistUser("david.kim@acme-demo.io", "David Kim", requesterRole, passwordHash));
        users.put("robert_johnson", persistUser("robert.johnson@acme-demo.io", "Robert Johnson", requesterRole, passwordHash));
        users.put("lisa_patel", persistUser("lisa.patel@acme-demo.io", "Lisa Patel", auditorRole, passwordHash));
        users.put("alex_thompson", persistUser("alex.thompson@acme-demo.io", "Alex Thompson", requesterRole, passwordHash));
        users.put("priya_sharma", persistUser("priya.sharma@acme-demo.io", "Priya Sharma", requesterRole, passwordHash));
        return users;
    }

    private UserEntity persistUser(String email, String fullName, RoleEntity role, String passwordHash) {
        UserEntity user = new UserEntity(email, fullName);
        user.setRole(role);
        user.setRoles(new LinkedHashSet<>(List.of(role)));
        user.setPasswordHash(passwordHash);
        user.setStatus(UserStatus.ACTIVE);
        user.setSample(true);
        return userRepository.save(user);
    }

    private Map<String, GroupEntity> createSampleGroups(Map<String, UserEntity> users, OffsetDateTime now) {
        UserEntity sarah = users.get("sarah_chen");
        Map<String, GroupEntity> groups = new LinkedHashMap<>();

        GroupEntity infraTeam = new GroupEntity("Infrastructure Team",
                "Manages production infrastructure, deployments, and incident response", sarah);
        infraTeam.setSample(true);
        groups.put("infrastructure", groupRepository.save(infraTeam));

        GroupEntity securityBoard = new GroupEntity("Security Review Board",
                "Reviews and approves security-related changes and policy updates", sarah);
        securityBoard.setSample(true);
        groups.put("security", groupRepository.save(securityBoard));

        GroupEntity devTeam = new GroupEntity("Development Team",
                "Full-stack development team working on product features and bug fixes", sarah);
        devTeam.setSample(true);
        groups.put("development", groupRepository.save(devTeam));

        GroupEntity cab = new GroupEntity("Change Advisory Board",
                "Cross-functional board overseeing all significant changes to production systems", sarah);
        cab.setSample(true);
        groups.put("cab", groupRepository.save(cab));

        return groups;
    }

    private void createSampleGroupMembers(Map<String, UserEntity> users, Map<String, GroupEntity> groups) {
        addGroupMember(groups.get("infrastructure"), users.get("david_kim"));
        addGroupMember(groups.get("infrastructure"), users.get("james_wilson"));
        addGroupMember(groups.get("infrastructure"), users.get("alex_thompson"));
        addGroupMember(groups.get("security"), users.get("robert_johnson"));
        addGroupMember(groups.get("security"), users.get("lisa_patel"));
        addGroupMember(groups.get("development"), users.get("maria_garcia"));
        addGroupMember(groups.get("development"), users.get("alex_thompson"));
        addGroupMember(groups.get("development"), users.get("priya_sharma"));
        addGroupMember(groups.get("cab"), users.get("sarah_chen"));
        addGroupMember(groups.get("cab"), users.get("david_kim"));
        addGroupMember(groups.get("cab"), users.get("robert_johnson"));
    }

    private void addGroupMember(GroupEntity group, UserEntity user) {
        GroupMemberEntity gm = new GroupMemberEntity(group, user);
        gm.setSample(true);
        groupMemberRepository.save(gm);
    }

    private Map<String, CustomFieldDefinitionEntity> createSampleCustomFields(OffsetDateTime now) {
        Map<String, CustomFieldDefinitionEntity> fields = new LinkedHashMap<>();
        int order = 1;

        CustomFieldDefinitionEntity f;
        f = new CustomFieldDefinitionEntity("Business Justification", "TEXT", List.of(), true, order++);
        f.setSample(true);
        fields.put("business_justification", customFieldDefinitionRepository.save(f));

        f = new CustomFieldDefinitionEntity("Risk Assessment Score", "NUMBER", List.of(),
                false, order++, java.math.BigDecimal.ZERO, new java.math.BigDecimal("100"));
        f.setSample(true);
        fields.put("risk_score", customFieldDefinitionRepository.save(f));

        f = new CustomFieldDefinitionEntity("Target Implementation Date", "DATE", List.of(), false, order++);
        f.setSample(true);
        fields.put("target_date", customFieldDefinitionRepository.save(f));

        f = new CustomFieldDefinitionEntity("Deployment Environment", "DROPDOWN",
                List.of("Production", "Staging", "Development", "QA"), false, order++);
        f.setSample(true);
        fields.put("deployment_env", customFieldDefinitionRepository.save(f));

        f = new CustomFieldDefinitionEntity("Requires Downtime", "CHECKBOX", List.of(), false, order);
        f.setSample(true);
        fields.put("requires_downtime", customFieldDefinitionRepository.save(f));

        return fields;
    }

    private Map<String, ChangeRequestEntity> createSampleChangeRequests(Map<String, UserEntity> users, OffsetDateTime now) {
        UserEntity sarah = users.get("sarah_chen");
        UserEntity james = users.get("james_wilson");
        UserEntity maria = users.get("maria_garcia");
        UserEntity david = users.get("david_kim");
        UserEntity robert = users.get("robert_johnson");
        UserEntity alex = users.get("alex_thompson");
        UserEntity priya = users.get("priya_sharma");
        UserEntity lisa = users.get("lisa_patel");

        Map<String, ChangeRequestEntity> crs = new LinkedHashMap<>();

        crs.put("pg_upgrade", createCR(
                "Upgrade PostgreSQL to v17 in Production",
                "PostgreSQL 17 brings significant performance improvements including improved query planning, faster WAL processing, and enhanced partition pruning. This upgrade will reduce query latency by an estimated 15-20% across all production databases.\n\n**Scope**: All primary and replica instances in us-east-1 and eu-west-1.\n**Impact**: Rolling upgrade with <5min downtime per instance.\n**Rollback**: PG 16 images on standby.",
                Priority.HIGH, RiskLevel.HIGH, ChangeRequestStatus.APPROVED,
                ApprovalType.LINEAR, james, now,
                new String[]{"postgresql", "database", "production"},
                now.plusDays(3), now.plusDays(5)));

        crs.put("payment_v2", createCR(
                "Deploy Payment Service v2.5.0",
                "Payment Service v2.5.0 introduces PCI-DSS v4.0 compliant tokenisation, revised fee calculation engine, and webhook retry with exponential backoff.\n\n**Scope**: All payment gateway endpoints.\n**Impact**: Zero-downtime blue-green deployment.\n**Rollback**: Revert traffic to v2.4.2 canary.",
                Priority.CRITICAL, RiskLevel.CRITICAL, ChangeRequestStatus.PENDING_APPROVAL,
                ApprovalType.LINEAR, maria, now,
                new String[]{"payment-service", "api", "production"},
                now.plusDays(1), now.plusDays(3)));

        crs.put("k8s_migration", createCR(
                "Migrate Auth Service to New Kubernetes Cluster",
                "The current k8s cluster (prod-01) has reached 85% capacity. This change migrates the Auth Service to the new prod-02 cluster with improved node autoscaler policies.\n\n**Scope**: Auth Service + Redis session store.\n**Impact**: DNS cutover with <30s interruption.\n**Rollback**: Re-point DNS to prod-01.",
                Priority.HIGH, RiskLevel.HIGH, ChangeRequestStatus.APPROVED,
                ApprovalType.NON_LINEAR, james, now,
                new String[]{"kubernetes", "auth-service", "infrastructure"},
                now.plusDays(2), now.plusDays(4)));

        crs.put("ssl_renewal", createCR(
                "Update SSL Certificates for *.acme-demo.io",
                "Annual SSL certificate renewal for the wildcard domain. Current certificates expire in 14 days. New certificates have been provisioned via Let's Encrypt.\n\n**Scope**: All edge proxies and load balancers.\n**Impact**: Zero downtime — certs are hot-swapped.\n**Rollback**: Retain existing certs until expiry.",
                Priority.MEDIUM, RiskLevel.LOW, ChangeRequestStatus.APPROVED,
                ApprovalType.LINEAR, sarah, now,
                new String[]{"ssl", "security", "networking"},
                now.plusDays(1), now.plusDays(2)));

        crs.put("network_switches", createCR(
                "Replace Network Switches in Data Center B",
                "End-of-life Cisco Catalyst 9300 switches in rack rows B1-B4 are causing intermittent link flapping. Replacement with Arista 7050X3 is approved by procurement.\n\n**Scope**: 12 switches across 4 rack rows.\n**Impact**: 15-minute maintenance window per rack row.\n**Rollback**: Re-seat existing switches if new units fail POST.",
                Priority.CRITICAL, RiskLevel.HIGH, ChangeRequestStatus.APPROVED,
                ApprovalType.LINEAR, david, now,
                new String[]{"networking", "data-center", "hardware"},
                now.plusDays(7), now.plusDays(9)));

        ChangeRequestEntity kafkaCR = createCR(
                "Upgrade Kafka to v3.7",
                "Kafka 3.7 introduces KRaft mode (no ZooKeeper dependency), improved tiered storage, and 30% faster consumer group rebalancing.\n\n**Scope**: All Kafka clusters in us-east-1.\n**Impact**: Rolling broker restart with <2min consumer lag per broker.\n**Rollback**: Downgrade broker binaries to 3.6.",
                Priority.MEDIUM, RiskLevel.MEDIUM, ChangeRequestStatus.PENDING_APPROVAL,
                ApprovalType.NON_LINEAR, alex, now,
                new String[]{"kafka", "messaging", "production"},
                now.plusDays(5), now.plusDays(7));
        crs.put("kafka_upgrade", kafkaCR);

        ChangeRequestEntity blueGreenCR = createCR(
                "Implement Blue-Green Deployment Pipeline",
                "Introduce blue-green deployment pattern to the CI/CD pipeline for all stateless services. Currently deployments use canary which increases blast radius.\n\n**Scope**: Deployment pipeline config, ingress controller, health-check endpoints.\n**Impact**: No production impact — pipeline-only change.\n**Rollback**: Revert pipeline config to previous canary setup.",
                Priority.MEDIUM, RiskLevel.MEDIUM, ChangeRequestStatus.DRAFT,
                ApprovalType.LINEAR, priya, now,
                new String[]{"deployment", "ci-cd", "infrastructure"},
                null, null);
        crs.put("blue_green", blueGreenCR);

        crs.put("spring_cve", createCR(
                "Patch Spring Framework CVE-2026-1234",
                "Critical RCE vulnerability (CVSS 9.8) in Spring Framework affects all Java services. Patch to 6.2.4 is available and tested in staging.\n\n**Scope**: All Java services (Auth, Payment, Notification, Analytics).\n**Impact**: Rolling restart per service.\n**Rollback**: Revert to 6.2.3 images.",
                Priority.CRITICAL, RiskLevel.HIGH, ChangeRequestStatus.APPROVED,
                ApprovalType.LINEAR, robert, now,
                new String[]{"security", "spring", "vulnerability"},
                now, now.plusDays(1)));

        ChangeRequestEntity dbPoolCR = createCR(
                "Refactor Database Connection Pooling",
                "Current HikariCP configuration causes connection leaks under high load (p99 latency spikes from 50ms to 800ms). Refactor to use virtual-thread-aware connection pool.\n\n**Scope**: Connection pool configuration across all data services.\n**Impact**: No downtime — config change only.\n**Rollback**: Restore previous pool config.",
                Priority.LOW, RiskLevel.LOW, ChangeRequestStatus.CANCELLED,
                ApprovalType.LINEAR, maria, now,
                new String[]{"database", "performance"},
                null, null);
        crs.put("db_pooling", dbPoolCR);

        crs.put("monitoring", createCR(
                "Deploy Monitoring Stack (Prometheus + Grafana)",
                "Set up a dedicated observability stack with Prometheus for metrics collection, Grafana for dashboards, and Alertmanager for routing alerts to PagerDuty.\n\n**Scope**: New observability namespace in prod-02 cluster.\n**Impact**: None — this is additive infrastructure.\n**Rollback**: Delete namespace.",
                Priority.MEDIUM, RiskLevel.MEDIUM, ChangeRequestStatus.APPROVED,
                ApprovalType.NON_LINEAR, james, now,
                new String[]{"monitoring", "infrastructure", "observability"},
                now.plusDays(4), now.plusDays(6)));

        crs.put("redis_migrate", createCR(
                "Migrate Redis Cluster to v7.2",
                "Redis 7.2 brings native function support, improved memory efficiency, and 23% faster replication.\n\n**Scope**: All Redis clusters in us-east-1.\n**Impact**: Online migration using replica-of redirection.\n**Rollback**: Re-point clients to old cluster.",
                Priority.HIGH, RiskLevel.MEDIUM, ChangeRequestStatus.PENDING_APPROVAL,
                ApprovalType.LINEAR, alex, now,
                new String[]{"redis", "cache", "production"},
                now.plusDays(3), now.plusDays(5)));

        crs.put("firewall", createCR(
                "Update Firewall Rules for New API Gateway",
                "New API gateway requires opening ports 8443 and 9090 on the DMZ firewalls. Rules have been reviewed by Network Engineering.\n\n**Scope**: DMZ firewall rulesets in us-east-1 and eu-west-1.\n**Impact**: Rules apply immediately; no traffic disruption.\n**Rollback**: Revert rule changes via firewall manager.",
                Priority.HIGH, RiskLevel.HIGH, ChangeRequestStatus.APPROVED,
                ApprovalType.LINEAR, robert, now,
                new String[]{"networking", "security", "firewall"},
                now.plusDays(2), now.plusDays(3)));

        ChangeRequestEntity zeroTrustCR = createCR(
                "Implement Zero-Trust Network Architecture",
                "Phase 1 of zero-trust migration: implement mTLS between all service meshes, deploy service mesh proxy (Istio), and enforce identity-based access policies.\n\n**Scope**: All production services in both regions.\n**Impact**: Progressive rollout; no downtime expected.\n**Rollback**: Remove mTLS enforcement; revert to network-policy-based access.",
                Priority.CRITICAL, RiskLevel.CRITICAL, ChangeRequestStatus.DRAFT,
                ApprovalType.NON_LINEAR, sarah, now,
                new String[]{"security", "networking", "zero-trust"},
                null, null);
        crs.put("zero_trust", zeroTrustCR);

        ChangeRequestEntity lbCR = createCR(
                "Upgrade Load Balancer Firmware",
                "HAProxy 2.10 patches a connection-draining bug under high concurrency. Staging tests show 40% improvement in connection teardown latency.\n\n**Scope**: All HAProxy instances in us-east-1.\n**Impact**: Rolling restart with <1s connection interruption per instance.\n**Rollback**: Downgrade to 2.9.",
                Priority.MEDIUM, RiskLevel.LOW, ChangeRequestStatus.REJECTED,
                ApprovalType.LINEAR, david, now,
                new String[]{"load-balancer", "infrastructure"},
                now.plusDays(6), now.plusDays(7));
        crs.put("lb_upgrade", lbCR);

        crs.put("encryption_at_rest", createCR(
                "Deploy Customer Data Encryption at Rest",
                "Implement AES-256 encryption at rest for all customer-facing data stores (PostgreSQL, S3, Redis) to meet SOC 2 Type II and PCI-DSS v4.0 requirements.\n\n**Scope**: All databases, object storage, and cache layers.\n**Impact**: Online encryption with negligible performance overhead.\n**Rollback**: Disable encryption policies; data remains encrypted.",
                Priority.HIGH, RiskLevel.MEDIUM, ChangeRequestStatus.APPROVED,
                ApprovalType.LINEAR, lisa, now,
                new String[]{"security", "encryption", "compliance"},
                now.plusDays(10), now.plusDays(15)));

        return crs;
    }

    private ChangeRequestEntity createCR(String title, String description,
            Priority priority, RiskLevel riskLevel, ChangeRequestStatus status,
            ApprovalType approvalType, UserEntity createdBy, OffsetDateTime now,
            String[] affectedSystems, OffsetDateTime scheduledStart, OffsetDateTime scheduledEnd) {
        ChangeRequestEntity cr = new ChangeRequestEntity();
        cr.setTitle(title);
        cr.setDescription(description);
        cr.setPriority(priority);
        cr.setRiskLevel(riskLevel);
        cr.setStatus(status);
        cr.setApprovalType(approvalType);
        cr.setCreatedBy(createdBy);
        cr.setAffectedSystems(affectedSystems);
        cr.setSample(true);

        if (scheduledStart != null) {
            cr.setScheduledStart(scheduledStart);
        }
        if (scheduledEnd != null) {
            cr.setScheduledEnd(scheduledEnd);
        }

        if (status == ChangeRequestStatus.PENDING_APPROVAL || status.isClosed()) {
            cr.setSlaDeadline(now.plusDays(priority == Priority.CRITICAL ? 1 : priority == Priority.HIGH ? 3 : 7));
        }

        return changeRequestRepository.save(cr);
    }

    private void createSampleApprovers(Map<String, ChangeRequestEntity> crs, Map<String, UserEntity> users, OffsetDateTime now) {
        UserEntity david = users.get("david_kim");
        UserEntity robert = users.get("robert_johnson");
        UserEntity priya = users.get("priya_sharma");
        UserEntity sarah = users.get("sarah_chen");

        ChangeRequestEntity pg = crs.get("pg_upgrade");
        addApprover(pg, david, 1, ApproverStatus.APPROVED, now.minusDays(4));
        addApprover(pg, robert, 2, ApproverStatus.APPROVED, now.minusDays(3));
        pg.setApprovalLocked(true);

        ChangeRequestEntity payment = crs.get("payment_v2");
        addApprover(payment, david, 1, ApproverStatus.PENDING, null);
        addApprover(payment, robert, 2, ApproverStatus.PENDING, null);

        ChangeRequestEntity k8s = crs.get("k8s_migration");
        addApprover(k8s, david, 1, ApproverStatus.APPROVED, now.minusDays(2));
        addApprover(k8s, robert, 2, ApproverStatus.APPROVED, now.minusDays(1));
        addApprover(k8s, priya, 3, ApproverStatus.APPROVED, now.minusDays(1));
        k8s.setApprovalLocked(true);

        ChangeRequestEntity ssl = crs.get("ssl_renewal");
        addApprover(ssl, david, 1, ApproverStatus.APPROVED, now.minusDays(1));
        ssl.setApprovalLocked(true);

        ChangeRequestEntity networkSw = crs.get("network_switches");
        addApprover(networkSw, robert, 1, ApproverStatus.APPROVED, now.minusDays(6));
        addApprover(networkSw, david, 2, ApproverStatus.APPROVED, now.minusDays(5));
        networkSw.setApprovalLocked(true);

        ChangeRequestEntity kafka = crs.get("kafka_upgrade");
        addApprover(kafka, david, 1, ApproverStatus.PENDING, null);
        addApprover(kafka, priya, 2, ApproverStatus.PENDING, null);

        ChangeRequestEntity spring = crs.get("spring_cve");
        addApprover(spring, david, 1, ApproverStatus.APPROVED, now.minusHours(12));
        addApprover(spring, robert, 2, ApproverStatus.APPROVED, now.minusHours(6));
        spring.setApprovalLocked(true);

        ChangeRequestEntity monitoring = crs.get("monitoring");
        addApprover(monitoring, david, 1, ApproverStatus.APPROVED, now.minusDays(3));
        addApprover(monitoring, robert, 2, ApproverStatus.APPROVED, now.minusDays(2));
        monitoring.setApprovalLocked(true);

        ChangeRequestEntity redis = crs.get("redis_migrate");
        addApprover(redis, david, 1, ApproverStatus.PENDING, null);

        ChangeRequestEntity firewall = crs.get("firewall");
        addApprover(firewall, robert, 1, ApproverStatus.APPROVED, now.minusDays(2));
        addApprover(firewall, david, 2, ApproverStatus.APPROVED, now.minusDays(1));
        firewall.setApprovalLocked(true);

        ChangeRequestEntity encryption = crs.get("encryption_at_rest");
        addApprover(encryption, robert, 1, ApproverStatus.APPROVED, now.minusDays(4));
        addApprover(encryption, sarah, 2, ApproverStatus.APPROVED, now.minusDays(3));
        encryption.setApprovalLocked(true);

        ChangeRequestEntity lb = crs.get("lb_upgrade");
        addApprover(lb, robert, 1, ApproverStatus.REJECTED, now.minusDays(2));
        lb.setApprovalLocked(true);
    }

    private void addApprover(ChangeRequestEntity cr, UserEntity user,
            int position, ApproverStatus status, OffsetDateTime decidedAt) {
        CrApproverEntity approver = new CrApproverEntity(cr, user, position);
        approver.setSample(true);
        approver.setStatus(status);
        approver.setDecidedAt(decidedAt);
        if (status == ApproverStatus.REJECTED) {
            approver.setRejectionReason("Does not meet current priority thresholds. Resubmit with stronger business case.");
        }
        crApproverRepository.save(approver);
    }

    private void createSampleWatchers(Map<String, ChangeRequestEntity> crs, Map<String, UserEntity> users, OffsetDateTime now) {
        addWatcher(crs.get("pg_upgrade"), users.get("lisa_patel"));
        addWatcher(crs.get("payment_v2"), users.get("alex_thompson"));
        addWatcher(crs.get("k8s_migration"), users.get("maria_garcia"));
        addWatcher(crs.get("spring_cve"), users.get("james_wilson"));
        addWatcher(crs.get("monitoring"), users.get("priya_sharma"));
    }

    private void addWatcher(ChangeRequestEntity cr, UserEntity user) {
        CrWatcherEntity watcher = new CrWatcherEntity(cr, user);
        watcher.setSample(true);
        crWatcherRepository.save(watcher);
    }

    private void createSampleDeployments(Map<String, ChangeRequestEntity> crs, Map<String, UserEntity> users, OffsetDateTime now) {
        UserEntity david = users.get("david_kim");

        ChangeRequestEntity pg = crs.get("pg_upgrade");
        pg.setWorkflowMode(RequestWorkflowMode.DELIVERY_PIPELINE);
        pg.setApprovalStatus(ChangeRequestStatus.APPROVED);
        RequestUatEntity pgUat = createSampleUat(pg, pg.getCreatedBy(), now);
        createDeployment(pg, pgUat, david, "COMPLETED", now.minusDays(2), now.minusDays(1));

        ChangeRequestEntity k8s = crs.get("k8s_migration");
        k8s.setWorkflowMode(RequestWorkflowMode.DELIVERY_PIPELINE);
        k8s.setApprovalStatus(ChangeRequestStatus.APPROVED);
        RequestUatEntity k8sUat = createSampleUat(k8s, k8s.getCreatedBy(), now);
        createDeployment(k8s, k8sUat, david, "PENDING", now.minusDays(1), null);
    }

    private RequestUatEntity createSampleUat(ChangeRequestEntity cr, UserEntity creator, OffsetDateTime now) {
        RequestUatEntity uat = new RequestUatEntity();
        uat.setRequestId(cr.getId());
        uat.setTitle("UAT for " + cr.getTitle());
        uat.setDetails("Sample UAT details");
        uat.setStatus("PROMOTED");
        uat.setReadOnly(true);
        uat.setCreatedBy(creator.getId());
        return requestUatRepository.save(uat);
    }

    private RequestDeploymentEntity createDeployment(ChangeRequestEntity cr, RequestUatEntity uat,
            UserEntity assignee, String status, OffsetDateTime promotedAt, OffsetDateTime completedAt) {
        RequestDeploymentEntity deployment = new RequestDeploymentEntity();
        deployment.setRequestId(cr.getId());
        deployment.setUatId(uat.getId());
        deployment.setAssignee(assignee);
        deployment.setStatus(status);
        deployment.setCreatedBy(cr.getCreatedBy().getId());
        deployment.setPromotedAt(promotedAt);
        if (completedAt != null) {
            deployment.setCompletedAt(completedAt);
        }
        return requestDeploymentRepository.save(deployment);
    }

    private void createSampleCustomFieldValues(Map<String, ChangeRequestEntity> crs,
            Map<String, CustomFieldDefinitionEntity> fields) {
        CustomFieldDefinitionEntity bizJust = fields.get("business_justification");
        CustomFieldDefinitionEntity riskScore = fields.get("risk_score");
        CustomFieldDefinitionEntity targetDate = fields.get("target_date");
        CustomFieldDefinitionEntity deployEnv = fields.get("deployment_env");
        CustomFieldDefinitionEntity downtime = fields.get("requires_downtime");

        createCustomField(crs.get("pg_upgrade"), bizJust, "Performance gains of 15-20% from PG17 query planner improvements");
        createCustomField(crs.get("pg_upgrade"), riskScore, "7");
        createCustomField(crs.get("pg_upgrade"), targetDate, OffsetDateTime.now().plusDays(14).toString());
        createCustomField(crs.get("pg_upgrade"), deployEnv, "Production");
        createCustomField(crs.get("pg_upgrade"), downtime, "true");

        createCustomField(crs.get("payment_v2"), bizJust, "PCI-DSS v4.0 compliance requirement and fee calculation accuracy");
        createCustomField(crs.get("payment_v2"), riskScore, "9");
        createCustomField(crs.get("payment_v2"), deployEnv, "Production");
        createCustomField(crs.get("payment_v2"), downtime, "false");

        createCustomField(crs.get("spring_cve"), bizJust, "Critical RCE vulnerability — immediate patching required");
        createCustomField(crs.get("spring_cve"), riskScore, "10");
        createCustomField(crs.get("spring_cve"), deployEnv, "Production");
        createCustomField(crs.get("spring_cve"), downtime, "false");

        createCustomField(crs.get("monitoring"), deployEnv, "Staging");
        createCustomField(crs.get("monitoring"), downtime, "false");

        createCustomField(crs.get("firewall"), deployEnv, "Production");
        createCustomField(crs.get("firewall"), downtime, "false");

        createCustomField(crs.get("encryption_at_rest"), bizJust, "SOC 2 Type II and PCI-DSS v4.0 compliance mandate");
        createCustomField(crs.get("encryption_at_rest"), riskScore, "6");
        createCustomField(crs.get("encryption_at_rest"), deployEnv, "Production");
        createCustomField(crs.get("encryption_at_rest"), downtime, "false");
    }

    private void createCustomField(ChangeRequestEntity cr, CustomFieldDefinitionEntity field, String value) {
        ChangeRequestCustomFieldEntity cf = new ChangeRequestCustomFieldEntity(cr, field.getId(), value);
        cf.setSample(true);
        changeRequestCustomFieldRepository.save(cf);
    }

    private void createSampleComments(Map<String, ChangeRequestEntity> crs, Map<String, UserEntity> users, OffsetDateTime now) {
        createCommentWithMentions(crs.get("pg_upgrade"), users.get("david_kim"),
                "I've reviewed the upgrade plan. The rolling strategy looks solid. My team will be on standby during the maintenance window.",
                List.of(users.get("james_wilson")), now.minusHours(2));

        createCommentWithMentions(crs.get("pg_upgrade"), users.get("robert_johnson"),
                "<p>Security review passed. Please ensure the <strong>pg_dump</strong> backup completes before starting the upgrade.</p>",
                List.of(), now.minusHours(1));

        createCommentWithMentions(crs.get("payment_v2"), users.get("priya_sharma"),
                "<p>Tested on staging — all webhook integration tests pass. Ready for production deploy.</p>",
                List.of(users.get("maria_garcia")), now.minusHours(3));

        createCommentWithMentions(crs.get("payment_v2"), users.get("lisa_patel"),
                "<p>Audit trail for the new tokenisation flow is properly configured. PCI-DSS compliance verified.</p>",
                List.of(), now.minusHours(2));

        createCommentWithMentions(crs.get("k8s_migration"), users.get("david_kim"),
                "DNS cutover plan is reviewed. We tested the failover and it works within 15 seconds.",
                List.of(), now.minusHours(4));

        createCommentWithMentions(crs.get("spring_cve"), users.get("james_wilson"),
                "<p>Patch 6.2.4 deployed to staging. Running full regression suite now — ETA 2 hours.</p>",
                List.of(users.get("robert_johnson")), now.minusHours(5));

        createCommentWithMentions(crs.get("blue_green"), users.get("sarah_chen"),
                "<p>Priya, can you add the rollback procedure details? We need explicit steps for reverting the ingress config.</p>",
                List.of(users.get("priya_sharma")), now.minusHours(1));

        createCommentWithMentions(crs.get("zero_trust"), users.get("robert_johnson"),
                "<p>Phase 1 scope looks right. Let's ensure we have <strong>mTLS health checks</strong> before enabling enforcement.</p>",
                List.of(users.get("sarah_chen")), now.minusMinutes(30));

        createCommentWithMentions(crs.get("lb_upgrade"), users.get("james_wilson"),
                "<p>Staging tests confirm the connection-draining fix. +1 from me.</p>",
                List.of(), now.minusHours(6));
    }

    private void createCommentWithMentions(ChangeRequestEntity cr, UserEntity author, String body,
            List<UserEntity> mentioned, OffsetDateTime createdAt) {
        CommentEntity comment = new CommentEntity(cr, author, body);
        comment.setSample(true);
        commentRepository.save(comment);

        for (UserEntity u : mentioned) {
            CommentMentionEntity mention = new CommentMentionEntity(comment, u);
            mention.setSample(true);
            commentMentionRepository.save(mention);
        }
    }

    private void createSampleActivityStream(Map<String, ChangeRequestEntity> crs, Map<String, UserEntity> users, OffsetDateTime now) {
        createActivity(crs.get("pg_upgrade"), users.get("james_wilson"), "created", Map.of("title", "Upgrade PostgreSQL to v17 in Production"), now.minusDays(5));
        createActivity(crs.get("pg_upgrade"), users.get("james_wilson"), "submitted", Map.of(), now.minusDays(4));
        createActivity(crs.get("pg_upgrade"), users.get("david_kim"), "approved", Map.of(), now.minusDays(3));

        createActivity(crs.get("payment_v2"), users.get("maria_garcia"), "created", Map.of("title", "Deploy Payment Service v2.5.0"), now.minusDays(2));
        createActivity(crs.get("payment_v2"), users.get("maria_garcia"), "submitted", Map.of(), now.minusDays(1));

        createActivity(crs.get("k8s_migration"), users.get("james_wilson"), "created", Map.of("title", "Migrate Auth Service to New Kubernetes Cluster"), now.minusDays(3));
        createActivity(crs.get("k8s_migration"), users.get("james_wilson"), "submitted", Map.of(), now.minusDays(2));
        createActivity(crs.get("k8s_migration"), users.get("david_kim"), "approved", Map.of(), now.minusDays(1));

        createActivity(crs.get("spring_cve"), users.get("robert_johnson"), "created", Map.of("title", "Patch Spring Framework CVE-2026-1234"), now.minusDays(1));
        createActivity(crs.get("spring_cve"), users.get("robert_johnson"), "submitted", Map.of(), now.minusHours(12));
        createActivity(crs.get("spring_cve"), users.get("david_kim"), "approved", Map.of(), now.minusHours(6));

        createActivity(crs.get("monitoring"), users.get("james_wilson"), "created", Map.of("title", "Deploy Monitoring Stack"), now.minusDays(4));
        createActivity(crs.get("monitoring"), users.get("james_wilson"), "submitted", Map.of(), now.minusDays(3));

        createActivity(crs.get("encryption_at_rest"), users.get("lisa_patel"), "created", Map.of("title", "Deploy Customer Data Encryption at Rest"), now.minusDays(6));
        createActivity(crs.get("encryption_at_rest"), users.get("lisa_patel"), "submitted", Map.of(), now.minusDays(5));
    }

    private void createActivity(ChangeRequestEntity cr, UserEntity actor, String actionType,
            Map<String, Object> payload, OffsetDateTime createdAt) {
        ActivityStreamEntity activity = new ActivityStreamEntity(cr, actor, actionType, payload);
        activity.setSample(true);
        activityStreamRepository.save(activity);
    }

    private void createSampleNotifications(Map<String, UserEntity> users, Map<String, ChangeRequestEntity> crs, OffsetDateTime now) {
        createNotification(users.get("david_kim"), "APPROVAL_REQUEST",
                "Change Request Requires Your Approval",
                "Upgrade PostgreSQL to v17 in Production is pending your approval.",
                "/change-requests/" + crs.get("pg_upgrade").getId(), now.minusDays(4));

        createNotification(users.get("robert_johnson"), "APPROVAL_REQUEST",
                "Change Request Requires Your Approval",
                "Deploy Payment Service v2.5.0 is pending your approval.",
                "/change-requests/" + crs.get("payment_v2").getId(), now.minusDays(1));

        createNotification(users.get("james_wilson"), "STATUS_CHANGE",
                "Change Request Approved",
                "Your change request 'Migrate Auth Service to New Kubernetes Cluster' has been approved.",
                "/change-requests/" + crs.get("k8s_migration").getId(), now.minusDays(1));

        createNotification(users.get("priya_sharma"), "MENTION",
                "You were mentioned in a comment",
                "Sarah Chen mentioned you in 'Implement Blue-Green Deployment Pipeline'.",
                "/change-requests/" + crs.get("blue_green").getId(), now.minusHours(1));

        createNotification(users.get("sarah_chen"), "SLA_WARNING",
                "SLA Deadline Approaching",
                "Change Request 'Patch Spring Framework CVE-2026-1234' SLA deadline is in 6 hours.",
                "/change-requests/" + crs.get("spring_cve").getId(), now.minusHours(6));

        createNotification(users.get("maria_garcia"), "APPROVAL_REQUEST",
                "Change Request Requires Your Approval",
                "Upgrade Kafka to v3.7 is pending your review.",
                "/change-requests/" + crs.get("kafka_upgrade").getId(), now.minusDays(1));
    }

    private void createNotification(UserEntity recipient, String type, String title, String body, String link, OffsetDateTime createdAt) {
        NotificationEntity notification = new NotificationEntity(recipient, type, title, body, link);
        notification.setSample(true);
        notificationRepository.save(notification);
    }

    private void createSampleAttachments(Map<String, ChangeRequestEntity> crs, Map<String, UserEntity> users, OffsetDateTime now) {
        createAttachment(crs.get("payment_v2"), users.get("maria_garcia"),
                "payment-v2-deployment-plan.pdf", "application/pdf", 245_760,
                "sample-data/payment-v2-deployment-plan.pdf");

        createAttachment(crs.get("spring_cve"), users.get("robert_johnson"),
                "CVE-2026-1234-advisory.pdf", "application/pdf", 89_432,
                "sample-data/CVE-2026-1234-advisory.pdf");

        createAttachment(crs.get("k8s_migration"), users.get("james_wilson"),
                "k8s-migration-runbook.md", "text/markdown", 15_234,
                "sample-data/k8s-migration-runbook.md");

        createAttachment(crs.get("encryption_at_rest"), users.get("lisa_patel"),
                "soc2-encryption-evidence.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 198_450,
                "sample-data/soc2-encryption-evidence.docx");
    }

    private void createAttachment(ChangeRequestEntity cr, UserEntity uploader, String fileName,
            String mimeType, long sizeBytes, String storagePath) {
        AttachmentEntity attachment = new AttachmentEntity(cr, uploader, fileName, mimeType, sizeBytes, storagePath);
        attachment.setSample(true);
        attachmentRepository.save(attachment);
    }
}