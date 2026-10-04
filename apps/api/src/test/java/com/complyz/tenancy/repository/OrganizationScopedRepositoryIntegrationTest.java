package com.complyz.tenancy.repository;

import com.complyz.audit.entity.AuditAction;
import com.complyz.audit.entity.AuditLog;
import com.complyz.audit.repository.AuditLogRepository;
import com.complyz.audit.service.AuditService;
import com.complyz.clients.entity.BeneficialOwner;
import com.complyz.clients.entity.BeneficialOwnerRole;
import com.complyz.clients.entity.Client;
import com.complyz.clients.repository.BeneficialOwnerRepository;
import com.complyz.clients.repository.ClientRepository;
import com.complyz.clients.service.KycService;
import com.complyz.flags.entity.Flag;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.flags.service.FlagService;
import com.complyz.ingest.entity.ImportBatch;
import com.complyz.ingest.entity.ImportFormat;
import com.complyz.ingest.repository.ImportBatchRepository;
import com.complyz.monitoring.entity.ThresholdRule;
import com.complyz.monitoring.entity.ThresholdRuleType;
import com.complyz.monitoring.entity.Transaction;
import com.complyz.monitoring.entity.TransactionType;
import com.complyz.monitoring.repository.TransactionRepository;
import com.complyz.reporting.entity.CurrencyTransactionReport;
import com.complyz.reporting.entity.SuspiciousActivityReport;
import com.complyz.reporting.controller.ReportController;
import com.complyz.reporting.repository.CurrencyTransactionReportRepository;
import com.complyz.reporting.repository.SuspiciousActivityReportRepository;
import com.complyz.reporting.service.ReportExportService;
import com.complyz.settings.entity.SystemConfig;
import com.complyz.tenancy.entity.Organization;
import com.complyz.tenancy.service.TenantAccessService;
import com.complyz.users.entity.Role;
import com.complyz.users.entity.User;
import com.complyz.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Import({
        FlagService.class,
        KycService.class,
        ReportController.class,
        ReportExportService.class,
        AuditService.class,
        TenantAccessService.class
})
class OrganizationScopedRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("complyz_test")
            .withUsername("complyz_test")
            .withPassword("complyz_test");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private EntityManager entityManager;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private BeneficialOwnerRepository beneficialOwnerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ImportBatchRepository importBatchRepository;
    @Autowired private FlagRepository flagRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private SuspiciousActivityReportRepository strRepository;
    @Autowired private CurrencyTransactionReportRepository ctrRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private FlagService flagService;
    @Autowired private KycService kycService;
    @Autowired private ReportController reportController;
    @Autowired private ReportExportService reportExportService;

    private Organization organizationA;
    private Organization organizationB;
    private Client clientA;
    private Client clientB;
    private User userA;
    private User userB;
    private BeneficialOwner ownerA;
    private BeneficialOwner ownerB;
    private ImportBatch batchA;
    private ImportBatch batchB;
    private ImportBatch uploaderMismatchBatch;
    private ImportBatch batchWithoutUploaderA;
    private Transaction transactionA;
    private Transaction transactionB;
    private Flag directFlagA;
    private Flag transactionOnlyFlagA;
    private Flag directFlagB;
    private Flag inconsistentFlag;
    private Flag reviewerMismatchFlag;
    private SuspiciousActivityReport strA;
    private SuspiciousActivityReport strTransactionOnlyA;
    private SuspiciousActivityReport strB;
    private SuspiciousActivityReport strInconsistent;
    private SuspiciousActivityReport strReviewerMismatch;
    private CurrencyTransactionReport ctrA;
    private CurrencyTransactionReport ctrB;

    @BeforeEach
    void createTwoOrganizationsWithSeparateRecords() {
        organizationA = organizationRepository.save(new Organization("tenant-a", "Tenant A"));
        organizationB = organizationRepository.save(new Organization("tenant-b", "Tenant B"));

        clientA = client("Cliente A", "DOC-A", organizationA);
        clientB = client("Cliente B", "DOC-B", organizationB);
        userA = user("user-a@complyz.test", organizationA);
        userB = user("user-b@complyz.test", organizationB);

        ownerA = beneficialOwnerRepository.save(new BeneficialOwner(
                clientA, "Beneficiário A", BeneficialOwnerRole.FOUNDER, new BigDecimal("60.00")));
        ownerB = beneficialOwnerRepository.save(new BeneficialOwner(
                clientB, "Beneficiário B", BeneficialOwnerRole.BENEFICIARY, new BigDecimal("40.00")));

        batchA = batch("tenant-a.csv", userA, organizationA);
        batchB = batch("tenant-b.csv", userB, organizationB);
        uploaderMismatchBatch = batch("tenant-a-foreign-uploader.csv", userB, organizationA);
        batchWithoutUploaderA = batch("tenant-a-no-uploader.csv", null, organizationA);

        transactionA = transactionRepository.save(new Transaction(
                clientA, new BigDecimal("100.00"), "MZN", LocalDate.of(2026, 1, 10), TransactionType.DEPOSIT));
        transactionRepository.save(new Transaction(
                clientA, new BigDecimal("50.00"), "MZN", LocalDate.of(2026, 1, 11), TransactionType.TRANSFER));
        transactionB = transactionRepository.save(new Transaction(
                clientB, new BigDecimal("900.00"), "MZN", LocalDate.of(2026, 1, 10), TransactionType.DEPOSIT));

        directFlagA = saveFlag(clientA, null, "Direct flag A");
        transactionOnlyFlagA = saveFlag(null, transactionA, "Transaction-only flag A");
        directFlagB = saveFlag(clientB, null, "Direct flag B");
        inconsistentFlag = saveFlag(clientA, transactionB, "Inconsistent A/B flag");
        reviewerMismatchFlag = saveFlag(clientA, null, "A flag reviewed by a B user");
        reviewerMismatchFlag.setReviewedBy(userB);
        reviewerMismatchFlag.setStatus(com.complyz.flags.entity.FlagStatus.RESOLVED);
        reviewerMismatchFlag = flagRepository.save(reviewerMismatchFlag);

        strA = report(directFlagA, true, "STR A");
        strTransactionOnlyA = report(transactionOnlyFlagA, false, "STR transaction-only A");
        strB = report(directFlagB, true, "STR B");
        strInconsistent = report(inconsistentFlag, true, "STR inconsistent A/B");
        strReviewerMismatch = report(reviewerMismatchFlag, true, "STR with foreign reviewer");

        ThresholdRule rule = new ThresholdRule(
                "CTR threshold", ThresholdRuleType.SINGLE_TRANSACTION_VALUE, new BigDecimal("100000"), 30);
        entityManager.persist(rule);
        ctrA = ctrRepository.save(new CurrencyTransactionReport(clientA,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), new BigDecimal("150.00"), rule));
        ctrB = ctrRepository.save(new CurrencyTransactionReport(clientB,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), new BigDecimal("900.00"), rule));

        auditLogRepository.save(new AuditLog(userA, AuditAction.LOGIN, "User", userA.getId(), null));
        auditLogRepository.save(new AuditLog(userB, AuditAction.LOGIN, "User", userB.getId(), null));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void flagsAndSuspiciousActivityReportsExcludeForeignAndInconsistentRelations() {
        assertThat(flagRepository.findAllByOrganizationId(organizationA.getId(), null, null, Sort.unsorted()))
                .extracting(Flag::getId)
                .containsExactlyInAnyOrder(directFlagA.getId(), transactionOnlyFlagA.getId());
        assertThat(flagRepository.findAllByOrganizationId(organizationB.getId(), null, null, Sort.unsorted()))
                .extracting(Flag::getId)
                .containsExactly(directFlagB.getId());
        assertThat(flagRepository.findByIdAndOrganizationId(directFlagB.getId(), organizationA.getId())).isEmpty();
        assertThat(flagRepository.findByIdAndOrganizationId(inconsistentFlag.getId(), organizationA.getId())).isEmpty();
        assertThat(flagRepository.findByIdAndOrganizationId(reviewerMismatchFlag.getId(), organizationA.getId())).isEmpty();
        assertThat(flagRepository.countByClientIdAndOrganizationIdAndTypeAndStatus(
                clientA.getId(), organizationA.getId(), FlagType.KYC_INCOMPLETE,
                com.complyz.flags.entity.FlagStatus.PENDING)).isEqualTo(1);
        assertThat(flagRepository.countByTransactionIdAndOrganizationIdAndType(
                transactionB.getId(), organizationB.getId(), FlagType.KYC_INCOMPLETE)).isZero();
        assertThat(flagRepository.countByOrganizationIdAndStatus(organizationA.getId(),
                com.complyz.flags.entity.FlagStatus.PENDING)).isEqualTo(2);
        assertThat(flagRepository.countByOrganizationIdAndStatus(organizationB.getId(),
                com.complyz.flags.entity.FlagStatus.PENDING)).isEqualTo(1);

        assertThat(strRepository.findAllByOrganizationId(organizationA.getId()))
                .extracting(SuspiciousActivityReport::getId)
                .containsExactlyInAnyOrder(strA.getId(), strTransactionOnlyA.getId());
        assertThat(strRepository.findAllByOrganizationId(organizationB.getId()))
                .extracting(SuspiciousActivityReport::getId)
                .containsExactly(strB.getId());
        assertThat(strRepository.findByIdAndOrganizationId(strB.getId(), organizationA.getId())).isEmpty();
        assertThat(strRepository.findByIdAndOrganizationId(strInconsistent.getId(), organizationB.getId())).isEmpty();
        assertThat(strRepository.findByIdAndOrganizationId(strReviewerMismatch.getId(), organizationA.getId())).isEmpty();
        assertThat(strRepository.findSubmittedByOrganizationId(organizationA.getId()))
                .extracting(SuspiciousActivityReport::getId)
                .containsExactly(strA.getId());
        assertThat(strRepository.findDraftsByOrganizationId(organizationA.getId()))
                .extracting(SuspiciousActivityReport::getId)
                .containsExactly(strTransactionOnlyA.getId());
        assertThat(strRepository.findSubmittedBetweenForOrganization(
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z"), organizationA.getId()))
                .extracting(SuspiciousActivityReport::getId)
                .containsExactly(strA.getId());
    }

    @Test
    void clientsOwnersImportsUsersAndAuditLogsAreOrganizationScoped() {
        assertThat(clientRepository.findAll((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("organization").get("id"), organizationA.getId()),
                PageRequest.of(0, 10)).getContent())
                .extracting(Client::getId)
                .containsExactly(clientA.getId());
        assertThat(clientRepository.findByIdAndOrganization(clientB.getId(), organizationA)).isEmpty();
        assertThat(clientRepository.countByOrganizationId(organizationA.getId())).isEqualTo(1);
        assertThat(clientRepository.countByOrganizationId(organizationB.getId())).isEqualTo(1);

        assertThat(beneficialOwnerRepository.findByClientIdAndClientOrganizationId(clientA.getId(), organizationA.getId()))
                .extracting(BeneficialOwner::getId)
                .containsExactly(ownerA.getId());
        assertThat(beneficialOwnerRepository.findByClientIdAndClientOrganizationId(clientB.getId(), organizationA.getId()))
                .isEmpty();
        assertThat(beneficialOwnerRepository.findByIdAndClientOrganizationId(ownerB.getId(), organizationA.getId()))
                .isEmpty();

        assertThat(importBatchRepository.findAllByOrganizationId(organizationA.getId()))
                .extracting(ImportBatch::getId)
                .containsExactlyInAnyOrder(batchA.getId(), batchWithoutUploaderA.getId());
        assertThat(importBatchRepository.findByIdAndOrganizationId(batchB.getId(), organizationA.getId())).isEmpty();
        assertThat(importBatchRepository.findByIdAndOrganizationId(uploaderMismatchBatch.getId(), organizationA.getId()))
                .isEmpty();
        assertThat(importBatchRepository.findByIdAndOrganizationId(batchWithoutUploaderA.getId(), organizationA.getId()))
                .hasValueSatisfying(batch -> assertThat(batch.getId()).isEqualTo(batchWithoutUploaderA.getId()));

        assertThat(userRepository.findAllByOrganizationId(organizationA.getId()))
                .extracting(User::getId)
                .containsExactly(userA.getId());
        assertThat(userRepository.findByIdAndOrganizationId(userB.getId(), organizationA.getId())).isEmpty();
        assertThat(auditLogRepository.findByOrganizationIdAndAction(organizationA.getId(), AuditAction.LOGIN,
                PageRequest.of(0, 10)).getContent())
                .extracting(AuditLog::getActor)
                .extracting(User::getId)
                .containsExactly(userA.getId());
    }

    @Test
    void reportsAndTransactionAggregatesAreScopedInPersistence() {
        assertThat(ctrRepository.findAllByClientOrganizationId(organizationA.getId()))
                .extracting(CurrencyTransactionReport::getId)
                .containsExactly(ctrA.getId());
        assertThat(ctrRepository.findByIdAndClientOrganizationId(ctrB.getId(), organizationA.getId())).isEmpty();
        assertThat(ctrRepository.findByOrganizationAndAggregationPeriodStartBetween(
                organizationA.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .extracting(CurrencyTransactionReport::getId)
                .containsExactly(ctrA.getId());

        assertThat(transactionRepository.countByOrganizationId(organizationA.getId())).isEqualTo(2);
        assertThat(transactionRepository.countByOrganizationId(organizationB.getId())).isEqualTo(1);
        assertThat(transactionRepository.sumAmountByOrganizationAndTransactionDateBetween(
                organizationA.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .isEqualByComparingTo("150.00");
        assertThat(transactionRepository.sumAmountByOrganizationAndTransactionDateBetween(
                organizationB.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .isEqualByComparingTo("900.00");
        assertThat(transactionRepository.sumAmountByClientAndOrganizationAndDateBetween(
                clientA.getId(), organizationB.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .isEqualByComparingTo("0");
    }

    @Test
    void sharedSystemConfigDoesNotSerializeAnUpdaterFromAnotherOrganization() throws Exception {
        SystemConfig config = new SystemConfig("shared.setting", "{\"enabled\":true}", userB);
        config.setUpdatedAt(null);

        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(config);

        assertThat(json).doesNotContain(userB.getEmail(), userB.getFullName(), organizationB.getName());
    }

    @Test
    void foreignRecordsCannotBeMutatedSubmittedOrExported() throws Exception {
        assertThatThrownBy(() -> flagService.reviewFlag(
                directFlagB.getId(), userA, "unauthorized", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> kycService.verifyOwner(ownerB.getId(), userA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> kycService.addOwner(clientB.getId(), "Unauthorized owner",
                BeneficialOwnerRole.FOUNDER, new BigDecimal("20.00"), userA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> kycService.recalculateAndPersist(clientB.getId(), userA))
                .isInstanceOf(IllegalArgumentException.class);

        var authentication = new TestingAuthenticationToken(userA, null, "ROLE_OCOS");
        var updateResponse = reportController.updateStr(strB.getId(), new ReportController.UpdateStrRequest(
                "unauthorized-account", "Changed B", null, null, null, null, false, null), authentication);
        assertThat(updateResponse.getStatusCode().value()).isEqualTo(404);
        assertThat(reportController.submitCtr(ctrB.getId(), authentication).getStatusCode().value()).isEqualTo(404);
        assertThat(reportController.exportStrPdf(strB.getId(), authentication).getStatusCode().value()).isEqualTo(404);

        entityManager.flush();
        entityManager.clear();
        assertThat(strRepository.findById(strB.getId()).orElseThrow().getAccountHolder()).isEqualTo("STR B");
        assertThat(ctrRepository.findById(ctrB.getId()).orElseThrow().getSubmittedAt()).isNull();
        assertThat(flagRepository.findById(directFlagB.getId()).orElseThrow().getReviewNote()).isNull();
        assertThat(beneficialOwnerRepository.findById(ownerB.getId()).orElseThrow().getVerifiedAt()).isNull();
        assertThat(beneficialOwnerRepository.findByClientIdAndClientOrganizationId(
                clientB.getId(), organizationB.getId())).hasSize(1);
        assertThat(clientRepository.findById(clientB.getId()).orElseThrow().isKycComplete()).isFalse();

        byte[] annualExport = reportExportService.exportAnnualExcel(2026, organizationA.getId());
        List<String> exportedCells = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(annualExport))) {
            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        exportedCells.add(formatter.formatCellValue(cell));
                    }
                }
            }
        }
        assertThat(exportedCells).contains("STR A", "150.00");
        assertThat(exportedCells).doesNotContain("STR B", "STR inconsistent A/B", "900.00");
    }

    private Client client(String name, String document, Organization organization) {
        Client client = new Client(name, "BI", document);
        client.setOrganization(organization);
        return clientRepository.save(client);
    }

    private User user(String email, Organization organization) {
        User user = new User(email, email, "password-hash", Role.COMPLIANCE_OFFICER);
        user.setOrganization(organization);
        return userRepository.save(user);
    }

    private ImportBatch batch(String fileName, User user, Organization organization) {
        ImportBatch batch = new ImportBatch(fileName, ImportFormat.CSV, user);
        batch.setOrganization(organization);
        return importBatchRepository.save(batch);
    }

    private Flag saveFlag(Client client, Transaction transaction, String reason) {
        return flagRepository.save(new Flag(client, transaction, FlagType.KYC_INCOMPLETE, reason, null));
    }

    private SuspiciousActivityReport report(Flag flag, boolean submitted, String holder) {
        SuspiciousActivityReport report = new SuspiciousActivityReport();
        report.setFlag(flag);
        report.setReportingInstitution("Instituição de crédito");
        report.setAccountHolder(holder);
        report.setSuspicionDescription("Actividade suspeita para teste");
        report.setBusinessRelationshipDescription("Relação comercial de teste");
        report.setOcosName("Responsável de conformidade");
        if (submitted) {
            report.setSubmittedAt(Instant.parse("2026-01-15T12:00:00Z"));
        }
        return strRepository.save(report);
    }
}
