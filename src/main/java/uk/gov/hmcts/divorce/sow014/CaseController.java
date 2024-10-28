package uk.gov.hmcts.divorce.sow014;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;

import java.util.Map;

@RestController
@RequestMapping( path = "/ccd" )
public class CaseController {

    @Autowired
    private JdbcTemplate db;

    @Autowired
    private ObjectMapper mapper;

    @GetMapping(
        value = "/cases/{caseRef}",
        produces = "application/json"
    )
    public String getCase(@PathVariable("caseRef") long caseRef) {
        return db.queryForObject(
            """
                select
                (r - 'data') - 'marked_by_logstash'
                || jsonb_build_object('case_data', r->'data')
                from (
                select to_jsonb(c) r from case_data c where reference = ?
                ) s""",
            new Object[]{ caseRef }, String.class);
    }

    @SneakyThrows
    @PostMapping("/cases")
    public AboutToStartOrSubmitResponse aboutToSubmit(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        var details = (Map<String, Object>) request.get("case_details");
        // persist the request.caseDetails to case_data table
        db.update(
            """
                insert into case_data (state, data, data_classification, reference, security_classification, version)
                values (?, ?::jsonb, ?::jsonb, ?, ?::securityclassification, ?)
                """,
            details.get("state"),
            mapper.writeValueAsString(details.get("case_data")),
            mapper.writeValueAsString(details.get("data_classification")),
            details.get("id"),
            details.get("security_classification"),
            1
        );
        return AboutToStartOrSubmitResponse.builder().build();
    }
}
