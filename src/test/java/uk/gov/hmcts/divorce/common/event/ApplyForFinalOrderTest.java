package uk.gov.hmcts.divorce.common.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.ConfigBuilderImpl;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.Event;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.divorce.common.notification.Applicant1AppliedForFinalOrderNotification;
import uk.gov.hmcts.divorce.divorcecase.model.ApplicationType;
import uk.gov.hmcts.divorce.divorcecase.model.CaseData;
import uk.gov.hmcts.divorce.divorcecase.model.State;
import uk.gov.hmcts.divorce.divorcecase.model.UserRole;
import uk.gov.hmcts.divorce.notification.NotificationDispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;
import static uk.gov.hmcts.divorce.common.event.ApplyForFinalOrder.FINAL_ORDER_REQUESTED;
import static uk.gov.hmcts.divorce.divorcecase.model.State.AwaitingFinalOrder;
import static uk.gov.hmcts.divorce.divorcecase.model.State.AwaitingJointFinalOrder;
import static uk.gov.hmcts.divorce.divorcecase.model.State.FinalOrderOverdue;
import static uk.gov.hmcts.divorce.divorcecase.model.State.FinalOrderRequested;
import static uk.gov.hmcts.divorce.divorcecase.model.State.WelshTranslationReview;
import static uk.gov.hmcts.divorce.testutil.ConfigTestUtil.createCaseDataConfigBuilder;
import static uk.gov.hmcts.divorce.testutil.ConfigTestUtil.getEventsFrom;

@ExtendWith(MockitoExtension.class)
class ApplyForFinalOrderTest {

    @Mock
    private Applicant1AppliedForFinalOrderNotification applicant1AppliedForFinalOrderNotification;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @InjectMocks
    private ApplyForFinalOrder applyForFinalOrder;

    @Test
    void shouldAddConfigurationToConfigBuilder() {
        final ConfigBuilderImpl<CaseData, State, UserRole> configBuilder = createCaseDataConfigBuilder();

        applyForFinalOrder.configure(configBuilder);

        assertThat(getEventsFrom(configBuilder).values())
            .extracting(Event::getId)
            .contains(FINAL_ORDER_REQUESTED);
    }

    @Test
    void shouldChangeStateToFinalOrderRequestedOnAboutToSubmitIfSoleApplication() {
        final CaseData caseData = CaseData.builder().applicationType(ApplicationType.SOLE_APPLICATION).build();
        final CaseDetails<CaseData, State> caseDetails = CaseDetails.<CaseData, State>builder()
            .data(caseData).state(State.AwaitingFinalOrder).id(1L).build();

        final AboutToStartOrSubmitResponse<CaseData, State> response = applyForFinalOrder.aboutToSubmit(caseDetails, caseDetails);

        assertThat(response.getState()).isEqualTo(FinalOrderRequested);
    }

    @Test
    void shouldChangeStateToAwaitingJointFinalOrderOnAboutToSubmitIfJointApplication() {
        final CaseData caseData = CaseData.builder().applicationType(ApplicationType.JOINT_APPLICATION).build();
        final CaseDetails<CaseData, State> caseDetails = CaseDetails.<CaseData, State>builder()
            .data(caseData).state(State.AwaitingFinalOrder).id(1L).build();

        final AboutToStartOrSubmitResponse<CaseData, State> response = applyForFinalOrder.aboutToSubmit(caseDetails, caseDetails);

        assertThat(response.getState()).isEqualTo(AwaitingJointFinalOrder);
    }

    @Test
    void shouldNotChangeStateIfStateIsFinalOrderOverdueOnAboutToSubmit() {
        final CaseData caseData = CaseData.builder().applicationType(ApplicationType.JOINT_APPLICATION).build();
        final CaseDetails<CaseData, State> caseDetails = CaseDetails.<CaseData, State>builder()
            .data(caseData).state(FinalOrderOverdue).id(1L).build();

        final AboutToStartOrSubmitResponse<CaseData, State> response = applyForFinalOrder.aboutToSubmit(caseDetails, caseDetails);

        assertThat(response.getState()).isEqualTo(FinalOrderOverdue);
    }

    @Test
    void shouldSendSoleAppliedForFinalOrderNotificationIfSoleApplicationTypeAndAwaitingFinalOrderState() {
        final CaseData caseData = CaseData.builder().applicationType(ApplicationType.SOLE_APPLICATION).build();
        final CaseDetails<CaseData, State> caseDetails = CaseDetails.<CaseData, State>builder().id(1L).data(caseData).build();
        caseDetails.setState(AwaitingFinalOrder);

        applyForFinalOrder.aboutToSubmit(caseDetails, null);

        verify(notificationDispatcher).send(applicant1AppliedForFinalOrderNotification, caseData, caseDetails.getId());
        verifyNoMoreInteractions(notificationDispatcher);
    }

    @Test
    void shouldNotSendSoleAppliedForFinalOrderNotificationFinalOrderOverdueState() {
        final CaseData caseData = CaseData.builder().applicationType(ApplicationType.SOLE_APPLICATION).build();
        final CaseDetails<CaseData, State> caseDetails = CaseDetails.<CaseData, State>builder().id(1L).data(caseData).build();
        caseDetails.setState(FinalOrderOverdue);

        applyForFinalOrder.aboutToSubmit(caseDetails, null);

        verify(notificationDispatcher, never()).send(applicant1AppliedForFinalOrderNotification, caseData, caseDetails.getId());
    }

    @Test
    void shouldSetStateToWelshTranslationReviewIfSoleAndApp1LanguagePreferenceWelshYes() {
        final CaseData caseData = CaseData.builder().applicationType(ApplicationType.SOLE_APPLICATION).build();
        caseData.getApplicant1().setLanguagePreferenceWelsh(YES);
        final CaseDetails<CaseData, State> caseDetails = CaseDetails.<CaseData, State>builder()
            .data(caseData).state(State.AwaitingFinalOrder).id(1L).build();

        final AboutToStartOrSubmitResponse<CaseData, State> response = applyForFinalOrder.aboutToSubmit(caseDetails, null);

        assertThat(response.getState()).isEqualTo(WelshTranslationReview);
        assertThat(response.getData().getApplication().getWelshPreviousState()).isEqualTo(FinalOrderRequested);
    }

    @Test
    void shouldSetStateToWelshTranslationReviewIfJointAndApp1LanguagePreferenceWelshYes() {
        final CaseData caseData = CaseData.builder().applicationType(ApplicationType.JOINT_APPLICATION).build();
        caseData.getApplicant1().setLanguagePreferenceWelsh(YES);

        final CaseDetails<CaseData, State> beforeDetails = CaseDetails.<CaseData, State>builder()
            .data(caseData).state(State.AwaitingFinalOrder).id(1L).build();
        final CaseDetails<CaseData, State> caseDetails = CaseDetails.<CaseData, State>builder()
            .data(caseData).state(State.AwaitingFinalOrder).id(1L).build();

        final AboutToStartOrSubmitResponse<CaseData, State> response = applyForFinalOrder.aboutToSubmit(caseDetails, beforeDetails);

        assertThat(response.getState()).isEqualTo(WelshTranslationReview);
        assertThat(response.getData().getApplication().getWelshPreviousState()).isEqualTo(AwaitingJointFinalOrder);
    }
}
