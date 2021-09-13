package uk.gov.hmcts.divorce.caseworker.event;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.sdk.api.CCDConfig;
import uk.gov.hmcts.ccd.sdk.api.ConfigBuilder;
import uk.gov.hmcts.divorce.common.ccd.PageBuilder;
import uk.gov.hmcts.divorce.divorcecase.model.Applicant;
import uk.gov.hmcts.divorce.divorcecase.model.CaseData;
import uk.gov.hmcts.divorce.divorcecase.model.State;
import uk.gov.hmcts.divorce.divorcecase.model.UserRole;

import static uk.gov.hmcts.divorce.divorcecase.model.UserRole.CASE_WORKER;
import static uk.gov.hmcts.divorce.divorcecase.model.UserRole.LEGAL_ADVISOR;
import static uk.gov.hmcts.divorce.divorcecase.model.UserRole.SOLICITOR;
import static uk.gov.hmcts.divorce.divorcecase.model.UserRole.SUPER_USER;
import static uk.gov.hmcts.divorce.divorcecase.model.access.Permissions.CREATE_READ_UPDATE;
import static uk.gov.hmcts.divorce.divorcecase.model.access.Permissions.CREATE_READ_UPDATE_DELETE;
import static uk.gov.hmcts.divorce.divorcecase.model.access.Permissions.READ;

@Component
public class CaseworkerUpdateLanguage implements CCDConfig<CaseData, State, UserRole> {

    public static final String CASEWORKER_UPDATE_LANGUAGE = "caseworker-update-language";

    @Override
    public void configure(final ConfigBuilder<CaseData, State, UserRole> configBuilder) {
        new PageBuilder(configBuilder
            .event(CASEWORKER_UPDATE_LANGUAGE)
            .forAllStates()
            .name("Update language")
            .description("Update language")
            .showSummary(false)
            .explicitGrants()
            .grant(CREATE_READ_UPDATE_DELETE, SUPER_USER)
            .grant(READ, SOLICITOR)
            .grant(CREATE_READ_UPDATE,
                CASE_WORKER,
                LEGAL_ADVISOR))
            .page("caseworkerLangPref")
            .pageLabel("Select Language")
            .complex(CaseData::getApplicant1)
                .mandatoryWithLabel(Applicant::getLanguagePreferenceWelsh, "Applicant's language preference Welsh?")
                .done()
            .complex(CaseData::getApplicant2)
                .mandatoryWithLabel(Applicant::getLanguagePreferenceWelsh, "Respondent's language preference Welsh?")
                .done();
    }
}
