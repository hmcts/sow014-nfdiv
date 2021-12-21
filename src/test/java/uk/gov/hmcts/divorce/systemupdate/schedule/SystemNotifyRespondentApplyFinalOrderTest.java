package uk.gov.hmcts.divorce.systemupdate.schedule;

import feign.FeignException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.type.YesOrNo;
import uk.gov.hmcts.divorce.idam.IdamService;
import uk.gov.hmcts.divorce.systemupdate.service.CcdConflictException;
import uk.gov.hmcts.divorce.systemupdate.service.CcdManagementException;
import uk.gov.hmcts.divorce.systemupdate.service.CcdSearchCaseException;
import uk.gov.hmcts.divorce.systemupdate.service.CcdSearchService;
import uk.gov.hmcts.divorce.systemupdate.service.CcdUpdateService;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.idam.client.models.User;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.divorce.divorcecase.model.State.AwaitingFinalOrder;
import static uk.gov.hmcts.divorce.systemupdate.event.SystemNotifyRespondentFinalOrderApply.SYSTEM_NOTIFY_RESPONDENT_APPLY_FINAL_ORDER;
import static uk.gov.hmcts.divorce.systemupdate.schedule.SystemNotifyRespondentApplyFinalOrderTask.APPLICATION_TYPE;
import static uk.gov.hmcts.divorce.systemupdate.service.CcdSearchService.DATA;
import static uk.gov.hmcts.divorce.systemupdate.service.CcdSearchService.STATE;
import static uk.gov.hmcts.divorce.testutil.TestConstants.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.divorce.testutil.TestConstants.SYSTEM_UPDATE_AUTH_TOKEN;

@ExtendWith(MockitoExtension.class)
public class SystemNotifyRespondentApplyFinalOrderTest {

    @Mock
    private CcdSearchService ccdSearchService;
    @Mock
    private CcdUpdateService ccdUpdateService;
    @Mock
    private IdamService idamService;
    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @InjectMocks
    private SystemNotifyRespondentApplyFinalOrderTask systemNotifyRespondentApplyFinalOrder;

    private User user;

    private static final BoolQueryBuilder query =
        boolQuery()
            .must(matchQuery(STATE, AwaitingFinalOrder))
            .must(matchQuery(String.format(DATA, APPLICATION_TYPE), "soleApplication"));

