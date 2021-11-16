package uk.gov.hmcts.divorce.systemupdate.schedule.bulkaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.divorce.bulkaction.data.BulkCaseRetiredFields;
import uk.gov.hmcts.divorce.divorcecase.model.CaseData;
import uk.gov.hmcts.divorce.idam.IdamService;
import uk.gov.hmcts.divorce.systemupdate.service.CcdConflictException;
import uk.gov.hmcts.divorce.systemupdate.service.CcdManagementException;
import uk.gov.hmcts.divorce.systemupdate.service.CcdSearchCaseException;
import uk.gov.hmcts.divorce.systemupdate.service.CcdSearchService;
import uk.gov.hmcts.divorce.systemupdate.service.CcdUpdateService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.idam.client.models.User;

import static uk.gov.hmcts.divorce.systemupdate.event.SystemMigrateBulkCase.SYSTEM_MIGRATE_BULK_CASE;

@Component
@Slf4j
public class SystemMigrateBulkCasesTask implements Runnable {

    @Autowired
    private CcdSearchService ccdSearchService;

    @Autowired
    private CcdUpdateService ccdUpdateService;

    @Autowired
    private IdamService idamService;

    @Autowired
    private AuthTokenGenerator authTokenGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void run() {
        log.info("Migrate cases scheduled task started");

        final User user = idamService.retrieveSystemUpdateUserDetails();
        final String serviceAuthorization = authTokenGenerator.generate();

        try {
            ccdSearchService
                .searchForBulkCasesWithVersionLessThan(BulkCaseRetiredFields.getVersion(), user, serviceAuthorization)
                .parallelStream()
                .forEach(details -> migrateCase(details, user, serviceAuthorization));

        } catch (final CcdSearchCaseException e) {
            log.error("Case migration schedule task stopped after search error", e);
        }
    }

    private void migrateCase(final CaseDetails caseDetails, final User user, final String serviceAuthorization) {
        try {
            final var data = BulkCaseRetiredFields.migrate(caseDetails.getData());
            objectMapper.convertValue(data, CaseData.class);

            caseDetails.setData(data);
            ccdUpdateService.submitEvent(caseDetails, SYSTEM_MIGRATE_BULK_CASE, user, serviceAuthorization);
            log.info("Migration complete for case id: {}", caseDetails.getId());
        } catch (final CcdConflictException e) {
            log.error("Could not get lock for case id: {}, continuing to next case", caseDetails.getId());
        } catch (final CcdManagementException e) {
            log.error("Submit event failed for case id: {}, continuing to next case", caseDetails.getId());
        }
    }
}
