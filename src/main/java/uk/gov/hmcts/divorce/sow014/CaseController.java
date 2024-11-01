package uk.gov.hmcts.divorce.sow014;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.ccd.sdk.api.callback.AboutToStartOrSubmitResponse;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(path = "/ccd")
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
                        (((r - 'data') - 'marked_by_logstash') - 'reference') - 'resolved_ttl'
                        || jsonb_build_object('case_data', r->'data')
                        from (
                        select to_jsonb(c) r from case_data c where reference = ?
                        ) s""",
                new Object[]{caseRef}, String.class);
    }

    @SneakyThrows
    @PostMapping("/cases")
    public String aboutToSubmit(@RequestBody Map<String, Object> details) {
        log.info("case Details: {}", details);
        db.update(
            """
                insert into case_data (jurisdiction, case_type_id, state, data, data_classification, reference, security_classification, version)
                values (?, ?, ?, ?::jsonb, ?::jsonb, ?, ?::securityclassification, ?)
                """,
            details.get("jurisdiction"),
            details.get("case_type_id"),
            details.get("state"),
            mapper.writeValueAsString(details.get("case_data")),
            mapper.writeValueAsString(details.get("data_classification")),
            details.get("id"),
            details.get("security_classification"),
            1
        );
        String response = getCase((long) details.get("id"));
        log.info("case response: {}", response);
        return response;
    }
}
