package uk.gov.hmcts.divorce.solicitor.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.divorce.common.ccd.PageBuilder;
import uk.gov.hmcts.divorce.divorcecase.model.CaseData;
import uk.gov.hmcts.divorce.divorcecase.model.FinalOrder;
import uk.gov.hmcts.divorce.divorcecase.model.LabelContent;
import uk.gov.hmcts.divorce.divorcecase.model.State;
import uk.gov.hmcts.divorce.divorcecase.model.UserRole;

import java.time.Clock;
import java.time.LocalDate;

import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;
import static uk.gov.hmcts.divorce.common.ccd.CcdPageConfiguration.NEVER_SHOW;
import static uk.gov.hmcts.divorce.divorcecase.model.State.AwaitingJointFinalOrder;
import static uk.gov.hmcts.divorce.divorcecase.model.UserRole.APPLICANT_2_SOLICITOR;
import static uk.gov.hmcts.divorce.divorcecase.model.UserRole.CASE_WORKER;
import static uk.gov.hmcts.divorce.divorcecase.model.UserRole.CITIZEN;
import static uk.gov.hmcts.divorce.divorcecase.model.UserRole.SUPER_USER;
import static uk.gov.hmcts.divorce.divorcecase.model.access.Permissions.CREATE_READ_UPDATE;
import static uk.gov.hmcts.divorce.solicitor.event.Applicant1SolicitorIntendsSwitchToSoleFo.getIntendsToSwitchToSoleInformationLabel;
import static uk.gov.hmcts.divorce.solicitor.event.Applicant1SolicitorIntendsSwitchToSoleFo.getOtherApplicantIsNotRepresentedLabel;
import static uk.gov.hmcts.divorce.solicitor.event.Applicant1SolicitorIntendsSwitchToSoleFo.getOtherApplicantIsRepresentedLabel;

@Component
public class Applicant2SolicitorIntendsSwitchToSoleFo implements CCDConfig<CaseData, State, UserRole> {

    public static final String APPLICANT_2_INTENDS_TO_SWITCH_TO_SOLE_FO = "applicant2-intends-switch-to-sole-fo";
    private static final String BLANK_LABEL = "";

    @Autowired
    private Clock clock;

    @Override
    public void configure(ConfigBuilder<CaseData, State, UserRole> configBuilder) {

        new PageBuilder(configBuilder
            .event(APPLICANT_2_INTENDS_TO_SWITCH_TO_SOLE_FO)
            .forStates(AwaitingJointFinalOrder)
            .name("Intends to switch to sole FO")
            .description("Applicant 2 intends to switch to sole FO")
            .showCondition("applicationType=\"jointApplication\" AND applicant2CanIntendToSwitchToSoleFo\"Yes\"")
            .grant(CREATE_READ_UPDATE, APPLICANT_2_SOLICITOR)
            .grantHistoryOnly(CITIZEN, CASE_WORKER, SUPER_USER)
            .aboutToSubmitCallback(this::aboutToSubmit))
            .page("app2IntentionSwitchToSoleFo")
            .pageLabel("Intention to apply for a final order")
            .complex(CaseData::getLabelContent)
                .readonlyNoSummary(LabelContent::getFinaliseDivorceOrLegallyEndYourCivilPartnership, NEVER_SHOW)
            .done()
            .label("app2OtherApplicantIsRepresented",
                getOtherApplicantIsRepresentedLabel(), "applicant1SolicitorRepresented=\"Yes\"")
            .label("app2OtherApplicantIsNotRepresented",
                getOtherApplicantIsNotRepresentedLabel(),"applicant1SolicitorRepresented=\"No\"")
            .label("app2IntendsSwitchToSoleFoInfo", getIntendsToSwitchToSoleInformationLabel())
            .complex(CaseData::getFinalOrder)
                .mandatoryNoSummary(FinalOrder::getApplicant2IntendsToSwitchToSole, null, BLANK_LABEL)
            .done();
    }

    public AboutToStartOrSubmitResponse<CaseData, State> aboutToSubmit(CaseDetails<CaseData, State> details,
                                                                       CaseDetails<CaseData, State> beforeDetails) {

        CaseData data = details.getData();

        data.getFinalOrder().setDoesApplicant2IntendToSwitchToSole(YES);
        data.getFinalOrder().setDateApplicant1DeclaredIntentionToSwitchToSoleFo(LocalDate.now(clock));

        return AboutToStartOrSubmitResponse.<CaseData, State>builder()
            .data(data)
            .build();
    }
}
