package uk.gov.hmcts.divorce.bulkaction.ccd;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.divorce.bulkaction.data.BulkActionCaseData;
import uk.gov.hmcts.divorce.divorcecase.model.UserRole;

@Component
public class BulkActionCaseTypeTab implements CCDConfig<BulkActionCaseData, BulkActionState, UserRole> {

    @Override
    public void configure(final ConfigBuilder<BulkActionCaseData, BulkActionState, UserRole> configBuilder) {
        configBuilder.tab("bulkCaseList", "Bulk case list")
            .field(BulkActionCaseData::getCaseTitle)
            .field("coCourtName")
            .field("coDateAndTimeOfHearing")
            .field("coPronouncementJudge")
            .field("coHasJudgePronounced")
            .field("coPronouncedDate")
            .field("dateFinalOrderEligibleFrom")
            .field(BulkActionCaseData::getBulkListCaseDetails);
    }
}
