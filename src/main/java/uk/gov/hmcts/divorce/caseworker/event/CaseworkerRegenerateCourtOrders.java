package uk.gov.hmcts.divorce.caseworker.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.divorce.caseworker.service.task.GenerateFinalOrder;
import uk.gov.hmcts.divorce.caseworker.service.task.GenerateFinalOrderCoverLetter;
import uk.gov.hmcts.divorce.common.ccd.PageBuilder;
import uk.gov.hmcts.divorce.common.notification.RegenerateCourtOrdersNotification;
import uk.gov.hmcts.divorce.divorcecase.model.CaseData;
import uk.gov.hmcts.divorce.divorcecase.model.State;
import uk.gov.hmcts.divorce.divorcecase.model.UserRole;
import uk.gov.hmcts.divorce.document.DocumentGenerationUtil;
import uk.gov.hmcts.divorce.document.print.documentpack.CertificateOfEntitlementDocumentPack;
import uk.gov.hmcts.divorce.notification.NotificationDispatcher;
import uk.gov.hmcts.divorce.systemupdate.service.task.GenerateConditionalOrderPronouncedCoversheet;
import uk.gov.hmcts.divorce.systemupdate.service.task.GenerateConditionalOrderPronouncedDocument;
import uk.gov.hmcts.divorce.systemupdate.service.task.RemoveExistingConditionalOrderPronouncedDocument;

import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static uk.gov.hmcts.divorce.divorcecase.model.State.POST_SUBMISSION_STATES;
import static uk.gov.hmcts.divorce.divorcecase.model.UserRole.CASE_WORKER;
import static uk.gov.hmcts.divorce.divorcecase.model.UserRole.LEGAL_ADVISOR;
import static uk.gov.hmcts.divorce.divorcecase.model.UserRole.SOLICITOR;
import static uk.gov.hmcts.divorce.divorcecase.model.UserRole.SUPER_USER;
import static uk.gov.hmcts.divorce.divorcecase.model.access.Permissions.CREATE_READ_UPDATE;
import static uk.gov.hmcts.divorce.divorcecase.task.CaseTaskRunner.caseTasks;
import static uk.gov.hmcts.divorce.document.model.DocumentType.CERTIFICATE_OF_ENTITLEMENT_COVER_LETTER_APP1;
import static uk.gov.hmcts.divorce.document.model.DocumentType.CERTIFICATE_OF_ENTITLEMENT_COVER_LETTER_APP2;
import static uk.gov.hmcts.divorce.document.model.DocumentType.CONDITIONAL_ORDER_GRANTED;
import static uk.gov.hmcts.divorce.document.model.DocumentType.FINAL_ORDER_GRANTED;


@RequiredArgsConstructor
@Component
@Slf4j
public class CaseworkerRegenerateCourtOrders implements CCDConfig<CaseData, State, UserRole> {
    public static final String CASEWORKER_REGENERATE_COURT_ORDERS = "caseworker-regenerate-court-orders";
    private GenerateConditionalOrderPronouncedDocument generateConditionalOrderPronouncedDocument;
    private GenerateConditionalOrderPronouncedCoversheet generateConditionalOrderPronouncedCoversheetDocument;
    private GenerateFinalOrderCoverLetter generateFinalOrderCoverLetter;
    private GenerateFinalOrder generateFinalOrder;
    private RegenerateCourtOrdersNotification regenerateCourtOrdersNotification;

    private NotificationDispatcher notificationDispatcher;
    private RemoveExistingConditionalOrderPronouncedDocument removeExistingConditionalOrderPronouncedDocument;

    private DocumentGenerationUtil documentGenerationUtil;
    private final CertificateOfEntitlementDocumentPack certificateOfEntitlementDocumentPack;

    @Override
    public void configure(final ConfigBuilder<CaseData, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
            .event(CASEWORKER_REGENERATE_COURT_ORDERS)
            .forStates(POST_SUBMISSION_STATES)
            .name("Regenerate court orders")
            .description("Regenerate court orders")
            .aboutToSubmitCallback(this::aboutToSubmit)
            .showEventNotes()
            .grant(CREATE_READ_UPDATE, SUPER_USER)
            .grantHistoryOnly(CASE_WORKER, LEGAL_ADVISOR, SOLICITOR))
            .page("regenerateCourtOrderDocs")
            .pageLabel("Regenerate court orders")
            .label("regenerateCourtOrdersWarningLabel", "Updating court orders recreates the Certificate of Entitlement, "
                + "Conditional Order and Final Order, "
                + "based on the latest case data. "
                + "Any other court orders e.g. conditional order refusals, will remain unchanged.\r\n\r\n"
                + "If there have been updates to the case data e.g. change of applicant name, then these will be reflected in the updated "
                + "court orders.\r\n\r\nPrevious versions of court orders will not be stored against the case.");
    }

    public AboutToStartOrSubmitResponse<CaseData, State> aboutToSubmit(
            final CaseDetails<CaseData, State> details,
            final CaseDetails<CaseData, State> beforeDetails
    ) {
        log.info("Caseworker regenerate court orders callback invoked for Case Id: {}", details.getId());

        var caseData = details.getData();

        if (caseData.getDocuments().getDocumentGeneratedWithType(CONDITIONAL_ORDER_GRANTED).isPresent()) {
            log.info("Regenerating CO Pronounced document for Case Id: {}", details.getId());

            //TODO: Needs to be split into tasks
            generateConditionalOrderPronouncedCoversheetDocument.removeExistingAndGenerateConditionalOrderPronouncedCoversheet(details);

            caseTasks(
                removeExistingConditionalOrderPronouncedDocument,
                generateConditionalOrderPronouncedDocument
            ).run(details);
        }

        if (caseData.getDocuments().getDocumentGeneratedWithType(FINAL_ORDER_GRANTED).isPresent()) {
            log.info("Regenerating Final Order Granted document for Case Id: {}", details.getId());
            generateFinalOrderCoverLetter.removeExistingAndGenerateNewFinalOrderGrantedCoverLetters(details);
            generateFinalOrder.removeExistingAndGenerateNewFinalOrderGrantedDoc(details);
        }

        if (isNotEmpty(caseData.getConditionalOrder().getCertificateOfEntitlementDocument())) {
            log.info("Regenerating certificate of entitlement document for Case Id: {}", details.getId());

            documentGenerationUtil.removeExistingAndGenerateNewDocuments(
                    caseData,
                    details.getId(),
                    certificateOfEntitlementDocumentPack,
                    List.of(CERTIFICATE_OF_ENTITLEMENT_COVER_LETTER_APP1,
                            CERTIFICATE_OF_ENTITLEMENT_COVER_LETTER_APP2));

        }

        notificationDispatcher.send(regenerateCourtOrdersNotification, caseData, details.getId());

        return AboutToStartOrSubmitResponse.<CaseData, State>builder()
                .data(details.getData())
                .build();
    }
}
