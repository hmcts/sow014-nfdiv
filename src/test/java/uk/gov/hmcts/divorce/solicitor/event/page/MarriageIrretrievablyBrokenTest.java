package uk.gov.hmcts.divorce.solicitor.event.page;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.sdk.api.CaseDetails;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.divorce.common.model.CaseData;
import uk.gov.hmcts.divorce.common.model.State;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.NO;
import static uk.gov.hmcts.ccd.sdk.type.YesOrNo.YES;
import static uk.gov.hmcts.divorce.testutil.TestDataHelper.LOCAL_DATE_TIME;
import static uk.gov.hmcts.divorce.testutil.TestDataHelper.caseData;

@ExtendWith(MockitoExtension.class)
class MarriageIrretrievablyBrokenTest {

    private final MarriageIrretrievablyBroken page = new MarriageIrretrievablyBroken();

    @Test
    public void shouldPreventProgressIfMarriageNotBroken() {
        final CaseData caseData = caseData();
        caseData.setScreenHasMarriageBroken(NO);

        final CaseDetails<CaseData, State> details = new CaseDetails<>();
        details.setData(caseData);
        details.setId(1L);
        details.setCreatedDate(LOCAL_DATE_TIME);

        AboutToStartOrSubmitResponse<CaseData, State> response = page.midEvent(details, details);

        assertEquals(response.getErrors().size(), 1);
        assertEquals(
            response.getErrors().get(0),
            "To continue, the applicant must believe and declare that their marriage has irrevocably broken"
        );
    }

    @Test
    public void shouldAllowProgressIfMarriageIsBroken() {
        final CaseData caseData = caseData();
        caseData.setScreenHasMarriageBroken(YES);

        final CaseDetails<CaseData, State> details = new CaseDetails<>();
        details.setData(caseData);
        details.setId(1L);
        details.setCreatedDate(LOCAL_DATE_TIME);

        AboutToStartOrSubmitResponse<CaseData, State> response = page.midEvent(details, details);

        assertEquals(response.getErrors().size(), 0);
    }
}
