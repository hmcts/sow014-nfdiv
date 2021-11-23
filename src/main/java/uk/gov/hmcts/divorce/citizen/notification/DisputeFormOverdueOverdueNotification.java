package uk.gov.hmcts.divorce.citizen.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.divorce.divorcecase.model.CaseData;
import uk.gov.hmcts.divorce.notification.CommonContent;
import uk.gov.hmcts.divorce.notification.NotificationService;

import java.util.Map;

import static uk.gov.hmcts.divorce.citizen.notification.Applicant2ApprovedNotification.PAYS_FEES;
import static uk.gov.hmcts.divorce.notification.CommonContent.ACTION_REQUIRED;
import static uk.gov.hmcts.divorce.notification.CommonContent.IS_REMINDER;
import static uk.gov.hmcts.divorce.notification.CommonContent.NO;
import static uk.gov.hmcts.divorce.notification.CommonContent.REVIEW_DEADLINE_DATE;
import static uk.gov.hmcts.divorce.notification.CommonContent.YES;
import static uk.gov.hmcts.divorce.notification.EmailTemplateName.DISPUTE_FORM_OVERDUE;
import static uk.gov.hmcts.divorce.notification.EmailTemplateName.JOINT_APPLICATION_APPROVED_APPLICANT1_REMINDER;
import static uk.gov.hmcts.divorce.notification.EmailTemplateName.JOINT_APPLICATION_OVERDUE;
import static uk.gov.hmcts.divorce.notification.FormatUtil.DATE_TIME_FORMATTER;

@Component
@Slf4j
public class DisputeFormOverdueOverdueNotification {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CommonContent commonContent;

    public void send(CaseData caseData, Long id) {
        log.info("Notifying the applicant that the Dispute Form for application {} is overdue", id);

        Map<String, String> templateVars =
            commonContent.mainTemplateVars(caseData, id, caseData.getApplicant1(), caseData.getApplicant2());
        templateVars.put(REVIEW_DEADLINE_DATE, caseData.getDueDate().format(DATE_TIME_FORMATTER));

        notificationService.sendEmail(
            caseData.getApplicant1().getEmail(),
            DISPUTE_FORM_OVERDUE,
            templateVars,
            caseData.getApplicant1().getLanguagePreference()
        );
    }
}