    @BeforeEach
    void setUp() {
        user = new User(SYSTEM_UPDATE_AUTH_TOKEN, UserDetails.builder().build());
        when(idamService.retrieveSystemUpdateUserDetails()).thenReturn(user);
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION);
    }

    @Test
    void shouldNotTriggerNotifyRespondentTaskWhenDateFinalOrderEligibleToRespondentNotReached() {
        final CaseDetails caseDetails1 = mock(CaseDetails.class);

        Map<String, Object> data1 = new HashMap<>();
        data1.put("dateFinalOrderEligibleToRespondent", LocalDate.now().plusMonths(3).toString());
        data1.put("applicant2FinalOrderReminderSent", YesOrNo.NO);
        when(caseDetails1.getData()).thenReturn(data1);

        final List<CaseDetails> caseDetailsList = List.of(caseDetails1);

        when(ccdSearchService.searchForAllCasesWithQuery(AwaitingFinalOrder, query, user, SERVICE_AUTHORIZATION))
            .thenReturn(caseDetailsList);

        systemNotifyRespondentApplyFinalOrder.run();

        verifyNoInteractions(ccdUpdateService);
    }

    @Test
    void shouldNotTriggerNotifyRespondentTaskWhenDateFinalOrderEligibleToRespondentIsNull() {
        final CaseDetails caseDetails1 = mock(CaseDetails.class);

        Map<String, Object> data1 = new HashMap<>();
        data1.put("dateFinalOrderEligibleToRespondent", null);
        when(caseDetails1.getData()).thenReturn(data1);

        final List<CaseDetails> caseDetailsList = List.of(caseDetails1);

        when(ccdSearchService.searchForAllCasesWithQuery(AwaitingFinalOrder, query, user, SERVICE_AUTHORIZATION))
            .thenReturn(caseDetailsList);

        systemNotifyRespondentApplyFinalOrder.run();

        verifyNoInteractions(ccdUpdateService);
    }

    @Test
    void shouldNotTriggerNotifyRespondentTaskWhenAlreadyNotified() {
        final CaseDetails caseDetails1 = mock(CaseDetails.class);

        Map<String, Object> data1 = new HashMap<>();
        data1.put("dateFinalOrderEligibleToRespondent", LocalDate.now().minusMonths(1).toString());
        data1.put("applicant2FinalOrderReminderSent", YesOrNo.YES);
        when(caseDetails1.getData()).thenReturn(data1);

        final List<CaseDetails> caseDetailsList = List.of(caseDetails1);

        when(ccdSearchService.searchForAllCasesWithQuery(AwaitingFinalOrder, query, user, SERVICE_AUTHORIZATION))
            .thenReturn(caseDetailsList);

        systemNotifyRespondentApplyFinalOrder.run();

        verifyNoInteractions(ccdUpdateService);
    }

    @Test
    void shouldNotTriggerNotifyRespondentTaskWhenSearchReturnsEmptyList() {

        when(ccdSearchService.searchForAllCasesWithQuery(AwaitingFinalOrder, query, user, SERVICE_AUTHORIZATION))
            .thenReturn(Collections.emptyList());

        systemNotifyRespondentApplyFinalOrder.run();

        verifyNoInteractions(ccdUpdateService);
    }

    @Test
    void shouldTriggerNotifyRespondentTaskWhenDateFinalOrderEligibleToRespondentAndNotNotified() {
        final CaseDetails caseDetails1 = mock(CaseDetails.class);

        Map<String, Object> data1 = new HashMap<>();
        data1.put("dateFinalOrderEligibleToRespondent", LocalDate.now().minusMonths(1).toString());
        data1.put("applicant2FinalOrderReminderSent", YesOrNo.NO);
        when(caseDetails1.getData()).thenReturn(data1);

        final List<CaseDetails> caseDetailsList = List.of(caseDetails1);

        when(ccdSearchService.searchForAllCasesWithQuery(AwaitingFinalOrder, query, user, SERVICE_AUTHORIZATION))
            .thenReturn(caseDetailsList);

        systemNotifyRespondentApplyFinalOrder.run();

        verify(ccdUpdateService).submitEvent(caseDetails1, SYSTEM_NOTIFY_RESPONDENT_APPLY_FINAL_ORDER, user, SERVICE_AUTHORIZATION);
    }

    @Test
    void shouldNotTriggerNotifyRespondentTaskWhenDateFinalOrderEligibleToRespondentButAlreadyNotified() {
        final CaseDetails caseDetails1 = mock(CaseDetails.class);

        Map<String, Object> data1 = new HashMap<>();
        data1.put("dateFinalOrderEligibleToRespondent", LocalDate.now().minusMonths(1).toString());
        data1.put("applicant2FinalOrderReminderSent", YesOrNo.YES);
        when(caseDetails1.getData()).thenReturn(data1);

        final List<CaseDetails> caseDetailsList = List.of(caseDetails1);

        when(ccdSearchService.searchForAllCasesWithQuery(AwaitingFinalOrder, query, user, SERVICE_AUTHORIZATION))
            .thenReturn(caseDetailsList);

        systemNotifyRespondentApplyFinalOrder.run();

        verifyNoInteractions(ccdUpdateService);
    }

    @Test
    void shouldNotSubmitEventIfSearchFails() {
        when(ccdSearchService.searchForAllCasesWithQuery(AwaitingFinalOrder, query, user, SERVICE_AUTHORIZATION))
            .thenThrow(new CcdSearchCaseException("Failed to search cases", mock(FeignException.class)));

        systemNotifyRespondentApplyFinalOrder.run();

        verifyNoInteractions(ccdUpdateService);
    }

    @Test
    void shouldStopProcessingIfThereIsConflictDuringSubmission() {
        final CaseDetails caseDetails1 = mock(CaseDetails.class);
        final CaseDetails caseDetails2 = mock(CaseDetails.class);
        final List<CaseDetails> caseDetailsList = List.of(caseDetails1, caseDetails2);

        Map<String, Object> data1 = new HashMap<>();
        data1.put("dateFinalOrderEligibleToRespondent", LocalDate.now().minusMonths(1).toString());
        data1.put("applicant2FinalOrderReminderSent", YesOrNo.NO);
        when(caseDetails1.getData()).thenReturn(data1);

        when(ccdSearchService.searchForAllCasesWithQuery(AwaitingFinalOrder, query, user, SERVICE_AUTHORIZATION))
            .thenReturn(caseDetailsList);

        doThrow(new CcdConflictException("Case is modified by another transaction", mock(FeignException.class)))
            .when(ccdUpdateService).submitEvent(caseDetails1, SYSTEM_NOTIFY_RESPONDENT_APPLY_FINAL_ORDER, user, SERVICE_AUTHORIZATION);

        systemNotifyRespondentApplyFinalOrder.run();

        verify(ccdUpdateService)
            .submitEvent(caseDetails1, SYSTEM_NOTIFY_RESPONDENT_APPLY_FINAL_ORDER, user, SERVICE_AUTHORIZATION);
        verify(ccdUpdateService, never())
            .submitEvent(caseDetails2, SYSTEM_NOTIFY_RESPONDENT_APPLY_FINAL_ORDER, user, SERVICE_AUTHORIZATION);
    }

    @Test
    void shouldContinueToNextCaseIfExceptionIsThrownWhileProcessingPreviousCase() {
        final CaseDetails caseDetails1 = mock(CaseDetails.class);
        final CaseDetails caseDetails2 = mock(CaseDetails.class);

        final List<CaseDetails> caseDetailsList = List.of(caseDetails1, caseDetails2);

        Map<String, Object> data1 = new HashMap<>();
        data1.put("dateFinalOrderEligibleToRespondent", LocalDate.now().minusMonths(1).toString());
        data1.put("applicant2FinalOrderReminderSent", YesOrNo.NO);
        when(caseDetails1.getData()).thenReturn(data1);

        Map<String, Object> data2 = new HashMap<>();
        data2.put("dateFinalOrderEligibleToRespondent", LocalDate.now().minusMonths(1).toString());
        data2.put("applicant2FinalOrderReminderSent", YesOrNo.NO);
        when(caseDetails2.getData()).thenReturn(data2);

        when(ccdSearchService.searchForAllCasesWithQuery(AwaitingFinalOrder, query, user, SERVICE_AUTHORIZATION))
            .thenReturn(caseDetailsList);

        doThrow(new CcdManagementException("Failed processing of case", mock(FeignException.class)))
            .when(ccdUpdateService).submitEvent(caseDetails1, SYSTEM_NOTIFY_RESPONDENT_APPLY_FINAL_ORDER, user, SERVICE_AUTHORIZATION);

        systemNotifyRespondentApplyFinalOrder.run();

        verify(ccdUpdateService).submitEvent(caseDetails1, SYSTEM_NOTIFY_RESPONDENT_APPLY_FINAL_ORDER, user, SERVICE_AUTHORIZATION);
        verify(ccdUpdateService).submitEvent(caseDetails2, SYSTEM_NOTIFY_RESPONDENT_APPLY_FINAL_ORDER, user, SERVICE_AUTHORIZATION);
    }

}
